package z1gned.goetyneorevelation.gametest;

import com.Polarice3.Goety.common.entities.boss.Apostle;
import com.Polarice3.Goety.config.MainConfig;
import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import top.theillusivec4.curios.api.CuriosApi;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import z1gned.goetyneorevelation.GoetyNeoRevelation;
import z1gned.goetyneorevelation.config.HaloConfig;
import z1gned.goetyneorevelation.entity.ModEntities;
import z1gned.goetyneorevelation.entity.WitherServant;
import z1gned.goetyneorevelation.item.ModItems;

/** Regressions follow the user's actual commands and AI rather than testing damage only. */
@GameTestHolder(GoetyNeoRevelation.MOD_ID)
@PrefixGameTestTemplate(false)
public class ManualFeedbackGameTests {
    private static final String PREFIX = "[test] manual-feedback ";

    private static Player survivalPlayer(GameTestHelper helper, Item equipped) {
        Player player = new Player(helper.getLevel(), BlockPos.ZERO, 0,
                new GameProfile(UUID.randomUUID(), "halo-ai-test")) {
            @Override public boolean isCreative() { return false; }
            @Override public boolean isSpectator() { return false; }
        };
        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        player.setPos(pos.getX(), pos.getY(), pos.getZ());
        player.setHealth(player.getMaxHealth());
        if (equipped != null) {
            var inventory = CuriosApi.getCuriosInventory(player).orElseThrow();
            var head = inventory.getStacksHandler("head").orElseThrow();
            head.getStacks().setStackInSlot(0, new ItemStack(equipped));
        }
        return player;
    }

