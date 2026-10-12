package z1gned.goetyneorevelation.gametest;

import com.mojang.authlib.GameProfile;
import com.Polarice3.Goety.common.entities.projectiles.NetherMeteor;
import com.Polarice3.Goety.common.entities.util.SummonCircle;
import com.Polarice3.Goety.utils.LichdomHelper;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;
import z1gned.goetyneorevelation.GoetyNeoRevelation;
import z1gned.goetyneorevelation.combat.HaloCombat;
import z1gned.goetyneorevelation.combat.HaloEquipment;
import z1gned.goetyneorevelation.config.ModConfig;
import z1gned.goetyneorevelation.item.ModItems;
import z1gned.goetyneorevelation.state.HaloAttachments;

/** Tests call real Player.hurt with real equipped Curios, not an isolated multiplier only. */
@GameTestHolder(GoetyNeoRevelation.MOD_ID)
@PrefixGameTestTemplate(false)
public class P2CombatGameTests {
    private static Player player(GameTestHelper helper, ServerLevel level) {
        Player player = new Player(level, BlockPos.ZERO, 0, new GameProfile(UUID.randomUUID(), "halo-test")) {
            @Override public boolean isSpectator() { return false; }
            @Override public boolean isCreative() { return false; }
        };
        player.setPos(0, 100, 0);
        player.setHealth(player.getMaxHealth());
        return player;
    }

    private static void equip(GameTestHelper helper, Player player, Item item, int index) {
        var inventory = CuriosApi.getCuriosInventory(player);
        helper.assertTrue(inventory.isPresent(), "真实玩家必须有 Curios inventory");
        var handler = inventory.get().getStacksHandler("head");
        helper.assertTrue(handler.isPresent(), "真实玩家必须有 head 槽");
        if (handler.get().getStacks().getSlots() <= index) handler.get().grow(index + 1 - handler.get().getStacks().getSlots());
        handler.get().getStacks().setStackInSlot(index, new ItemStack(item));
        helper.assertTrue(!HaloEquipment.find(player, item).isEmpty(), "必须由装备查询识别光环");
    }

    private static Player plainAttacker(GameTestHelper helper) {
        return player(helper, helper.getLevel());
    }

    private static void assertClose(GameTestHelper helper, float actual, float expected, String name) {
        helper.assertTrue(Math.abs(actual - expected) < 0.0001F, name + " expected=" + expected + " actual=" + actual);
    }

