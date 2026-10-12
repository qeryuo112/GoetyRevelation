#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
P1 限定纵切的静态基线测试（标准 unittest）。

这些断言验证“限定注册 / 配置 / 提示 / 掉落物免疫”四条纵切在源码与资源层面就位，
不启动游戏（游戏内行为由 runGameTestServer 的 GameTest 覆盖）。

若在本批实施前的空骨架（仅有 GoetyNeoRevelation.java 占位、无 ModItems / ModConfig /
mixin / 资源）上运行，绝大多数断言会失败——这正是“测试先行、证明旧骨架缺失失败”的机制。
实施并写入源码/资源后，本文件应全部通过。

注意：本测试只检查静态结构，不声称 combat/state/damage、Shader/AI/全实体已实现。
"""

import json
import os
import re
import unittest

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "src", "main", "java", "z1gned", "goetyneorevelation")
RES = os.path.join(ROOT, "src", "main", "resources")


def read(path):
    with open(path, "r", encoding="utf-8") as f:
        return f.read()


def exists(path):
    return os.path.isfile(path)


class TestRegistration(unittest.TestCase):
    def test_legacy_namespace_and_deferred_register(self):
        main = read(os.path.join(SRC, "GoetyNeoRevelation.java"))
        moditems = read(os.path.join(SRC, "item", "ModItems.java"))
        self.assertIn('LEGACY_NAMESPACE = "goety_revelation"', main,
                       "应在入口定义旧命名空间 goety_revelation")
        # 注册表必须使用旧命名空间（与 MOD_ID 不同）
        self.assertIn("createItems(GoetyNeoRevelation.LEGACY_NAMESPACE)", moditems,
                       "物品 DeferredRegister 应使用 LEGACY_NAMESPACE")

    def test_items_registered_with_expected_ids(self):
        mod = read(os.path.join(SRC, "item", "ModItems.java"))
        self.assertIn('ITEMS.register("ascension_halo"', mod,
                       "应注册 goety_revelation:ascension_halo")
        self.assertIn('ITEMS.register("broken_halo"', mod,
                       "应注册 goety_revelation:broken_halo")
        self.assertIn("AscensionHalo::new", mod)
        self.assertIn("BrokenAscensionHalo::new", mod)

    def test_items_implement_icurioitem_and_props(self):
        asc = read(os.path.join(SRC, "item", "AscensionHalo.java"))
        brk = read(os.path.join(SRC, "item", "BrokenAscensionHalo.java"))
        self.assertIn("implements ICurioItem", asc)
        self.assertIn("implements ICurioItem", brk)
        # 属性：fireResistant + stacksTo(1) + 对应稀有度
        self.assertIn("fireResistant()", asc)
        self.assertIn("stacksTo(1)", asc)
        self.assertIn("Rarity.EPIC", asc)
        self.assertIn("fireResistant()", brk)
        self.assertIn("stacksTo(1)", brk)
        self.assertIn("Rarity.RARE", brk)
        # 不可附魔
        self.assertIn("isEnchantable", asc)
        self.assertIn("isEnchantable", brk)

    def test_namespace_differs_from_modid(self):
        main = read(os.path.join(SRC, "GoetyNeoRevelation.java"))
        self.assertIn('MOD_ID = "goety_neorevelation"', main)
        self.assertIn('LEGACY_NAMESPACE = "goety_revelation"', main)
        self.assertNotEqual("goety_neorevelation", "goety_revelation")


class TestResources(unittest.TestCase):
    def test_models_exist_and_reference_correct_texture(self):
        for name, tex in (("ascension_halo", "item/curio/halo"),
                          ("broken_halo", "item/curio/broken_halo")):
            model = os.path.join(RES, "assets", "goety_revelation", "models", "item", name + ".json")
            self.assertTrue(exists(model), f"模型缺失: {name}.json")
            data = json.loads(read(model))
            self.assertEqual("minecraft:item/generated", data.get("parent"))
            self.assertEqual("goety_revelation:" + tex, data["textures"]["layer0"])

    def test_textures_exist_nonempty(self):
        for name in ("halo.png", "broken_halo.png"):
            p = os.path.join(RES, "assets", "goety_revelation", "textures", "item", "curio", name)
            self.assertTrue(exists(p), f"纹理缺失: {name}")
            self.assertGreater(os.path.getsize(p), 0, f"纹理为空: {name}")

    def test_lang_files_present_with_required_keys(self):
        required = [
            "item.goety_revelation.ascension_halo",
            "item.goety_revelation.broken_halo",
            "tooltip.revelationfix.holdAltEffect",
            "tooltip.revelationfix.holdShift",
            "tooltip.goety_revelation.tip1",
            "tooltip.goety_revelation.tip12",
            "tooltip.goety_revelation.tip1.ability",
            "tooltip.goety_revelation.tip19.ability",
            "tooltip.goety_revelation.tip13",
        ]
        for lang in ("en_us", "zh_cn", "zh_tw"):
            p = os.path.join(RES, "assets", "goety_revelation", "lang", lang + ".json")
            self.assertTrue(exists(p), f"语言文件缺失: {lang}.json")
            data = json.loads(read(p))
            for k in required:
                self.assertIn(k, data, f"{lang}.json 缺少键 {k}")

    def test_curios_head_tag_contains_two_halos(self):
        p = os.path.join(RES, "data", "curios", "tags", "item", "head.json")
        self.assertTrue(exists(p), "curios head 标签缺失")
        data = json.loads(read(p))
        vals = data.get("values", [])
        self.assertIn("goety_revelation:ascension_halo", vals)
        self.assertIn("goety_revelation:broken_halo", vals)

    def test_mod_pack_uses_loader_optional_format(self):
        p = os.path.join(RES, "pack.mcmeta")
        self.assertTrue(exists(p), "pack.mcmeta 缺失")
        data = json.loads(read(p))
        self.assertIn("description", data["pack"])
        # NeoForge ResourcePackLoader.OPTIONAL_FORMAT 支持内置 mod pack 省略格式，
        # 分别按 resource 34/data 48 加载，不把两种类型都声明成 34。
        self.assertNotIn("pack_format", data["pack"])
        self.assertNotIn("supported_formats", data["pack"])


class TestConfig(unittest.TestCase):
    def test_seven_root_keys_with_666_and_build_last(self):
        cfg = read(os.path.join(SRC, "config", "ModConfig.java"))
        simple_keys = ("player_invincible", "player_ambient_sound", "hellfire_damage_amount",
                      "hellcloud_damage_amount", "apollyon_health", "apollyon_hurt_limit")
        for key in simple_keys:
            self.assertIn('define("%s"' % key, cfg, f"配置应定义根级键 {key}")
        # summon 使用 defineInRange
        self.assertIn('defineInRange("summon_apollyon_random_value"', cfg,
                       "summon 应使用 defineInRange")
        # apollyon_health 默认 666（RevelationFix 修改值，非 560）
        self.assertIn('define("apollyon_health", 666.0)', cfg,
                       "apollyon_health 默认应为 666")
        # 召唤值范围为 [1, Integer.MAX_VALUE]
        self.assertIn("defineInRange(\"summon_apollyon_random_value\", 100, 1, Integer.MAX_VALUE)", cfg)
        # define 之后最后 build，且为根级（不应出现 push/pop 包裹键）
        self.assertIn("SPEC = BUILDER.build()", cfg)
        self.assertNotIn("BUILDER.push(", cfg, "七项键应为根级，不应套用分类 push")
        self.assertNotIn("BUILDER.pop()", cfg)

    def test_register_config_common_with_legacy_toml(self):
        main = read(os.path.join(SRC, "GoetyNeoRevelation.java"))
        # 通过 ModContainer 注册 COMMON 配置，且沿用原 goety_revelation.toml
        # （NeoForge 21.1 下 ModConfig 为 fml 与本项目配置类的同名导入，使用全限定名区分）
        self.assertIn("registerConfig(", main, "应通过 ModContainer 注册配置")
        self.assertIn("Type.COMMON", main, "应注册 COMMON 类型配置")
        self.assertIn("ModConfig.SPEC", main, "应注册本项目七项配置 SPEC")
        self.assertIn('"goety_revelation.toml"', main,
                       "配置文件名应沿用原 goety_revelation.toml")


class TestClientIsolation(unittest.TestCase):
    def test_common_classes_do_not_reference_screen(self):
        common_files = [
            "GoetyNeoRevelation.java",
            "HaloTooltipKeys.java",
            "item/ModItems.java",
            "config/ModConfig.java",
            "item/AscensionHalo.java",
            "item/BrokenAscensionHalo.java",
        ]
        for rel in common_files:
            src = read(os.path.join(SRC, rel))
            self.assertNotIn("net.minecraft.client.gui.screens.Screen", src,
                              f"公共类不应引用 Screen: {rel}")

    def test_client_handler_holds_screen_and_is_in_client_pkg(self):
        client = read(os.path.join(SRC, "client", "ClientHaloTooltip.java"))
        self.assertIn("net.minecraft.client.gui.screens.Screen", client,
                      "客户端处理器应读取 Screen 按键")
        self.assertIn("HaloTooltipKeys.completeLines", client,
                      "应通过 HaloTooltipKeys 选择提示")
        self.assertIn("ItemTooltipEvent", client)

    def test_pure_tooltip_function_present(self):
        keys = read(os.path.join(SRC, "HaloTooltipKeys.java"))
        self.assertIn("public static List<Component> completeLines(boolean shift, boolean alt)", keys)
        self.assertIn("public static List<Component> brokenLines()", keys)
        # Shift 优先：先判断 shift 再 alt
        idx_shift = keys.index("if (shift)")
        idx_alt = keys.index("} else if (alt)")
        self.assertLess(idx_shift, idx_alt, "Shift 分支应优先于 Alt 分支")


class TestMixinMetadata(unittest.TestCase):
    def test_mixin_config_present_and_well_formed(self):
        p = os.path.join(RES, "goety_neorevelation.mixins.json")
        self.assertTrue(exists(p), "mixin 配置缺失")
        data = json.loads(read(p))
        self.assertTrue(data.get("required"), "mixin config required 应为 true")
        self.assertIn("ItemEntityMixin", data.get("mixins", []),
                      "应声明 ItemEntityMixin")
        self.assertIn("JAVA_21", data.get("compatibilityLevel", ""),
                      "compatibilityLevel 应为 JAVA_21")
        # minVersion 形如 "0.8.5"，取主版本数值比较
        minver = tuple(int(x) for x in str(data.get("minVersion", "0")).split("."))
        self.assertGreaterEqual(minver, (0, 8), "minVersion 应 >= 0.8")
        # 此批官方名 remap=false，无映射处理器生成文件，不声明不存在的 refmap。
        self.assertNotIn("refmap", data)
        self.assertEqual(data.get("injectors", {}).get("defaultRequire"), 1)
        metadata = read(os.path.join(RES, "META-INF", "neoforge.mods.toml"))
        self.assertIn("[[mixins]]", metadata)
        self.assertIn('config = "goety_neorevelation.mixins.json"', metadata)

    def test_item_entity_mixin_uses_official_descriptor(self):
        mx = read(os.path.join(SRC, "mixin", "ItemEntityMixin.java"))
        # @Mixin无require参数；require=1属于@Inject，不能用配置required替代。
        self.assertIn("require = 1", mx)
        self.assertIn('method = "tick()V"', mx)
        self.assertIn("Integer.MAX_VALUE", mx)
        self.assertIn("@Unique", mx)
        self.assertIn("remap = false", mx, "应使用官方映射 remap=false")
        self.assertIn('method = "hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"',
                      mx, "应注入官方 hurt(DamageSource, float) 描述符")
        self.assertIn("CallbackInfoReturnable<Boolean>", mx)
        # 仅作用于两个光环（按 ResourceLocation 判定）
        self.assertIn('"ascension_halo"', mx)
        self.assertIn('"broken_halo"', mx)
        self.assertIn("setReturnValue(false)", mx)


class TestDevelopmentBoundary(unittest.TestCase):
    def test_release_jar_excludes_game_tests(self):
        gradle = read(os.path.join(ROOT, "build.gradle"))
        self.assertIn("exclude 'z1gned/goetyneorevelation/gametest/**'", gradle)
        self.assertIn("exclude 'data/goety_neorevelation/structure/empty.nbt'", gradle)

    def test_game_test_lifetime_has_boundary_and_control_group(self):
        test = read(os.path.join(SRC, "gametest", "P1GameTests.java"))
        self.assertIn("dropped_halo_lifetime", test)
        self.assertIn("5999, 6000, 32767", test)
        self.assertIn("readAdditionalSaveData", test)
        self.assertIn("stone.isRemoved()", test)

    def test_config_load_and_reload_logging_is_registered(self):
        config = read(os.path.join(SRC, "config", "ModConfig.java"))
        main = read(os.path.join(SRC, "GoetyNeoRevelation.java"))
        self.assertIn("ModConfigEvent.Loading", config)
        self.assertIn("ModConfigEvent.Reloading", config)
        self.assertIn("registerLifecycle", main)
        self.assertIn("apollyonHealth", config)


if __name__ == "__main__":
    unittest.main(verbosity=2)