    private static ServerPlayer commandPlayer(GameTestHelper helper) {
        var profile = new GameProfile(UUID.randomUUID(), "halo-command-test");
        var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(profile, false);
        var player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), profile, cookie.clientInformation());
        // Command/inventory/drop testing needs no fabricated Curios network handshake.
        // The isolated transport records no packets; this is not an end-to-end network test.
        player.connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(
                helper.getLevel().getServer(), new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),
                player, cookie) {
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet) {}
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet,
                                       net.minecraft.network.PacketSendListener listener) {}
        };
        return player;
    }

    private static Set<UUID> itemIds(ServerLevel level, Player player) {
        return level.getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(4))
                .stream().map(ItemEntity::getUUID).collect(Collectors.toSet());
    }

    @GameTest(template = "empty")
    public static void give_command_display_expires(GameTestHelper helper) {
        GoetyNeoRevelation.LOGGER.info(PREFIX + "give_command_display_expires");
        ServerPlayer player = commandPlayer(helper);
        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        player.setPos(pos.getX(), pos.getY(), pos.getZ());
        try {
            for (Item halo : new Item[]{ModItems.ASCENSION_HALO.get(), ModItems.BROKEN_HALO.get()}) {
                player.getInventory().clearContent();
                Set<UUID> before = itemIds(helper.getLevel(), player);
                String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(halo).toString();
                helper.getLevel().getServer().getCommands().performPrefixedCommand(
                        player.createCommandSourceStack().withPermission(4), "give @s " + id + " 1");
                helper.assertTrue(player.getInventory().countItem(halo) == 1,
                        "Actual /give must add exactly one Halo to the inventory");
                List<ItemEntity> displays = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                        player.getBoundingBox().inflate(4), entity -> !before.contains(entity.getUUID()) && entity.getItem().is(halo));
                helper.assertTrue(displays.size() == 1, "Actual /give should create its one-tick vanilla display item");
                ItemEntity display = displays.getFirst();
                CompoundTag tag = new CompoundTag();
                display.addAdditionalSaveData(tag);
                helper.assertTrue(tag.getShort("PickupDelay") == Short.MAX_VALUE,
                        "Vanilla /give display must be deliberately unpickable, not an inventory duplicate");
                display.tick();
                helper.assertTrue(display.isRemoved(), "Halo lifetime protection must not immortalize /give display items");
                helper.assertTrue(player.getInventory().countItem(halo) == 1, "Display expiry must not remove the granted item");
            }
            helper.succeed();
        } finally {
            player.discard();
        }
    }

    @GameTest(template = "empty")
    public static void give_overflow_is_real_pickable_drop(GameTestHelper helper) {
        GoetyNeoRevelation.LOGGER.info(PREFIX + "give_overflow_is_real_pickable_drop");
        ServerPlayer player = commandPlayer(helper);
        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        player.setPos(pos.getX(), pos.getY(), pos.getZ());
        try {
            for (int slot = 0; slot < player.getInventory().items.size(); slot++) {
                player.getInventory().items.set(slot, new ItemStack(Items.STONE, 64));
            }
            Set<UUID> before = itemIds(helper.getLevel(), player);
            helper.getLevel().getServer().getCommands().performPrefixedCommand(
                    player.createCommandSourceStack().withPermission(4), "give @s goety_revelation:ascension_halo 1");
            List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                    player.getBoundingBox().inflate(4), entity -> !before.contains(entity.getUUID())
                            && entity.getItem().is(ModItems.ASCENSION_HALO.get()));
            helper.assertTrue(drops.size() == 1 && player.getInventory().countItem(ModItems.ASCENSION_HALO.get()) == 0,
                    "A full inventory must create a real drop, not a display item");
            ItemEntity drop = drops.getFirst();
            drop.tick();
            helper.assertFalse(drop.isRemoved(), "A real overflow Halo must keep lifetime protection");
            helper.assertTrue(drop.lifespan == Integer.MAX_VALUE, "Only real dropped Halos keep unlimited lifespan");
            player.getInventory().items.set(0, ItemStack.EMPTY);
            drop.playerTouch(player);
            helper.assertTrue(drop.isRemoved() && player.getInventory().countItem(ModItems.ASCENSION_HALO.get()) == 1,
                    "A real overflow drop must remain pickable once a slot is freed");
            helper.succeed();
        } finally {
            player.discard();
        }
    }

    @GameTest(template = "empty")
    public static void saved_give_ghost_expires_without_deleting_real_halos(GameTestHelper helper) {
        GoetyNeoRevelation.LOGGER.info(PREFIX + "saved_give_ghost_expires_without_deleting_real_halos");
        for (Item halo : new Item[]{ModItems.ASCENSION_HALO.get(), ModItems.BROKEN_HALO.get()}) {
            ItemEntity ghost = new ItemEntity(helper.getLevel(), 0, 100, 0, new ItemStack(halo));
            ghost.makeFakeItem();
            CompoundTag saved = new CompoundTag();
            ghost.addAdditionalSaveData(saved);
            saved.putInt("Lifespan", Integer.MAX_VALUE);
            saved.putShort("Age", (short) 6100);
            ItemEntity restored = new ItemEntity(EntityType.ITEM, helper.getLevel());
            restored.readAdditionalSaveData(saved);
            restored.tick();
            helper.assertTrue(restored.isRemoved(), "Previously saved /give ghosts must recover vanilla expiry");

            ItemEntity real = new ItemEntity(helper.getLevel(), 0, 100, 0, new ItemStack(halo));
            CompoundTag realTag = new CompoundTag();
            real.addAdditionalSaveData(realTag);
            realTag.putShort("Age", (short) 6100);
            realTag.putInt("Lifespan", 6000);
            real.readAdditionalSaveData(realTag);
            real.tick();
            helper.assertFalse(real.isRemoved(), "A real old Halo must not be confused with an unpickable command ghost");

            ItemEntity reserved = new ItemEntity(helper.getLevel(), 0, 100, 0, new ItemStack(halo));
            reserved.setNeverPickUp();
            reserved.tick();
            helper.assertFalse(reserved.isRemoved(), "A fresh intentionally unpickable Halo is not a /give display");
            helper.assertTrue(reserved.lifespan == Integer.MAX_VALUE, "Pickup delay alone must not identify /give ghosts");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void equipped_halo_suppresses_undead_attack_selection(GameTestHelper helper) {
        GoetyNeoRevelation.LOGGER.info(PREFIX + "equipped_halo_suppresses_undead_attack_selection");
        Player full = survivalPlayer(helper, ModItems.ASCENSION_HALO.get());
        Player plain = survivalPlayer(helper, null);
        Player broken = survivalPlayer(helper, ModItems.BROKEN_HALO.get());
        Player backpack = survivalPlayer(helper, null);
        backpack.getInventory().add(new ItemStack(ModItems.ASCENSION_HALO.get()));
        WitherServant servant = ModEntities.WITHER_SERVANT.get().create(helper.getLevel());
        Mob zombie = EntityType.ZOMBIE.create(helper.getLevel());
        Mob spider = EntityType.SPIDER.create(helper.getLevel());
        helper.assertTrue(servant != null && zombie != null && spider != null, "Need real registered undead and non-undead controls");
        servant.setPos(full.position());
        helper.assertFalse(servant.canAttack(full), "Original fix2: WitherServant must not target a complete Halo wearer");
        helper.assertFalse(zombie.canAttack(full), "Ordinary undead must also respect complete Halo suppression");
        helper.assertFalse(TargetingConditions.forCombat().ignoreLineOfSight().test(servant, full),
                "Side-head AI targeting conditions must consult the real canAttack hook");
        helper.assertTrue(servant.canAttack(plain), "Do not disable all servant attacks");
        helper.assertTrue(servant.canAttack(broken), "Broken Halo never gained complete Halo undead suppression");
        helper.assertTrue(servant.canAttack(backpack), "Backpack Halo must not grant the equipped ability");
        helper.assertTrue(spider.canAttack(full), "Non-undead creatures remain hostile");
        CuriosApi.getCuriosInventory(full).orElseThrow().getStacksHandler("head").orElseThrow()
                .getStacks().setStackInSlot(0, ItemStack.EMPTY);
        helper.assertTrue(servant.canAttack(full), "Removing the Halo must restore targeting in the same tick");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void wither_ai_drops_preexisting_halo_target(GameTestHelper helper) {
        GoetyNeoRevelation.LOGGER.info(PREFIX + "wither_ai_drops_preexisting_halo_target");
        Player full = survivalPlayer(helper, null);
        // Other GameTests run nearby and intentionally spawn unprotected combat targets.
        // Isolate this AI test vertically so shooting those controls is not a false failure.
        full.setPos(full.getX(), full.getY() + 80, full.getZ());
        full.setNoGravity(true);
        helper.getLevel().addFreshEntity(full);
        WitherServant servant = ModEntities.WITHER_SERVANT.get().create(helper.getLevel());
        helper.assertTrue(servant != null, "Need real original WitherServant");
        servant.setPos(full.getX() + 3, full.getY() + 2, full.getZ());
        servant.setNoGravity(true);
        helper.getLevel().addFreshEntity(servant);
        try {
            // Start the real registered target goal before equipping, rather than planting
            // an impossible target with no running goal to clear it.
            var targetGoal = servant.targetSelector.getAvailableGoals().stream()
                    .filter(goal -> goal.getGoal() instanceof net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal)
                    .findFirst().orElseThrow();
            ((net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<?>) targetGoal.getGoal()).setTarget(full);
            targetGoal.start();
            helper.assertTrue(targetGoal.isRunning() && servant.getTarget() == full,
                    "Precondition: the real target goal must own the target before equipping");
            CuriosApi.getCuriosInventory(full).orElseThrow().getStacksHandler("head").orElseThrow()
                    .getStacks().setStackInSlot(0, new ItemStack(ModItems.ASCENSION_HALO.get()));
            servant.setAlternativeTarget(1, full.getId());
            servant.setAlternativeTarget(2, full.getId());
            Set<UUID> before = helper.getLevel().getEntitiesOfClass(WitherSkull.class,
                    servant.getBoundingBox().inflate(20)).stream().map(WitherSkull::getUUID).collect(Collectors.toSet());
            for (int tick = 0; tick < 45; tick++) {
                servant.tickCount++;
                servant.tick();
            }
            helper.assertTrue(servant.getTarget() != full, "Real goal ticking must drop a stale wearer target");
            helper.assertTrue(servant.getAlternativeTarget(1) != full.getId()
                    && servant.getAlternativeTarget(2) != full.getId(), "Both side heads must abandon the wearer");
            List<WitherSkull> fired = helper.getLevel().getEntitiesOfClass(WitherSkull.class,
                    servant.getBoundingBox().inflate(20), skull -> !before.contains(skull.getUUID())
                            && skull.getOwner() == servant);
            helper.assertTrue(fired.isEmpty(), "AI must not keep shooting the protected wearer after equipping");
            helper.succeed();
        } finally {
            servant.discard();
            full.discard();
        }
    }

    @GameTest(template = "empty")
    public static void undead_config_health_threshold_and_apostle_exception(GameTestHelper helper) {
        GoetyNeoRevelation.LOGGER.info(PREFIX + "undead_config_health_threshold_and_apostle_exception");
        Player full = survivalPlayer(helper, ModItems.ASCENSION_HALO.get());
        WitherServant servant = ModEntities.WITHER_SERVANT.get().create(helper.getLevel());
        Mob zombie = EntityType.ZOMBIE.create(helper.getLevel());
        Apostle apostle = com.Polarice3.Goety.common.entities.ModEntityType.APOSTLE.get().create(helper.getLevel());
        helper.assertTrue(servant != null && zombie != null && apostle != null, "Need original source behavior controls");
        var suppression = HaloConfig.UNDEAD_SUPPRESSION;
        boolean original = suppression.get();
        try {
            suppression.set(true);
            helper.assertFalse(servant.canAttack(full), "Default suppression must include strong undead");
            helper.assertTrue(apostle.canAttack(full), "Original full-suppression branch excludes Apostle");
            suppression.set(false);
            // worldRestart config intentionally keeps its cache until world reload.
            // Explicitly reload the test cache; do not weaken the actual config contract.
            suppression.clearCache();
            helper.assertTrue(servant.getMaxHealth() > MainConfig.LichPowerfulFoesHealth.get(), "Strong undead test precondition");
            helper.assertTrue(servant.canAttack(full), "False config restores hostility for undead above the Goety health threshold");
            helper.assertFalse(zombie.canAttack(full), "False config still suppresses weak undead, it is not a total off switch");
        } finally {
            suppression.set(original);
            suppression.clearCache();
        }
        helper.succeed();
    }
}
