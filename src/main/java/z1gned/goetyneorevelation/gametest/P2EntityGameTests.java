package z1gned.goetyneorevelation.gametest;

import com.Polarice3.Goety.api.entities.IOwned;
import com.Polarice3.Goety.init.ModMobType;
import com.Polarice3.Goety.utils.MobTypeHelper;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import z1gned.goetyneorevelation.GoetyNeoRevelation;
import z1gned.goetyneorevelation.entity.ModEntities;
import z1gned.goetyneorevelation.entity.WitherServant;

/**
 * P2 实体（WitherServant）真实 GameTest，运行于 {@code runGameTestServer}。
 *
 * <p>覆盖：注册原 ID {@code goety_revelation:wither_servant}、属性(300 血 / 4 甲)、
 * 实际生成与主人归属、synched 的 invul/target 保存重读、亡灵归类、半血时箭矢免疫、
 * 攻击生成带主人的 WitherSkull。
 *
 * <p>召唤由主代理在 P2Combat 用 {@code SummonCircle(...)} 负责；本批次只验证实体自身契约。
 */
@GameTestHolder(GoetyNeoRevelation.MOD_ID)
@PrefixGameTestTemplate(false)
public class P2EntityGameTests {

    private static final Logger LOGGER = LoggerFactory.getLogger(P2EntityGameTests.class);
    private static final String PREFIX = "[p2] ";

