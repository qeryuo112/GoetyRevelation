"""P2 contract gates: source checks complement real game damage tests."""
import json
import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/z1gned/goetyneorevelation"


class P2CombatContract(unittest.TestCase):
    def read(self, path):
        file = JAVA / path
        self.assertTrue(file.is_file(), "Missing P2 implementation: " + path)
        return file.read_text()

    def test_curios_equipment_not_backpack_uses_server_service(self):
        text = self.read("combat/HaloEquipment.java")
        self.assertIn("CuriosApi.getCuriosInventory", text)
        self.assertIn("findFirstCurio", text)
        self.assertNotIn("getInventory()", text)

    def test_damage_container_and_player_override_have_distinct_stages(self):
        living = self.read("mixin/HaloLivingMixin.java")
        player = self.read("mixin/HaloPlayerMixin.java")
        self.assertIn("damageContainers.peek()", living)
        self.assertIn("setNewDamage", living)
        self.assertIn("beforeArmor", living)
        self.assertIn("damageContainers.peek()", player)
        self.assertIn("setNewDamage", player)
        self.assertIn("beforeArmor", player)
        self.assertIn("modifyPlayerEntryDamage", player)
        self.assertIn("require = 1", player)
        self.assertIn("require = 1", living)

    def test_revival_preserves_legacy_not_invented_totem_priority(self):
        service = self.read("combat/HaloCombat.java")
        self.assertIn("36000", service)
        self.assertIn("6000", service)
        self.assertIn("400", service)
        self.assertIn("SummonCircle", service)
        self.assertIn("ModEntities.WITHER_SERVANT", service)
        self.assertIn("getMaxHealth() / 2.0F", service)
        mixin = self.read("mixin/HaloLivingMixin.java")
        self.assertIn("checkTotemDeathProtection", mixin)
        self.assertIn('at = @At("RETURN")', mixin)
        self.assertNotIn("!callback.getReturnValue", mixin)

    def test_original_freeze_and_one_server_tick(self):
        service = self.read("combat/HaloCombat.java")
        self.assertIn("PlayerTickEvent.Post", service)
        self.assertIn("level().isClientSide", service)
        self.assertIn("getHaloReviveCooldown", service)
        self.assertIn("getMeteorTime", service)
        self.assertIn("NetherMeteor", service)
        self.assertIn("0.8F", service)
        self.assertIn("LichdomHelper.isInLichMode", service)

    def test_configuration_and_effect_bypass_contract(self):
        config = self.read("config/HaloConfig.java")
        self.assertIn('push("Halo of Ascension")', config)
        self.assertIn('defineInRange("haloDamageCap", 20.0', config)
        self.assertIn("whitelistMobEffects", config)
        service = self.read("combat/HaloCombat.java")
        self.assertIn("MobEffectEvent.Applicable", service)
        self.assertIn("MobEffects.BAD_OMEN", service)
        self.assertIn("MobEffects.NIGHT_VISION", service)

    def test_native_attachment_integration_and_core_injections(self):
        main = self.read("GoetyNeoRevelation.java")
        self.assertIn("HaloAttachments.register", main)
        self.assertIn("ModEntities.register", main)
        cfg = json.loads((ROOT / "src/main/resources/goety_neorevelation.mixins.json").read_text())
        self.assertIn("HaloLivingMixin", cfg["mixins"])
        self.assertIn("HaloPlayerMixin", cfg["mixins"])
        self.assertEqual(cfg["injectors"]["defaultRequire"], 1)

    def test_game_test_is_real_equipment_damage_and_controls(self):
        text = self.read("gametest/P2CombatGameTests.java")
        self.assertIn("getStacksHandler", text)
        self.assertIn(".hurt(", text)
        self.assertIn("Items.TOTEM_OF_UNDYING", text)
        self.assertIn("fellOutOfWorld", text)
        self.assertIn("Attributes.ARMOR", text)
        self.assertIn("setAbsorptionAmount", text)


if __name__ == "__main__":
    unittest.main()
