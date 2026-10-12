#!/usr/bin/env python3
"""Collect an evidence-based local port inventory, never a runtime-success claim.

Only the supplied workspace artifact and explicitly cloned reference repositories
are read. No game-instance folders, credentials, network, or Gradle build is used.
"""
import argparse
import collections
import hashlib
import io
import json
import logging
import re
import subprocess
import zipfile
from pathlib import Path

LOGGER = logging.getLogger("port.baseline")
NESTED_PATH = "META-INF/jarjar/[Forge]RevelationFix-1.20.1-4.7.jar"
SRG_MEMBER = re.compile(r"(?<![\w$])[mf]_\d+_(?![\w$])")
LIB_IMPORT = re.compile(r"^import (com\.mega\.endinglib[\w.]+);", re.MULTILINE)
CONFIG_DEFAULTS = {
    "player_invincible": True,
    "player_ambient_sound": True,
    "hellfire_damage_amount": 0.0444,
    "hellcloud_damage_amount": 0.001,
    "apollyon_health": 666.0,
    "apollyon_hurt_limit": 20.0,
    "summon_apollyon_random_value": 100,
}


def sha256(data):
    return hashlib.sha256(data).hexdigest()


def archive_summary(package):
    names = [name for name in package.namelist() if not name.endswith("/")]
    return {
        "files": len(names),
        "class_files": sum(name.endswith(".class") for name in names),
        "assets_files": sum(name.startswith("assets/") for name in names),
        "data_files": sum(name.startswith("data/") for name in names),
        "nested_jars": sorted(name for name in names if name.startswith("META-INF/jarjar/") and name.endswith(".jar")),
    }


def resource_overlaps(outer, inner):
    names = set(outer.namelist()) & set(inner.namelist())
    resources = sorted(name for name in names if name.startswith(("assets/", "data/")) and not name.endswith("/"))
    identical, different = [], []
    for name in resources:
        (identical if outer.read(name) == inner.read(name) else different).append(name)
    return {
        "identical": identical,
        "different": different,
        "boundary": "content_collision_not_runtime_pack_precedence; language keys also require merged loading",
    }


def literal_item_registrations(text, source, namespace):
    result = []
    for match in re.finditer(r"\bITEMS\s*\.\s*register\s*\(\s*\"([^\"]+)\"", text):
        result.append({
            "id": namespace + ":" + match.group(1),
            "source": source,
            "line": text.count("\n", 0, match.start()) + 1,
            "status": "source_declaration_not_runtime_registry",
        })
    return result


def git_value(repository, *arguments):
    return subprocess.run(["git", "-C", str(repository), *arguments], check=True,
                          text=True, capture_output=True).stdout.strip()


def resolve_library_source(source_root, qualified_name):
    # Imports can name an inner class; try the enclosing compilation unit.
    parts = qualified_name.split(".")
    for end in range(len(parts), 3, -1):
        candidate = source_root.joinpath(*parts[:end]).with_suffix(".java")
        if candidate.is_file():
            return candidate
    return None


def dependency_group(qualified_name):
    if any(word in qualified_name for word in (".coremod.", ".asm.", ".mixin.", ".annotation.")):
        return "loader_asm_mixin"
    if any(word in qualified_name for word in (".capability.", ".data.")):
        return "state_serialization_sync"
    if any(word in qualified_name for word in ("Time", "time", "Task", ".server.")):
        return "time_scheduler_server"
    if any(word in qualified_name for word in (".client.", "Client", "Render", "Shader", "Gui")):
        return "client_render_gui"
    if ".network." in qualified_name:
        return "network"
    if any(word in qualified_name for word in (".item.", ".entity.", ".event.")):
        return "gameplay_entity_item_event"
    return "utility_or_compat"


