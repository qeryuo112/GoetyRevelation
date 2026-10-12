"""P0 inventory tests; no Minecraft launch or package download is needed."""
import importlib.util
import io
import json
import unittest
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MODULE_PATH = ROOT / "tools/port_baseline.py"
if MODULE_PATH.exists():
    spec = importlib.util.spec_from_file_location("port_baseline", MODULE_PATH)
    baseline = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(baseline)
else:
    baseline = None


def archive(entries):
    result = io.BytesIO()
    with zipfile.ZipFile(result, "w") as package:
        for name, content in entries.items():
            package.writestr(name, content)
    return zipfile.ZipFile(io.BytesIO(result.getvalue()))


class InventoryTests(unittest.TestCase):
    def setUp(self):
        self.assertIsNotNone(baseline, "P0 baseline inventory tool has not been implemented")

    def test_nested_classes_are_not_counted_as_outer_classes(self):
        inner = archive({"fix/Inner.class": b"x", "fix/Inner$Nested.class": b"y"})
        outer = archive({"main/Main.class": b"z", "META-INF/jarjar/fix.jar": b"jar"})
        self.assertEqual(baseline.archive_summary(outer)["class_files"], 1)
        self.assertEqual(baseline.archive_summary(inner)["class_files"], 2)

    def test_resource_override_and_identical_resource_are_distinct(self):
        outer = archive({"assets/test/lang/en_us.json": b"old", "data/test/recipe/a.json": b"same"})
        inner = archive({"assets/test/lang/en_us.json": b"new", "data/test/recipe/a.json": b"same"})
        result = baseline.resource_overlaps(outer, inner)
        self.assertEqual(result["different"], ["assets/test/lang/en_us.json"])
        self.assertEqual(result["identical"], ["data/test/recipe/a.json"])
        self.assertIn("not_runtime_pack_precedence", result["boundary"])

    def test_registration_inventory_preserves_line_and_is_not_runtime_claim(self):
        text = 'A = ITEMS.register("first", X::new);\nB = ITEMS.register(\n "second", Y::new);'
        result = baseline.literal_item_registrations(text, "sample.java", "legacy")
        self.assertEqual([value["id"] for value in result], ["legacy:first", "legacy:second"])
        self.assertEqual([value["line"] for value in result], [1, 2])
        self.assertTrue(all(value["status"] == "source_declaration_not_runtime_registry" for value in result))

    def test_frozen_jar_and_mapped_references(self):
        result = baseline.collect(ROOT)
        self.assertEqual(result["input"]["sha256"], "da200c27de7a78dc7f0b2e429d59cb9fdca341644da4cb7dd7cd5b50bd57f706")
        self.assertEqual(result["outer"]["class_files"], 112)
        self.assertEqual(result["fix"]["class_files"], 878)
        self.assertEqual(result["source"]["fix_java_files"], 733)
        self.assertEqual(result["source"]["remaining_method_field_tokens"], 0)
        self.assertEqual(result["mixins"]["outer_configured"], 42)
        self.assertEqual(result["mixins"]["fix_configured"], 220)
        patchouli = next(entry for entry in result["mixins"]["fix_inventory"]
                         if entry["class"] == "patchouli.BrewingCatalystProcessorMixin")
        self.assertEqual(patchouli["required_mod_annotations"], ["patchouli"])
        self.assertIn("hand_checked_classes", result["mixins"])
        self.assertEqual(result["network"]["legacy_channel_registrations"], 13)
        self.assertIn("goety_revelation:ascension_halo", result["implicit_library_tags"]["invulnerable_items"])
        self.assertEqual(len(result["mixins"]["removed_outer_configured"]), 24)
        self.assertEqual(len(result["mixins"]["outer_not_in_clear_list"]), 18)
        self.assertEqual(result["ending_library"]["commit"], "562c04751521802939d9ccc8201e7602841bb02b")
        ids = [entry["id"] for entry in result["registrations"]["outer_effective_item_declarations"]]
        self.assertIn("goety_revelation:revelation", ids)
        fix_ids = [entry["id"] for entry in result["registrations"]["fix_items_literal_declarations"]]
        self.assertIn("goety_revelation:wither_quietus_focus", fix_ids)
        self.assertIn("goety_revelation:quietus_star", fix_ids)
        extra_ids = [entry["id"] for entry in result["registrations"]["fix_helper_and_conditional_items"]]
        self.assertEqual(extra_ids, ["goety_revelation:rune_reactor",
                                    "goety_revelation:runestone_engraved_table",
                                    "goety_revelation:blessing_scroll"])
        self.assertNotIn("General.apollyon_health", result["first_batch_contract"]["config_defaults"])
        self.assertEqual(result["first_batch_contract"]["config_defaults"]["apollyon_health"], 666.0)


if __name__ == "__main__":
    unittest.main()
