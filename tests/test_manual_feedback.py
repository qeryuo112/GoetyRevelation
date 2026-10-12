"""Source gates supplement real /give, overflow pickup and undead-AI GameTests."""
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/z1gned/goetyneorevelation"


class ManualFeedbackContract(unittest.TestCase):
    def test_fake_display_exclusion_and_saved_ghost_recovery(self):
        text = (JAVA / "mixin/ItemEntityMixin.java").read_text()
        self.assertIn("pickupDelay", text)
        self.assertIn("getAge()", text)
        self.assertIn("getEntityLifespan", text)
        self.assertIn("Short.MAX_VALUE", text)
        self.assertIn("[state] restore command display expiry", text)
        self.assertIn("Integer.MAX_VALUE", text)

    def test_undead_suppression_matches_original_config_not_permanent_invulnerability(self):
        config = (JAVA / "config/HaloConfig.java").read_text()
        self.assertIn('define("haloUndeadSuppression", true)', config)
        service = (JAVA / "combat/HaloCombat.java").read_text()
        self.assertIn("suppressUndeadAttack", service)
        self.assertIn("MobTypeHelper", service)
        self.assertIn("ModMobType.UNDEAD", service)
        self.assertIn("LICH_NEUTRAL", service)
        self.assertIn("LichPowerfulFoesHealth", service)
        self.assertIn("instanceof Apostle", service)
        self.assertIn("[combat] suppress undead target", service)
        # Existing revive timings and damage semantics are not being redesigned.
        for value in ("36000", "6000", "400"):
            self.assertIn(value, service)

    def test_exact_can_attack_hook_is_required(self):
        text = (JAVA / "mixin/HaloLivingMixin.java").read_text()
        self.assertIn('method = "canAttack(Lnet/minecraft/world/entity/LivingEntity;)Z"', text)
        self.assertIn("suppressUndeadAttack", text)
        self.assertIn("require = 1", text)
        self.assertIn("callback.setReturnValue(false)", text)

    def test_real_commands_pickup_and_control_tests_exist(self):
        test = (JAVA / "gametest/ManualFeedbackGameTests.java").read_text()
        for token in ("performPrefixedCommand", "give @s", "playerTouch", "makeFakeItem",
                      "readAdditionalSaveData", "TargetingConditions.forCombat", "Apostle",
                      "saved_give_ghost", "countItem", "getStacksHandler"):
            self.assertIn(token, test)


if __name__ == "__main__":
    unittest.main(verbosity=2)