def collect(root):
    root = Path(root).resolve()
    reference = root / "reference"
    raw_root = reference / "revelationfix-raw"
    mapped_root = reference / "revelationfix-mapped"
    library = reference / "ending_library"
    source_root = library / "src/main/java"
    jar = root / "GoetyRevelation-2.3.4fix2.jar"
    data = jar.read_bytes()
    with zipfile.ZipFile(io.BytesIO(data)) as outer:
        nested_bytes = outer.read(NESTED_PATH)
        with zipfile.ZipFile(io.BytesIO(nested_bytes)) as fix:
            outer_summary, fix_summary = archive_summary(outer), archive_summary(fix)
            collisions = resource_overlaps(outer, fix)
            outer_config = json.loads(outer.read("mixins.goety_revelation.json"))
            fix_config = json.loads(fix.read("revelationfix.mixins.json"))
            metadata = fix.read("META-INF/mods.toml").decode()
            core_ids = re.findall(r"^modId\s*=\s*\"([^\"]+)\"", metadata, re.MULTILINE)
            invulnerable_path = "data/ending_library/tags/items/invulnerable_item.json"
            invulnerable_items = json.loads(outer.read(invulnerable_path))["values"]
    plugin_path = mapped_root / "com/mega/revelationfix/util/RevelationFixMixinPlugin.java"
    plugin = plugin_path.read_text()
    removed = sorted(set(re.findall(r'toRemovedMixins\.add\("z1gned/goetyrevelation/mixin/(\w+)"\)', plugin)))
    outer_names = outer_config.get("mixins", []) + outer_config.get("client", [])
    fix_names = fix_config.get("mixins", []) + fix_config.get("client", [])
    mixin_inventory = []
    for side in ("mixins", "client"):
        for name in fix_config.get(side, []):
            path = mapped_root.joinpath(*fix_config["package"].split("."), *name.split(".")).with_suffix(".java")
            text = path.read_text() if path.is_file() else ""
            mixin_inventory.append({
                "class": name,
                "configured_side": "common" if side == "mixins" else "client",
                "source_exists": path.is_file(),
                "target_imports": re.findall(r"^import ((?:net\.minecraft|com\.Polarice3\.Goety)[^;]+);", text, re.MULTILINE),
                "method_selectors": re.findall(r"method\s*=\s*\{([^}]+)\}", text),
                "loads_client_classes": bool(re.search(r"^import (net\.minecraft\.client|com\.mojang\.blaze3d)", text, re.MULTILINE)),
                "required_mod_annotations": re.findall(r'@ModDependsMixin\s*\(\s*"([^\"]+)"\s*\)', text),
                "excluded_mod_annotations": re.findall(r'@NoModDependsMixin\s*\(\s*"([^\"]+)"\s*\)', text),
                "environment_annotations": re.findall(r"@(DeprecatedMixin|NonDevEnvMixin|DevEnvMixin)\b", text),
                "application_status": "not_runtime_verified",
            })
    sources = list(mapped_root.rglob("*.java"))
    imported_by = collections.defaultdict(list)
    for path in sources:
        for qualified in set(LIB_IMPORT.findall(path.read_text())):
            imported_by[qualified].append(str(path.relative_to(mapped_root)))
    imports = []
    for qualified in sorted(imported_by):
        resolved = resolve_library_source(source_root, qualified)
        imports.append({
            "class": qualified,
            "source": str(resolved.relative_to(library)) if resolved else None,
            "referenced_by": sorted(imported_by[qualified]),
            "group": dependency_group(qualified),
            "compatibility_status": "class_source_located_method_contract_not_guaranteed" if resolved else "missing_source",
        })
    outer_items_path = root / "decompiled/z1gned/goetyrevelation/item/ModItems.java"
    outer_items = literal_item_registrations(outer_items_path.read_text(), str(outer_items_path.relative_to(root)), "goety_revelation")
    if len(outer_items) != 5 or outer_items[4]["id"] != "goety_revelation:wither_quietus_focus":
        raise ValueError("The frozen fifth registration no longer matches the audited ModifyArg ordinal 4")
    outer_items[4] = {**outer_items[4], "id": "goety_revelation:revelation",
                      "patch": "gr.ModItemsMixin ModifyArg DeferredRegister.register ordinal=4 index=0"}
    fix_items_path = mapped_root / "com/mega/revelationfix/common/init/GRItems.java"
    fix_items_text = fix_items_path.read_text()
    fix_items = literal_item_registrations(fix_items_text, str(fix_items_path.relative_to(root)), "goety_revelation")
    blocks_path = mapped_root / "com/mega/revelationfix/common/init/ModBlocks.java"
    blocks_text = blocks_path.read_text()
    block_paths = dict(re.findall(r'(\w+)\s*=\s*BLOCKS\.register\("([^\"]+)"', blocks_text))
    helper_items = []
    for match in re.finditer(r"ModBlocks\.asBLockItem\(ITEMS, ModBlocks\.(\w+)\)", fix_items_text):
        helper_items.append({"id": "goety_revelation:" + block_paths[match.group(1)],
                             "source": str(fix_items_path.relative_to(root)),
                             "line": fix_items_text.count("\n", 0, match.start()) + 1,
                             "condition": None, "status": "source_helper_not_runtime_registry"})
    optional_path = mapped_root / "com/mega/revelationfix/common/item/curios/enigmtic_legacy/EnigmaticLegacyItemInit.java"
    optional_text = optional_path.read_text()
    for match in re.finditer(r'deferredRegister\.register\("([^\"]+)"', optional_text):
        helper_items.append({"id": "goety_revelation:" + match.group(1),
                             "source": str(optional_path.relative_to(root)),
                             "line": optional_text.count("\n", 0, match.start()) + 1,
                             "condition": "SafeClass.isEnigmaticLegacyLoaded()",
                             "status": "conditional_source_not_runtime_registry"})
    network_path = mapped_root / "com/mega/revelationfix/common/network/PacketHandler.java"
    network_text = network_path.read_text()
    LOGGER.info("[baseline] outerClasses=%s fixClasses=%s mappedJava=%s libraryImports=%s",
                outer_summary["class_files"], fix_summary["class_files"], len(sources), len(imports))
    return {
        "schema": 1,
        "boundary": "P0 static evidence inventory; not a whole-mod runtime or port completion certificate",
        "input": {"path": jar.name, "sha256": sha256(data), "nested_path": NESTED_PATH, "nested_sha256": sha256(nested_bytes)},
        "outer": outer_summary,
        "fix": fix_summary,
        "source": {
            "raw_java_files": len(list(raw_root.rglob("*.java"))),
            "fix_java_files": len(sources),
            "remaining_method_field_tokens": sum(len(SRG_MEMBER.findall(path.read_text())) for path in sources),
            "parameters": "original_p_names_kept_to_avoid_scope_collisions",
            "readability_not_equivalence": True,
        },
        "mixins": {
            "outer_configured": len(outer_names), "fix_configured": len(fix_names),
            "removed_outer_names": removed,
            "removed_outer_configured": sorted(set(removed) & set(outer_names)),
            "outer_not_in_clear_list": sorted(set(outer_names) - set(removed)),
            "fix_inventory": mixin_inventory,
            "hand_checked_classes": re.findall(r'handCheckMixins\.add\("([^\"]+)"\)', plugin),
            "eligibility": "ApplyCheckMixinConfigPlugin annotations and SafeClass predicates must be evaluated per installed-mod combination",
        },
        "resource_collisions": collisions,
        "ending_library": {
            "path": str(library.relative_to(root)), "commit": git_value(library, "rev-parse", "HEAD"),
            "branch": git_value(library, "branch", "--show-current"),
            "remote_branches": git_value(library, "for-each-ref", "--format=%(refname:short)", "refs/remotes/origin").splitlines(),
            "license": "MIT, retain Copyright (c) 2024 MegaDarkness",
            "java_files": len(list(source_root.rglob("*.java"))),
            "imported_classes": len(imports),
            "importing_java_files": len(set(path for paths in imported_by.values() for path in paths)),
            "missing_class_sources": [entry["class"] for entry in imports if entry["source"] is None],
            "imports": imports,
            "runtime_policy": "no Forge jar added to NeoForge; native equivalents verified module by module",
            "declared_mod_ids": core_ids,
        },
        "registrations": {
            "outer_effective_item_declarations": outer_items,
            "fix_items_literal_declarations": fix_items,
            "fix_helper_and_conditional_items": helper_items,
            "boundary": "literal ITEMS.register source sites only; conditional helpers, ASM and runtime registry may add entries",
            "important": "Fix registers wither_quietus_focus anew after renaming the outer fifth registration to revelation",
        },
        "network": {
            "legacy_channel_registrations": len(re.findall(r"INSTANCE\.registerMessage\s*\(", network_text)),
            "declared_packet_imports": re.findall(r"^import (com\.mega\.revelationfix\.common\.network\.[^;]+);", network_text, re.MULTILINE),
            "migration": "explicit_direction_payload_StreamCodec_main_thread_handler; not yet implemented",
        },
        "implicit_library_tags": {
            "source": invulnerable_path,
            "invulnerable_items": invulnerable_items,
            "behavior": "Ending Library ItemEntity tick protection plus outer unconditional Halo hurt cancellation",
            "boundary": "only two Halo protections implemented in first batch; other tag members pending",
        },
        "first_batch_contract": {
            "mod_id": "goety_neorevelation", "registry_namespace": "goety_revelation",
            "items": {
                "ascension_halo": {"max_stack": 1, "rarity": "EPIC", "fire_resistant": True, "enchantable": False},
                "broken_halo": {"max_stack": 1, "rarity": "RARE", "fire_resistant": True, "enchantable": False},
            },
            "config_defaults": CONFIG_DEFAULTS,
            "config_key_scope": "root, based on original ModConfig <clinit>, not General",
            "config_file": "goety_revelation.toml",
            "tooltip": {"shift_count": 12, "alt_count": 19, "shift_before_alt": True,
                        "default_keys": ["tooltip.revelationfix.holdAltEffect", "tooltip.revelationfix.holdShift"]},
            "not_yet_implemented": ["wearer state", "damage reduction", "resurrection", "boss AI", "spells", "full Fix content", "full Library"],
        },
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument("--output", type=Path, required=True)
    options = parser.parse_args()
    logging.basicConfig(level=logging.INFO, format="%(levelname)s %(message)s")
    if not options.output.parent.is_dir():
        raise ValueError("Output parent must already exist")
    result = collect(options.root)
    options.output.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    LOGGER.info("[baseline] saved=%s collisions=%s missingLibraryClasses=%s",
                options.output, len(result["resource_collisions"]["different"]),
                len(result["ending_library"]["missing_class_sources"]))


if __name__ == "__main__":
    main()
