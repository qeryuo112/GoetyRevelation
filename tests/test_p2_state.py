import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/z1gned/goetyneorevelation"

class P2StateContract(unittest.TestCase):
    def test_state_preserves_legacy_keys_and_missing_defaults(self):
        path = JAVA / "state/HaloState.java"
        self.assertTrue(path.exists(), "P2 HaloState missing")
        text = path.read_text()
        for key in ["PurpleInvulnerableTime", "HaloRvvCooldown", "RvvMeteorTime", "OdamaneExpandedFlags", "ArmorClimbing", "ExtraMagicShield", "PreparingEndRitual", "spectreDarkmageMagicShieldCooldown"]:
            self.assertIn(key, text)
        self.assertIn("INBTSerializable<CompoundTag>", text)
        self.assertIn("contains(\"ArmorClimbing\")", text)
        self.assertIn("STREAM_CODEC", text)
        self.assertIn("unknownData", text)

    def test_native_registered_serializable_sync_and_copy(self):
        path = JAVA / "state/HaloAttachments.java"
        self.assertTrue(path.exists(), "P2 attachment missing")
        text = path.read_text()
        self.assertIn("copyOnDeath()", text)
        self.assertIn("sync(HaloState.STREAM_CODEC)", text)
        self.assertIn("AttachmentType.serializable", text)
        self.assertIn("ATTACHMENT_TYPES", text)

    def test_target_neoforge_minimum_covers_backported_sync(self):
        properties = (ROOT / "gradle.properties").read_text()
        self.assertIn("neo_version_range=[21.1.259,)", properties,
                      "native sync uses validated .259 API; must not advertise all 21.1.0+")

    def test_state_gametests_assert_roundtrip_and_isolation(self):
        path = JAVA / "gametest/P2StateGameTests.java"
        self.assertTrue(path.exists(), "P2 state GameTests missing")
        text = path.read_text()
        self.assertIn("copyAttachmentsFrom", text)
        self.assertIn("RegistryFriendlyByteBuf", text)
        self.assertIn("deserializeNBT", text)
        self.assertIn(" != ", text)

if __name__ == "__main__": unittest.main()
