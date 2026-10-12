"""Regression tests for mapping parsers, including unobfuscated class owners."""
import importlib.util
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SPEC = importlib.util.spec_from_file_location("srg_map", ROOT / "tools/build_srg_map.py")
MODULE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MODULE)


class SrgMapTests(unittest.TestCase):
    def test_proguard_line_prefix(self):
        self.assertEqual(
            MODULE.parse_proguard_sig("955:955:net.minecraft.server.level.ServerLevel overworld()"),
            (True, ("net.minecraft.server.level.ServerLevel", ()), "overworld"),
        )

    def test_library_descriptor_separator(self):
        self.assertEqual(
            MODULE.desc_to_canon("(Ljava/lang/String;[I)Ljava/lang/Iterable;", {}),
            ("java.lang.Iterable", ("java.lang.String", "int[]")),
        )

    def test_unobfuscated_owner_matches_internal_jvm_name(self):
        with tempfile.TemporaryDirectory() as temporary:
            directory = Path(temporary)
            (directory / "joined.tsrg").write_text(
                "tsrg2 obf srg id\n"
                "abc net/minecraft/server/level/ServerLevel 1\n"
                "net/minecraft/server/MinecraftServer net/minecraft/server/MinecraftServer 2\n"
                "\tD ()Labc; m_129783_ 129783\n"
                "\tF ()Ljava/lang/Iterable; m_129785_ 129785\n"
                "\ta (Ldef;)Labc; m_129880_ 129880\n"
                "def net/minecraft/resources/ResourceKey 3\n",
                encoding="utf-8",
            )
            mapping = (
                "net.minecraft.server.level.ServerLevel -> abc:\n"
                "net.minecraft.resources.ResourceKey -> def:\n"
                "net.minecraft.server.MinecraftServer -> net.minecraft.server.MinecraftServer:\n"
                "    955:955:net.minecraft.server.level.ServerLevel overworld() -> D\n"
                "    968:968:java.lang.Iterable getAllLevels() -> F\n"
                "    960:960:net.minecraft.server.level.ServerLevel getLevel(net.minecraft.resources.ResourceKey) -> a\n"
            )
            for name in ("client.txt", "server.txt"):
                (directory / name).write_text(mapping, encoding="utf-8")
            result = subprocess.run(
                [sys.executable, str(ROOT / "tools/build_srg_map.py"),
                 str(directory / "joined.tsrg"), str(directory / "client.txt"),
                 str(directory / "server.txt"), str(directory / "result.tsv")],
                text=True, capture_output=True, check=True,
            )
            actual = {parts[0]: parts[1] for line in (directory / "result.tsv").read_text().splitlines()
                      if len(parts := line.split("\t")) >= 2}
            self.assertEqual(actual.get("m_129783_"), "overworld", result.stdout)
            self.assertEqual(actual.get("m_129785_"), "getAllLevels")
            self.assertEqual(actual.get("m_129880_"), "getLevel")


if __name__ == "__main__":
    unittest.main()
