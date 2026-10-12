package z1gned.goetyneorevelation.gametest;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.Level;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICurio;
import z1gned.goetyneorevelation.GoetyNeoRevelation;
import z1gned.goetyneorevelation.HaloTooltipKeys;
import z1gned.goetyneorevelation.config.ModConfig;
import z1gned.goetyneorevelation.item.ModItems;

/**
 * P1 限定纵切的真实 GameTest（运行于 {@code runGameTestServer}）。
 *
 * <p>覆盖：物品注册/属性、Curios 能力与 head 标签、七项根级配置(666)、
 * 提示分支(12/19/shift优先)、掉落实体免疫 Mixin 真实生效、注册命名空间与 MOD_ID 不同。
 * 不声称 state/damage 战斗、Shader/AI/实体等后续批次已实现。
 *
 * <p>{@link PrefixGameTestTemplate}(false) 关闭类名前缀，使 template="empty" 解析为
 * goety_neorevelation:empty（资源 data/goety_neorevelation/structure/empty.nbt）。
 */
@GameTestHolder(GoetyNeoRevelation.MOD_ID)
@PrefixGameTestTemplate(false)
public class P1GameTests {

    private static final Logger LOGGER = LoggerFactory.getLogger(P1GameTests.class);
    private static final String PREFIX = "[test] ";
    private static final double EPS = 1e-9;

