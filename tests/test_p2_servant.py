#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
P2 实体（WitherServant）移植的静态基线测试（标准 unittest）。

这些断言验证从原 Forge 1.20.1 WitherServant 移植到 NeoForge 1.21.1 的关键契约在源码与资源层面就位，
不启动游戏（游戏内行为由 runGameTestServer 的 P2EntityGameTests 覆盖）。

若在本批实施前的空骨架（仅有占位、无 entity/ModEntities/客户端渲染/GameTest/undead 标签）上运行，
绝大多数断言会失败——这正是“测试先行、证明旧骨架缺失失败”的机制。实施并写入源码/资源后，本文件应全部通过。

注意：本测试只检查 P2 实体移植的静态结构；召唤（SummonCircle）、光环状态同步与战斗逻辑由其他代理负责。
"""

import json
import os
import unittest

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "src", "main", "java", "z1gned", "goetyneorevelation")
RES = os.path.join(ROOT, "src", "main", "resources")


def read(path):
    with open(path, "r", encoding="utf-8") as f:
        return f.read()


def exists(path):
    return os.path.isfile(path)


ENTITY = os.path.join(SRC, "entity", "WitherServant.java")
MODENT = os.path.join(SRC, "entity", "ModEntities.java")
RENDERER = os.path.join(SRC, "client", "render", "WitherServantRenderer.java")
CLIENT_EVENTS = os.path.join(SRC, "client", "P2EntityClientEvents.java")
GAMETEST = os.path.join(SRC, "gametest", "P2EntityGameTests.java")
UNDEAD_TAG = os.path.join(RES, "data", "minecraft", "tags", "entity_type", "undead.json")


class TestWitherServantPort(unittest.TestCase):
    def test_file_present(self):
        self.assertTrue(exists(ENTITY), "entity/WitherServant.java 缺失")

    def test_extends_summoned_and_interfaces(self):
        src = read(ENTITY)
        self.assertIn("extends Summoned", src, "应继承 Goety 的 Summoned（不能用原版凋灵或 Goety 骷髅代替）")
        self.assertIn("implements PowerableMob, RangedAttackMob, IMobTyped", src,
                      "应实现 PowerableMob / RangedAttackMob / IMobTyped")
        self.assertIn("com.Polarice3.Goety.common.entities.ally.Summoned", src)
        self.assertIn("com.Polarice3.Goety.api.entities.IMobTyped", src)

    def test_neoforge_synched_data_builder(self):
        src = read(ENTITY)
        self.assertIn("protected void defineSynchedData(SynchedEntityData.Builder", src,
                      "应使用 NeoForge 的 defineSynchedData(SynchedEntityData.Builder)")

    def test_forgetul_factory_replaced_by_eventhooks(self):
        src = read(ENTITY)
        self.assertNotIn("ForgeEventFactory", src, "不应残留未测试的 Forge ForgeEventFactory 包")
        self.assertIn("EventHooks.canEntityGrief", src, "应使用 EventHooks.canEntityGrief")
        self.assertIn("EventHooks.onEntityDestroyBlock", src, "应使用 EventHooks.onEntityDestroyBlock")

    def test_color_particle_option(self):
        src = read(ENTITY)
        self.assertIn("ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT", src,
                      "发光粒子应使用 ColorParticleOption（原三色 0.7/0.7/0.5 与 0.7/0.7/0.9）")
        self.assertIn("0.699999988079071F, 0.699999988079071F, 0.5F", src)
        self.assertIn("0.699999988079071F, 0.699999988079071F, 0.8999999761581421F", src)

    def test_get_entities_not_nearby(self):
        src = read(ENTITY)
        self.assertNotIn("getNearbyEntities(", src, "NeoForge 无 getNearbyEntities，应使用 getEntities")
        self.assertIn("getEntities(", src)

    def test_drop_custom_death_loot_signature(self):
        src = read(ENTITY)
        self.assertIn("protected void dropCustomDeathLoot(ServerLevel", src,
                      "死亡掉落签名应为 dropCustomDeathLoot(ServerLevel, DamageSource, boolean)")
        self.assertIn("Items.NETHER_STAR", src, "应掉落下界之星")
        self.assertIn("setExtendedLifetime", src)

    def test_can_change_dimensions_signature(self):
        src = read(ENTITY)
        self.assertIn("public boolean canChangeDimensions(Level", src,
                      "应覆盖 canChangeDimensions(Level, Level)")

    def test_invulnerable_to_kept(self):
        src = read(ENTITY)
        self.assertIn("public boolean isInvulnerableTo(DamageSource arg0)", src)
        self.assertIn("arg0.is(DamageTypeTags.IS_FALL) && super.isInvulnerableTo(arg0)", src,
                      "原 isInvulnerableTo 怪异实现（IS_FALL && super）应保留，不可静默修改")

    def test_mob_type_undead_and_goety(self):
        src = read(ENTITY)
        self.assertNotIn("net.minecraft.world.entity.MobType", src)
        self.assertIn("MobTypeHelper.getMobType", src)
        self.assertIn("public ModMobType getGoetyMobType()", src)
        self.assertIn("return ModMobType.UNDEAD;", src, "应实现 IMobTyped.getGoetyMobType 返回 ModMobType.UNDEAD")

    def test_attributes_300_and_4_armor(self):
        src = read(ENTITY)
        self.assertIn("public static AttributeSupplier.Builder createAttributes()", src)
        self.assertIn("Monster.createMonsterAttributes()", src)
        self.assertIn("Attributes.MAX_HEALTH, 300.0", src, "应保留 300 生命")
        self.assertIn("Attributes.ARMOR, 4.0", src, "应保留 4.0 护甲")
        self.assertIn("Attributes.FLYING_SPEED", src, "应保留飞行速度属性")

    def test_is_powered_and_targets(self):
        src = read(ENTITY)
        self.assertIn("public boolean isPowered()", src)
        self.assertIn("getHealth() <= this.getMaxHealth() / 2.0F", src)
        self.assertIn("getAlternativeTarget", src)
        self.assertIn("setAlternativeTarget", src)
        self.assertIn("getInvulnerableTicks", src)
        self.assertIn("setInvulnerableTicks", src)

    def test_wither_skull_vec3_direction(self):
        src = read(ENTITY)
        self.assertIn("new WitherSkull(this.level(), this, new Vec3(", src,
                      "WitherSkull 应使用 (Level, LivingEntity, Vec3 方向) 构造")
        self.assertIn("witherskull.setOwner(this)", src)
        self.assertIn("witherskull.setPosRaw(", src)

    def test_ranged_attack_and_hurt_logic(self):
        src = read(ENTITY)
        self.assertIn("public void performRangedAttack(LivingEntity arg0, float arg1)", src)
        self.assertIn("arg0.getEntity() instanceof WitherServant", src, "同类（WitherServant）不应互伤")
        self.assertIn("entity instanceof AbstractArrow", src, "半血时箭应免疫")
        self.assertIn("this.destroyBlocksTick = 20;", src)


class TestUndeadTag(unittest.TestCase):
    def test_undead_tag_only_self(self):
        self.assertTrue(exists(UNDEAD_TAG), "data/minecraft/tags/entity_type/undead.json 缺失")
        data = json.loads(read(UNDEAD_TAG))
        vals = data.get("values", [])
        self.assertIn("goety_revelation:wither_servant", vals,
                      "undead 标签应包含本实体")
        self.assertEqual(len(vals), 1, "undead 标签应仅含本实体，不污染原版/其它实体")


class TestModEntities(unittest.TestCase):
    def test_file_present(self):
        self.assertTrue(exists(MODENT), "entity/ModEntities.java 缺失")

    def test_deferred_register_legacy_namespace(self):
        src = read(MODENT)
        self.assertIn("DeferredRegister.create(Registries.ENTITY_TYPE, GoetyNeoRevelation.LEGACY_NAMESPACE)", src,
                      "应使用旧命名空间 goety_revelation 注册实体")
        self.assertIn("GoetyNeoRevelation.LEGACY_NAMESPACE", src)

    def test_wither_servant_registration(self):
        src = read(MODENT)
        self.assertIn('ENTITY_TYPE.register("wither_servant"', src)
        self.assertIn(".sized(0.9F, 3.5F)", src, "尺寸应为 0.9 x 3.5")
        self.assertIn(".clientTrackingRange(8)", src, "clientTrackingRange 应为 8")

    def test_register_bus_and_attribute_event(self):
        src = read(MODENT)
        self.assertIn("public static void register(IEventBus", src, "应提供 static register(IEventBus)")
        self.assertIn("ENTITY_TYPE.register(modEventBus)", src)
        self.assertIn("EntityAttributeCreationEvent", src)
        self.assertIn("WitherBoss.createAttributes().build()", src,
                      "属性必须保留原ModMain真正使用的WitherBoss注册基线")


class TestClientRegistration(unittest.TestCase):
    def test_renderer_present(self):
        self.assertTrue(exists(RENDERER), "client/render/WitherServantRenderer.java 缺失")
        src = read(RENDERER)
        self.assertIn("extends MobRenderer<WitherServant, WitherServantModel<WitherServant>>", src)
        self.assertIn("ModelLayerLocation LAYER", src, "应定义模型层常量供注册")
        self.assertIn("getTextureLocation", src)
        self.assertIn("getInvulnerableTicks", src, "纹理应随 invul 状态切换")
        self.assertIn("addLayer(new WitherServantArmorLayer", src, "应叠加半血能量护甲层")

    def test_client_events_present(self):
        self.assertTrue(exists(CLIENT_EVENTS), "client/P2EntityClientEvents.java 缺失")
        src = read(CLIENT_EVENTS)
        self.assertIn("@EventBusSubscriber(modid = GoetyNeoRevelation.MOD_ID", src)
        self.assertIn("value = Dist.CLIENT", src, "应在客户端自动订阅")
        self.assertIn("registerLayerDefinition(WitherServantRenderer.LAYER", src, "应注册模型层")
        self.assertIn("registerEntityRenderer(ModEntities.WITHER_SERVANT.get()", src, "应注册渲染器")
        self.assertIn("WitherServantModel::createBodyLayer", src)


class TestGameTests(unittest.TestCase):
    def test_file_present_and_annotations(self):
        self.assertTrue(exists(GAMETEST), "gametest/P2EntityGameTests.java 缺失")
        src = read(GAMETEST)
        self.assertIn("@GameTestHolder(GoetyNeoRevelation.MOD_ID)", src)
        self.assertIn("@PrefixGameTestTemplate(false)", src, "应关闭前缀，使 empty 解析为 goety_neorevelation:empty")
        self.assertIn('template = "empty"', src)

    def test_required_test_methods(self):
        src = read(GAMETEST)
        for m in ("wither_servant_registry", "wither_servant_attributes", "wither_servant_owner",
                 "wither_servant_synched_save", "wither_servant_undead",
                 "wither_servant_arrow_immune_when_powered", "wither_servant_projectile_owner"):
            self.assertIn("public static void %s(GameTestHelper helper)" % m, src,
                          "应实现 GameTest: %s" % m)

    def test_registry_id_assertion(self):
        src = read(GAMETEST)
        self.assertIn("goety_revelation:wither_servant", src, "应断言旧 id goety_revelation:wither_servant")
        self.assertIn("ModEntities.WITHER_SERVANT.getId()", src)

    def test_attributes_and_undead_assertions(self):
        src = read(GAMETEST)
        self.assertIn("wither.getMaxHealth() - 300.0F", src, "应断言 300 生命")
        self.assertIn("Attributes.ARMOR", src)
        self.assertIn("MobTypeHelper.getMobType(wither) == ModMobType.UNDEAD", src, "应断言 Goety 亡灵归类")
        self.assertIn("EntityTypeTags.UNDEAD", src, "应通过 minecraft:undead 标签判定")

    def test_owner_and_projectile_assertions(self):
        src = read(GAMETEST)
        self.assertIn("((IOwned) wither).setTrueOwner(owner)", src, "应验证实际生成主人")
        self.assertIn("performRangedAttack(target, 0.0F)", src, "应触发攻击生成投射物")
        self.assertIn("WitherSkull", src)
        self.assertIn("s.getOwner() == wither", src, "投射物应有 owner 且为召唤者")


if __name__ == "__main__":
    unittest.main(verbosity=2)