    @GameTest(template = "empty")
    public static void real_overworld_and_nether_damage(GameTestHelper helper) {
        GoetyNeoRevelation.LOGGER.info("[test] P2 real_overworld_and_nether_damage");
        Player plain = player(helper, helper.getLevel());
        plain.hurt(plain.damageSources().generic(), 8);
        assertClose(helper, plain.getHealth(), 12, "普通玩家控制组");
        Player broken = player(helper, helper.getLevel());
        equip(helper, broken, ModItems.BROKEN_HALO.get(), 0);
        broken.hurt(broken.damageSources().generic(), 8);
        assertClose(helper, broken.getHealth(), 13.2F, "破损主世界0.85");
        Player full = player(helper, helper.getLevel());
        equip(helper, full, ModItems.ASCENSION_HALO.get(), 0);
        full.hurt(full.damageSources().generic(), 8);
        assertClose(helper, full.getHealth(), 14, "完整主世界0.75");
        ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        helper.assertTrue(nether != null, "需要实际下界");
        Player netherFull = player(helper, nether);
        equip(helper, netherFull, ModItems.ASCENSION_HALO.get(), 0);
        netherFull.hurt(netherFull.damageSources().generic(), 8);
        assertClose(helper, netherFull.getHealth(), 18, "完整下界入口0.5乘护甲前0.5");
        Player netherBroken = player(helper, nether);
        equip(helper, netherBroken, ModItems.BROKEN_HALO.get(), 0);
        netherBroken.hurt(netherBroken.damageSources().generic(), 8);
        assertClose(helper, netherBroken.getHealth(), 14.4F, "破损下界0.7");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void both_halos_and_armor_cap_absorption(GameTestHelper helper) {
        Player both = player(helper, helper.getLevel());
        equip(helper, both, ModItems.BROKEN_HALO.get(), 0);
        equip(helper, both, ModItems.ASCENSION_HALO.get(), 1);
        both.hurt(both.damageSources().generic(), 8);
        assertClose(helper, both.getHealth(), 14.9F, "两个if独立倍率0.85*0.75");
        Player full = player(helper, helper.getLevel());
        equip(helper, full, ModItems.ASCENSION_HALO.get(), 0);
        full.getAttribute(Attributes.ARMOR).setBaseValue(20);
        full.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(0);
        full.getAttribute(Attributes.MAX_ABSORPTION).setBaseValue(4);
        full.setAbsorptionAmount(4);
        helper.assertTrue(full.getArmorValue() == 20 && full.getAbsorptionAmount() == 4,
                "测试前置：真实armor20、吸收4，不能靠默认吸收上限0");
        var attacker = net.minecraft.world.entity.EntityType.ZOMBIE.create(helper.getLevel());
        helper.assertTrue(attacker != null, "需要真实无PvP限制的生物伤害来源");
        full.hurt(full.damageSources().mobAttack(attacker), 100);
        // cap20 first => armor leaves12, absorption4 => health loses8.
        assertClose(helper, full.getHealth(), 12, "上限必须在护甲和吸收之前，而不是最终扣血上限");
        assertClose(helper, full.getAbsorptionAmount(), 0, "吸收量正常消费");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void revival_and_vanilla_totem_legacy_order(GameTestHelper helper) {
        Player full = player(helper, helper.getLevel());
        equip(helper, full, ModItems.ASCENSION_HALO.get(), 0);
        full.setHealth(1);
        full.hurt(full.damageSources().generic(), 4);
        assertClose(helper, full.getHealth(), 10, "Halo复活半血");
        var state = full.getData(HaloAttachments.PLAYER);
        helper.assertTrue(state.getHaloReviveCooldown() == 36000 && state.getMeteorTime() == 6000 && state.getInvulTick() == 400,
                "三个状态必须与旧补丁一致");
        var circles = helper.getLevel().getEntitiesOfClass(SummonCircle.class, full.getBoundingBox().inflate(4));
        helper.assertTrue(!circles.isEmpty(), "Halo复活应生成自己的凋灵仆从召唤圈");
        Player totem = player(helper, helper.getLevel());
        equip(helper, totem, ModItems.ASCENSION_HALO.get(), 0);
        totem.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));
        totem.setHealth(1);
        totem.hurt(totem.damageSources().generic(), 4);
        helper.assertTrue(totem.getOffhandItem().isEmpty(), "原版图腾先正常消耗");
        assertClose(helper, totem.getHealth(), 10, "旧RETURN钩子图腾成功后仍进行Halo复活");
        helper.assertTrue(totem.getData(HaloAttachments.PLAYER).getHaloReviveCooldown() == 36000, "不得擅自图腾优先跳过Halo");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void void_exception_and_revive_cooldown(GameTestHelper helper) {
        Player voidPlayer = player(helper, helper.getLevel());
        equip(helper, voidPlayer, ModItems.ASCENSION_HALO.get(), 0);
        voidPlayer.hurt(voidPlayer.damageSources().fellOutOfWorld(), 2);
        assertClose(helper, voidPlayer.getHealth(), 18, "void护甲前无减伤");
        voidPlayer.invulnerableTime = 0;
        voidPlayer.setHealth(1);
        voidPlayer.hurt(voidPlayer.damageSources().fellOutOfWorld(), 4);
        assertClose(helper, voidPlayer.getHealth(), 10, "旧Halo复活没有void排除");
        voidPlayer.invulnerableTime = 0;
        voidPlayer.hurt(voidPlayer.damageSources().fellOutOfWorld(), 40);
        helper.assertTrue(voidPlayer.isDeadOrDying(), "冷却中void不得再次复活");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void timer_freeze_and_temporary_invulnerability(GameTestHelper helper) {
        Player full = player(helper, helper.getLevel());
        equip(helper, full, ModItems.ASCENSION_HALO.get(), 0);
        var state = full.getData(HaloAttachments.PLAYER);
        state.setInvulTick(400); state.setHaloReviveCooldown(36000); state.setMeteorTime(6000);
        full.tickCount = 1;
        HaloCombat.serverTick(full);
        helper.assertTrue(state.getInvulTick() == 399 && state.getHaloReviveCooldown() == 35999 && state.getMeteorTime() == 5999,
                "服务端一次tick，不能双phase减两次");
        CuriosApi.getCuriosInventory(full).orElseThrow().getStacksHandler("head").orElseThrow().getStacks().setStackInSlot(0, ItemStack.EMPTY);
        HaloCombat.serverTick(full);
        helper.assertTrue(state.getInvulTick() == 398 && state.getHaloReviveCooldown() == 35999 && state.getMeteorTime() == 5999,
                "脱装冻结复活/陨石，但invul照常推进");
        helper.assertFalse(full.hurt(full.damageSources().generic(), 8), "默认临时invul拒伤，不要求Halo仍佩戴");
        ModConfig.PLAYER_INVULNERABLE.set(false);
        try {
            full.hurt(full.damageSources().generic(), 8);
            assertClose(helper, full.getHealth(), 18.4F, "配置false临时保护改为0.2倍率");
        } finally { ModConfig.PLAYER_INVULNERABLE.set(true); }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void immunity_effects_and_equipment_only(GameTestHelper helper) {
        Player backpack = player(helper, helper.getLevel());
        backpack.getInventory().add(new ItemStack(ModItems.ASCENSION_HALO.get()));
        helper.assertFalse(HaloEquipment.hasFull(backpack), "仅背包不提供饰品效果");
        Player full = player(helper, helper.getLevel());
        equip(helper, full, ModItems.ASCENSION_HALO.get(), 0);
        helper.assertFalse(full.hurt(full.damageSources().inFire(), 8), "完整Halo免疫火焰");
        helper.assertFalse(full.hurt(full.damageSources().fall(), 8), "完整Halo免疫摔落");
        helper.assertFalse(full.hurt(full.damageSources().drown(), 8), "完整Halo免疫溺水");
        helper.assertFalse(full.addEffect(new MobEffectInstance(MobEffects.POISON, 100)), "默认拒绝毒素");
        helper.assertTrue(full.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 100)), "夜视原例外保留");
        helper.assertTrue(full.addEffect(new MobEffectInstance(MobEffects.BAD_OMEN, 100)), "不祥之兆原例外保留");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void revive_circle_releases_original_servant(GameTestHelper helper) {
        Player full = player(helper, helper.getLevel());
        equip(helper, full, ModItems.ASCENSION_HALO.get(), 0);
        full.setPos(helper.absolutePos(new BlockPos(1, 2, 1)).getX(), helper.absolutePos(new BlockPos(1, 2, 1)).getY(),
                helper.absolutePos(new BlockPos(1, 2, 1)).getZ());
        helper.getLevel().addFreshEntity(full);
        full.setHealth(1);
        full.hurt(full.damageSources().generic(), 4);
        var circles = helper.getLevel().getEntitiesOfClass(SummonCircle.class, full.getBoundingBox().inflate(4));
        helper.assertTrue(!circles.isEmpty(), "复活必须产生召唤圈");
        SummonCircle circle = circles.getFirst();
        circle.setNoGravity(true);
        // ServerLevel normally increments tickCount before calling Entity.tick; emulate that scheduler step.
        for (int tick = 0; tick < circle.getLifeSpan(); tick++) {
            circle.tickCount++;
            circle.tick();
        }
        var servants = helper.getLevel().getEntitiesOfClass(z1gned.goetyneorevelation.entity.WitherServant.class,
                circle.getBoundingBox().inflate(4));
        GoetyNeoRevelation.LOGGER.info("[test] circle release pos={} removed={} tick={} lifespan={} entities={} owner={} servants={}",
                circle.position(), circle.isRemoved(), circle.tickCount, circle.getLifeSpan(), servants.size(), full.getUUID(),
                servants.stream().map(s -> String.valueOf(s.getOwnerId()) + "/" + s.position()).toList());
        helper.assertTrue(servants.stream().anyMatch(s -> full.getUUID().equals(s.getOwnerId())),
                "圈到期必须生成原自定义凋灵且归属复活者，不是普通凋灵替代");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void owned_monolith_refreshes_protection(GameTestHelper helper) {
        Player full = player(helper, helper.getLevel());
        equip(helper, full, ModItems.ASCENSION_HALO.get(), 0);
        full.setPos(helper.absolutePos(new BlockPos(1, 2, 1)).getX(), helper.absolutePos(new BlockPos(1, 2, 1)).getY(),
                helper.absolutePos(new BlockPos(1, 2, 1)).getZ());
        helper.getLevel().addFreshEntity(full);
        full.tickCount = 25;
        full.setHealth(10);
        var monolith = com.Polarice3.Goety.common.entities.ModEntityType.OBSIDIAN_MONOLITH.get().create(helper.getLevel());
        helper.assertTrue(monolith != null, "需要实际Goety巨柱");
        monolith.setTrueOwner(full);
        monolith.setNoAi(true);
        monolith.setPos(full.position());
        monolith.setAge(1000);
        monolith.aiStep();
        helper.assertTrue(full.getData(HaloAttachments.PLAYER).getInvulTick() == 10,
                "真实aiStep注入必须刷新巨柱保护10tick");
        assertClose(helper, full.getHealth(), 11.6F, "巨柱两个getTrueOwner调用点各回血0.8，保留旧非ordinal注入语义");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void lich_consumption_and_meteor_authority(GameTestHelper helper) {
        Player lich = player(helper, helper.getLevel());
        equip(helper, lich, ModItems.ASCENSION_HALO.get(), 0);
        LichdomHelper.setLichMode(lich, true);
        lich.tickCount = 1;
        HaloCombat.serverTick(lich);
        helper.assertFalse(HaloEquipment.hasFull(lich), "Lich mode应消费已装备完整Halo");
        Player meteorPlayer = player(helper, helper.getLevel());
        equip(helper, meteorPlayer, ModItems.ASCENSION_HALO.get(), 0);
        meteorPlayer.getData(HaloAttachments.PLAYER).setMeteorTime(100);
        meteorPlayer.tickCount = 20;
        int count = helper.getLevel().getEntitiesOfClass(NetherMeteor.class, meteorPlayer.getBoundingBox().inflate(100)).size();
        HaloCombat.serverTick(meteorPlayer);
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(NetherMeteor.class, meteorPlayer.getBoundingBox().inflate(100)).size() == count + 1,
                "无顶遮挡时服务端每20tick生成一颗归属玩家的陨石");
        helper.succeed();
    }
}