    @GameTest(template = "empty")
    public static void item_registration(GameTestHelper helper) {
        LOGGER.info(PREFIX + "item_registration 开始");
        ResourceLocation ascId = ModItems.ASCENSION_HALO.getId();
        ResourceLocation brkId = ModItems.BROKEN_HALO.getId();
        helper.assertTrue(BuiltInRegistries.ITEM.containsKey(ascId), "ascension_halo 应已注册");
        helper.assertTrue(BuiltInRegistries.ITEM.containsKey(brkId), "broken_halo 应已注册");

        Item asc = ModItems.ASCENSION_HALO.get();
        Item brk = ModItems.BROKEN_HALO.get();
        ItemStack ascStack = new ItemStack(asc);
        ItemStack brkStack = new ItemStack(brk);

        helper.assertTrue(ascStack.getRarity() == Rarity.EPIC, "ascension_halo 稀有度应为 EPIC");
        helper.assertTrue(brkStack.getRarity() == Rarity.RARE, "broken_halo 稀有度应为 RARE");
        helper.assertTrue(ascStack.getMaxStackSize() == 1, "ascension_halo maxStack 应为 1");
        helper.assertTrue(brkStack.getMaxStackSize() == 1, "broken_halo maxStack 应为 1");
        helper.assertTrue(ascStack.has(DataComponents.FIRE_RESISTANT), "ascension_halo 应防火");
        helper.assertTrue(brkStack.has(DataComponents.FIRE_RESISTANT), "broken_halo 应防火");
        helper.assertFalse(asc.isEnchantable(ascStack), "ascension_halo 不可附魔");
        helper.assertFalse(brk.isEnchantable(brkStack), "broken_halo 不可附魔");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void curios_capability_and_head_tag(GameTestHelper helper) {
        LOGGER.info(PREFIX + "curios_capability_and_head_tag 开始");
        Optional<ICurio> curioA = CuriosApi.getCurio(new ItemStack(ModItems.ASCENSION_HALO.get()));
        Optional<ICurio> curioB = CuriosApi.getCurio(new ItemStack(ModItems.BROKEN_HALO.get()));
        helper.assertTrue(curioA.isPresent(), "ascension_halo 应暴露 ICurio 能力");
        helper.assertTrue(curioB.isPresent(), "broken_halo 应暴露 ICurio 能力");

        TagKey<Item> head = ItemTags.create(ResourceLocation.fromNamespaceAndPath("curios", "head"));
        Registry<Item> itemReg = helper.getLevel().registryAccess().registryOrThrow(Registries.ITEM);
        Optional<HolderSet.Named<Item>> tag = itemReg.getTag(head);
        helper.assertTrue(tag.isPresent(), "curios:head 标签应存在");
        boolean inA = tag.get().stream().anyMatch(h -> h.is(ModItems.ASCENSION_HALO.getId()));
        boolean inB = tag.get().stream().anyMatch(h -> h.is(ModItems.BROKEN_HALO.getId()));
        helper.assertTrue(inA, "ascension_halo 应在 curios:head 标签内");
        helper.assertTrue(inB, "broken_halo 应在 curios:head 标签内");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void config_seven_root_keys(GameTestHelper helper) {
        LOGGER.info(PREFIX + "config_seven_root_keys 开始");
        helper.assertTrue(Math.abs(ModConfig.APOLLYON_HEALTH.get() - 666.0) < EPS, "apollyon_health 默认应为 666");
        helper.assertTrue(ModConfig.APOLLYON_HEALTH.getPath().size() == 1, "apollyon_health 应为根级键");
        helper.assertTrue(ModConfig.PLAYER_INVULNERABLE.get(), "player_invincible 默认 true");
        helper.assertTrue(ModConfig.ENABLE_PLAYER_AMBIENT.get(), "player_ambient_sound 默认 true");
        helper.assertTrue(Math.abs(ModConfig.HELLFIRE_DAMAGE_AMOUNT.get() - 0.0444) < EPS, "hellfire 默认 0.0444");
        helper.assertTrue(Math.abs(ModConfig.HELLCLOUD_DAMAGE_AMOUNT.get() - 0.001) < EPS, "hellcloud 默认 0.001");
        helper.assertTrue(Math.abs(ModConfig.APOLLYON_HURT_LIMIT.get() - 20.0) < EPS, "apollyon_hurt_limit 默认 20.0");
        helper.assertTrue(ModConfig.SUMMON_APOLLYON_RANDOM_VALUE.get() == 100, "summon 默认 100");
        helper.assertTrue(ModConfig.SUMMON_APOLLYON_RANDOM_VALUE.getPath().size() == 1, "summon 应为根级键");
        helper.assertTrue(ModConfig.SPEC.isLoaded(), "NeoForge 配置应实际完成生命周期载入");
        helper.assertTrue(ModConfig.SPEC.getValues().valueMap().size() == 7, "配置应恰有七个根级键");
        helper.assertFalse(ModConfig.SPEC.getValues().valueMap().containsKey("General"), "不能添加旧产物没有的分组");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void tooltip_branches(GameTestHelper helper) {
        LOGGER.info(PREFIX + "tooltip_branches 开始");
        List<Component> shift = HaloTooltipKeys.completeLines(true, false);
        List<Component> alt = HaloTooltipKeys.completeLines(false, true);
        List<Component> def = HaloTooltipKeys.completeLines(false, false);
        List<Component> both = HaloTooltipKeys.completeLines(true, true);

        helper.assertTrue(shift.size() == 12, "Shift 提示应为 12 行，实际 " + shift.size());
        helper.assertTrue(alt.size() == 19, "Alt 提示应为 19 行，实际 " + alt.size());
        helper.assertTrue(def.size() == 2, "默认提示应为 2 行，实际 " + def.size());
        helper.assertTrue(both.size() == 12, "Shift+Alt 应优先取 Shift 的 12 行，实际 " + both.size());
        helper.assertTrue(HaloTooltipKeys.brokenLines().size() == 1, "破碎光环提示应为 1 行");
        helper.assertTrue(((TranslatableContents) def.get(0).getContents()).getKey().equals(
                "tooltip.revelationfix.holdAltEffect"), "默认提示必须是补丁后的 Alt 键");
        helper.assertTrue(((TranslatableContents) def.get(1).getContents()).getKey().equals(
                "tooltip.revelationfix.holdShift"), "默认提示必须是补丁后的 Shift 键");
        helper.assertTrue(both.equals(shift), "同时按 Shift+Alt 的内容必须等于 Shift，不只数量相同");
        helper.assertTrue(((TranslatableContents) HaloTooltipKeys.brokenLines().getFirst().getContents())
                .getKey().equals("tooltip.goety_revelation.tip13"), "破损提示键必须保留");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void dropped_halo_immune(GameTestHelper helper) {
        LOGGER.info(PREFIX + "dropped_halo_immune 开始");
        Level level = helper.getLevel();
        DamageSource generic = level.damageSources().generic();
        DamageSource explosion = level.damageSources().explosion(null, null);
        DamageSource outOfWorld = level.damageSources().fellOutOfWorld();

        ItemEntity ascEntity = helper.spawn(EntityType.ITEM, new BlockPos(1, 4, 1));
        ascEntity.setItem(new ItemStack(ModItems.ASCENSION_HALO.get()));
        helper.assertFalse(ascEntity.hurt(generic, 10.0F), "ascension_halo 掉落物应对 generic 免疫");
        helper.assertFalse(ascEntity.hurt(explosion, 10.0F), "ascension_halo 掉落物应对 explosion 免疫");
        helper.assertFalse(ascEntity.hurt(outOfWorld, 10.0F), "ascension_halo 掉落物应对 void 免疫");

        ItemEntity brkEntity = helper.spawn(EntityType.ITEM, new BlockPos(2, 4, 1));
        brkEntity.setItem(new ItemStack(ModItems.BROKEN_HALO.get()));
        helper.assertFalse(brkEntity.hurt(generic, 10.0F), "broken_halo 掉落物应对 generic 免疫");
        helper.assertFalse(brkEntity.hurt(explosion, 10.0F), "broken_halo 掉落物应对 explosion 免疫");
        helper.assertFalse(brkEntity.hurt(outOfWorld, 10.0F), "broken_halo 掉落物应对 void 免疫");
        helper.assertFalse(ascEntity.isRemoved() || brkEntity.isRemoved(), "拒绝伤害后两个光环实体均不得移除");

        ItemEntity stoneEntity = helper.spawn(EntityType.ITEM, new BlockPos(3, 4, 1));
        stoneEntity.setItem(new ItemStack(Items.STONE));
        helper.assertTrue(stoneEntity.hurt(generic, 10.0F), "普通石头掉落物仍应被伤害（Mixin 非全局）");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void dropped_halo_lifetime(GameTestHelper helper) {
        LOGGER.info(PREFIX + "dropped_halo_lifetime 开始：高龄加载与普通石头对照");
        for (int age : new int[]{5999, 6000, 32767}) {
            for (Item halo : new Item[]{ModItems.ASCENSION_HALO.get(), ModItems.BROKEN_HALO.get()}) {
                ItemEntity entity = new ItemEntity(helper.getLevel(), 0, 100, 0, new ItemStack(halo));
                CompoundTag tag = new CompoundTag();
                entity.addAdditionalSaveData(tag);
                tag.putShort("Age", (short) age);
                tag.putInt("Lifespan", 6000);
                entity.readAdditionalSaveData(tag);
                entity.tick();
                helper.assertFalse(entity.isRemoved(), "高龄光环不得在 tick TAIL 之前过期，age=" + age);
                helper.assertTrue(entity.lifespan == Integer.MAX_VALUE, "光环必须恢复库的寿命保护");
                CompoundTag saved = new CompoundTag();
                entity.addAdditionalSaveData(saved);
                ItemEntity restored = new ItemEntity(EntityType.ITEM, helper.getLevel());
                restored.readAdditionalSaveData(saved);
                restored.tick();
                helper.assertFalse(restored.isRemoved(), "保护状态保存重读后仍不可过期");
                helper.assertTrue(restored.getItem().is(halo), "保存重读后必须保留正确物品 ID");
            }
        }
        ItemEntity stone = new ItemEntity(helper.getLevel(), 0, 100, 0, new ItemStack(Items.STONE));
        CompoundTag oldStone = new CompoundTag();
        stone.addAdditionalSaveData(oldStone);
        oldStone.putShort("Age", (short) 5999);
        oldStone.putInt("Lifespan", 6000);
        stone.readAdditionalSaveData(oldStone);
        stone.tick();
        helper.assertTrue(stone.isRemoved(), "普通石头达到 6000 tick 仍须正常过期，禁止全局延长寿命");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void registration_namespace(GameTestHelper helper) {
        LOGGER.info(PREFIX + "registration_namespace 开始");
        ResourceLocation ascId = ModItems.ASCENSION_HALO.getId();
        helper.assertTrue(ascId.getNamespace().equals(GoetyNeoRevelation.LEGACY_NAMESPACE),
                "物品命名空间应为旧 " + GoetyNeoRevelation.LEGACY_NAMESPACE);
        helper.assertFalse(ascId.getNamespace().equals(GoetyNeoRevelation.MOD_ID),
                "物品命名空间应与 MOD_ID 不同");
        helper.assertTrue(ascId.equals(ResourceLocation.fromNamespaceAndPath("goety_revelation", "ascension_halo")),
                "完整 id 应为 goety_revelation:ascension_halo");
        helper.assertTrue(ModItems.BROKEN_HALO.getId().equals(
                ResourceLocation.fromNamespaceAndPath("goety_revelation", "broken_halo")),
                "完整 id 应为 goety_revelation:broken_halo");
        helper.succeed();
    }
}