    @GameTest(template = "empty")
    public static void wither_servant_registry(GameTestHelper helper) {
        LOGGER.info(PREFIX + "wither_servant_registry 开始");
        ResourceLocation id = ModEntities.WITHER_SERVANT.getId();
        helper.assertTrue(id.toString().equals("goety_revelation:wither_servant"),
                "registry id 应为 goety_revelation:wither_servant, 实际 " + id);
        helper.assertTrue(BuiltInRegistries.ENTITY_TYPE.containsKey(
                        ResourceLocation.fromNamespaceAndPath(GoetyNeoRevelation.LEGACY_NAMESPACE, "wither_servant")),
                "实体应已在 ENTITY_TYPE 注册表存在");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void wither_servant_attributes(GameTestHelper helper) {
        LOGGER.info(PREFIX + "wither_servant_attributes 开始");
        WitherServant wither = helper.spawn(ModEntities.WITHER_SERVANT.get(), new BlockPos(1, 4, 1));
        helper.assertTrue(wither != null, "wither_servant 应可生成");
        helper.assertTrue(Math.abs(wither.getMaxHealth() - 300.0F) < 1e-3F,
                "最大生命应为 300, 实际 " + wither.getMaxHealth());
        double armor = wither.getAttribute(Attributes.ARMOR).getBaseValue();
        helper.assertTrue(Math.abs(armor - 4.0) < 1e-3, "护甲应为 4.0, 实际 " + armor);
        double flying = wither.getAttribute(Attributes.FLYING_SPEED).getBaseValue();
        helper.assertTrue(flying > 0.0, "应具备飞行速度属性, 实际 " + flying);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void wither_servant_owner(GameTestHelper helper) {
        LOGGER.info(PREFIX + "wither_servant_owner 开始");
        WitherServant wither = helper.spawn(ModEntities.WITHER_SERVANT.get(), new BlockPos(1, 4, 1));
        LivingEntity owner = helper.spawn(EntityType.ZOMBIE, new BlockPos(3, 4, 1));
        helper.assertTrue(wither instanceof IOwned, "WitherServant 应实现 IOwned");
        ((IOwned) wither).setTrueOwner(owner);
        LivingEntity resolved = ((IOwned) wither).getTrueOwner();
        helper.assertTrue(resolved != null, "wither_servant 应实际具有主人");
        helper.assertTrue(resolved == owner, "主人应为设定实体");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void wither_servant_synched_save(GameTestHelper helper) {
        LOGGER.info(PREFIX + "wither_servant_synched_save 开始");
        WitherServant wither = helper.spawn(ModEntities.WITHER_SERVANT.get(), new BlockPos(1, 4, 1));
        wither.setInvulnerableTicks(123);
        wither.setAlternativeTarget(0, 7);
        wither.setAlternativeTarget(1, 9);
        // 内存 round-trip
        helper.assertTrue(wither.getInvulnerableTicks() == 123, "内存 invul 应一致");
        helper.assertTrue(wither.getAlternativeTarget(0) == 7, "内存 target0 应一致");
        // NBT 保存重读
        CompoundTag tag = wither.saveWithoutId(new CompoundTag());
        WitherServant copy = ModEntities.WITHER_SERVANT.get().create(helper.getLevel());
        helper.assertTrue(copy != null, "应能依据类型重建实体");
        copy.load(tag);
        helper.assertTrue(copy.getInvulnerableTicks() == 123, "重读后 invul 应保留 123, 实际 " + copy.getInvulnerableTicks());
        // Original WitherServant only persisted Invul; numeric entity ids are transient.
        helper.assertTrue(copy.getAlternativeTarget(0) == 0 && copy.getAlternativeTarget(1) == 0,
                "旧目标entity id不应错误跨存档持久化");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void wither_servant_undead(GameTestHelper helper) {
        LOGGER.info(PREFIX + "wither_servant_undead 开始");
        WitherServant wither = helper.spawn(ModEntities.WITHER_SERVANT.get(), new BlockPos(1, 4, 1));
        helper.assertTrue(MobTypeHelper.getMobType(wither) == ModMobType.UNDEAD,
                "Goety MobTypeHelper 应判定为 UNDEAD");
        helper.assertTrue(wither.getType().is(EntityTypeTags.UNDEAD),
                "应属于 minecraft:undead 实体标签");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void wither_servant_arrow_immune_when_powered(GameTestHelper helper) {
        LOGGER.info(PREFIX + "wither_servant_arrow_immune_when_powered 开始");
        WitherServant wither = helper.spawn(ModEntities.WITHER_SERVANT.get(), new BlockPos(1, 4, 1));
        float lowHealth = wither.getMaxHealth() * 0.5F - 1.0F;
        wither.setHealth(lowHealth);
        helper.assertTrue(wither.isPowered(), "半血以下应处于 powered");
        Arrow arrow = new Arrow(EntityType.ARROW, helper.getLevel());
        arrow.setPos(wither.getX(), wither.getY(), wither.getZ());
        DamageSource ds = helper.getLevel().damageSources().arrow((AbstractArrow) arrow, null);
        boolean hurt = wither.hurt(ds, 10.0F);
        helper.assertFalse(hurt, "半血时来自箭矢的伤害应被免疫");
        helper.assertTrue(Math.abs(wither.getHealth() - lowHealth) < 1e-3F, "免疫后血量不应变化");
        // 非 powered 时应可受伤（对照）
        WitherServant wither2 = helper.spawn(ModEntities.WITHER_SERVANT.get(), new BlockPos(2, 4, 1));
        wither2.setHealth(wither2.getMaxHealth());
        helper.assertFalse(wither2.isPowered(), "满血不应 powered");
        boolean hurt2 = wither2.hurt(ds, 10.0F);
        helper.assertTrue(hurt2, "满血时非同类来源伤害应生效");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void wither_servant_projectile_owner(GameTestHelper helper) {
        LOGGER.info(PREFIX + "wither_servant_projectile_owner 开始");
        WitherServant wither = helper.spawn(ModEntities.WITHER_SERVANT.get(), new BlockPos(1, 4, 1));
        LivingEntity target = helper.spawn(EntityType.ZOMBIE, new BlockPos(4, 4, 1));
        wither.performRangedAttack(target, 0.0F);
        Level level = helper.getLevel();
        AABB box = wither.getBoundingBox().inflate(12.0);
        List<WitherSkull> skulls = level.getEntities(
                EntityTypeTest.forClass(WitherSkull.class), box, s -> s.getOwner() == wither);
        helper.assertFalse(skulls.isEmpty(), "攻击应生成带主人的 WitherSkull");
        if (!skulls.isEmpty()) {
            Entity owner = skulls.get(0).getOwner();
            helper.assertTrue(owner == wither, "WitherSkull 的 owner 应为召唤者");
        }
        helper.succeed();
    }
}
