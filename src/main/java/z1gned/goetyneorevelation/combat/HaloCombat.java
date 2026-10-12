package z1gned.goetyneorevelation.combat;

import com.Polarice3.Goety.common.effects.GoetyEffects;
import com.Polarice3.Goety.common.entities.boss.Apostle;
import com.Polarice3.Goety.config.MainConfig;
import com.Polarice3.Goety.init.ModMobType;
import com.Polarice3.Goety.utils.MobTypeHelper;
import com.Polarice3.Goety.common.entities.hostile.servants.ObsidianMonolith;
import com.Polarice3.Goety.common.entities.projectiles.NetherMeteor;
import com.Polarice3.Goety.common.entities.util.SummonCircle;
import com.Polarice3.Goety.init.ModSounds;
import com.Polarice3.Goety.utils.LichdomHelper;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import z1gned.goetyneorevelation.GoetyNeoRevelation;
import z1gned.goetyneorevelation.config.HaloConfig;
import z1gned.goetyneorevelation.config.ModConfig;
import z1gned.goetyneorevelation.entity.ModEntities;
import z1gned.goetyneorevelation.item.ModItems;
import z1gned.goetyneorevelation.state.HaloAttachments;
import z1gned.goetyneorevelation.state.HaloState;

/** Server-authoritative Halo behavior; old timing and even unusual branches are preserved. */
@EventBusSubscriber(modid = GoetyNeoRevelation.MOD_ID)
public final class HaloCombat {
    private HaloCombat() {}

    /** Restore Fix LivingEntity.canAttack suppression, not blanket damage immunity. */
    public static boolean suppressUndeadAttack(LivingEntity attacker, LivingEntity target) {
        if (!HaloEquipment.hasFull(target)) return false;
        boolean undead = MobTypeHelper.getMobType(attacker) == ModMobType.UNDEAD
                || attacker.getType().is(com.Polarice3.Goety.init.ModTags.EntityTypes.LICH_NEUTRAL);
        if (!undead) return false;
        boolean suppress = HaloConfig.UNDEAD_SUPPRESSION.get()
                ? !(attacker instanceof Apostle)
                : attacker.getMaxHealth() <= MainConfig.LichPowerfulFoesHealth.get();
        if (suppress && !attacker.level().isClientSide) {
            GoetyNeoRevelation.LOGGER.debug("[combat] suppress undead target: attacker={} type={} target={} maxHealth={} fullSuppression={}",
                    attacker.getUUID(), attacker.getType(), target.getUUID(), attacker.getMaxHealth(),
                    HaloConfig.UNDEAD_SUPPRESSION.get());
        }
        return suppress;
    }

    public static boolean isImmune(Player player, DamageSource source) {
        if (player.level().isClientSide) return false;
        HaloState state = player.getData(HaloAttachments.PLAYER);
        if (state.getInvulTick() > 0 && ModConfig.PLAYER_INVULNERABLE.get()
                && !source.is(DamageTypes.FELL_OUT_OF_WORLD)) return true;
        if (state.getMeteorTime() > 0 && source.is(DamageTypeTags.IS_EXPLOSION)) return true;
        return HaloEquipment.hasFull(player) && (source.is(DamageTypeTags.IS_EXPLOSION)
                || source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypes.LAVA)
                || source.is(DamageTypes.IN_WALL) || source.is(DamageTypeTags.IS_FALL)
                || source.is(DamageTypes.CRAMMING) || source.is(DamageTypeTags.IS_DROWNING));
    }

    /** Matches the old Player.hurt ModifyArg, before shields and invulnerability differences. */
    public static float modifyPlayerEntryDamage(Player player, DamageSource source, float amount) {
        if (player.level().isClientSide) return amount;
        HaloState state = player.getData(HaloAttachments.PLAYER);
        if (state.getInvulTick() > 0 && !ModConfig.PLAYER_INVULNERABLE.get()) amount *= 0.2F;
        if (HaloEquipment.hasFull(player) && player.level().dimension() == Level.NETHER) amount /= 2.0F;
        return amount;
    }

    /** Old LivingHurt multiplier and cap are BEFORE armor, resistance, enchantments and absorption. */
    public static float beforeArmor(LivingEntity entity, DamageSource source, float amount) {
        if (!(entity instanceof Player player) || player.level().isClientSide
                || source.is(DamageTypes.FELL_OUT_OF_WORLD)) return amount;
        float original = amount;
        if (HaloEquipment.hasBroken(player)) amount *= player.level().dimension() == Level.NETHER ? 0.7F : 0.85F;
        if (HaloEquipment.hasFull(player)) {
            amount *= player.level().dimension() == Level.NETHER ? 0.5F : 0.75F;
            amount = Math.min(amount, HaloConfig.DAMAGE_CAP.get().floatValue());
        }
        if (amount != original) GoetyNeoRevelation.LOGGER.debug(
                "[combat] preArmor entity={} dimension={} source={} input={} adjusted={} absorption={}",
                player.getUUID(), player.level().dimension().location(), source.getMsgId(), original, amount,
                player.getAbsorptionAmount());
        return amount;
    }

    /** Original RETURN injection also runs after a successful totem and on void damage. */
    public static boolean revive(Player player, DamageSource source) {
        if (!(player.level() instanceof ServerLevel level) || !HaloEquipment.hasFull(player)) return false;
        HaloState state = player.getData(HaloAttachments.PLAYER);
        if (state.getHaloReviveCooldown() > 0) return false;
        player.setHealth(player.getMaxHealth() / 2.0F);
        state.setHaloReviveCooldown(36000);
        state.setMeteorTime(6000);
        state.setInvulTick(400);
        player.syncData(HaloAttachments.PLAYER);
        var servant = ModEntities.WITHER_SERVANT.get().create(level);
        if (servant == null) throw new IllegalStateException("Registered WitherServant factory returned null");
        servant.setTrueOwner(player);
        servant.finalizeSpawn(level, level.getCurrentDifficultyAt(player.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
        SummonCircle circle = new SummonCircle(level, player.blockPosition().offset(0, 2, 0), servant, false, true, player);
        if (!level.addFreshEntity(circle)) GoetyNeoRevelation.LOGGER.warn("[combat] Halo revive circle spawn rejected entity={}", player.getUUID());
        GoetyNeoRevelation.LOGGER.debug("[combat] revive entity={} source={} health={} cooldown={} meteoring={} invul={}",
                player.getUUID(), source.getMsgId(), player.getHealth(), 36000, 6000, 400);
        return true;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        serverTick(player);
    }

    public static void serverTick(Player player) {
        if (!(player.level() instanceof ServerLevel level)) return;
        HaloState state = player.getData(HaloAttachments.PLAYER);
        boolean full = HaloEquipment.hasFull(player);
        boolean changed = false;
        if (state.getInvulTick() > 0) {
            state.setInvulTick(state.getInvulTick() - 1);
            changed = true;
        }
        // As in fix2, removing the complete Halo freezes revive and meteor cooldowns only.
        if (player.isAlive() && full) {
            if (state.getHaloReviveCooldown() > 0) { state.setHaloReviveCooldown(state.getHaloReviveCooldown() - 1); changed = true; }
            if (state.getMeteorTime() > 0) { state.setMeteorTime(state.getMeteorTime() - 1); changed = true; }
            if (level.dimension() == Level.NETHER && player.tickCount % 25 == 0) player.heal(0.8F);
            if (state.getMeteorTime() > 0 && !player.isSpectator()) {
                if (player.tickCount % 20 == 0) spawnMeteor(player, level);
                if (!level.isThundering()) level.setWeatherParameters(0, 6000, true, true);
            }
        }
        if (LichdomHelper.isInLichMode(player)) {
            ItemStack halo = HaloEquipment.find(player, ModItems.ASCENSION_HALO.get());
            if (!halo.isEmpty()) {
                halo.shrink(1);
                GoetyNeoRevelation.LOGGER.debug("[state] consume equipped Halo for Lich mode entity={}", player.getUUID());
            }
        }
        if (changed) player.syncData(HaloAttachments.PLAYER);
    }

    private static void spawnMeteor(Player player, ServerLevel level) {
        MutableBlockPos pos = new MutableBlockPos(player.getRandomX(0.2), player.getY(), player.getRandomZ(0.2));
        while (pos.getY() < player.getY() + 64.0 && !level.getBlockState(pos).isSolidRender(level, pos)) pos.move(Direction.UP);
        if (pos.getY() <= player.getY() + 32.0) return;
        int range = player.getHealth() < player.getMaxHealth() / 2.0F ? 450 : 900;
        // Preserve the original range. The old health < health/4 branch is unreachable at positive health.
        double x = level.random.nextInt(range) * (level.random.nextBoolean() ? 1.0 : -1.0);
        double z = level.random.nextInt(range) * (level.random.nextBoolean() ? 1.0 : -1.0);
        NetherMeteor meteor = new NetherMeteor(level, player, x, -900.0, z);
        meteor.setDangerous(false);
        meteor.setPos(pos.getX(), pos.getY(), pos.getZ());
        level.addFreshEntity(meteor);
        GoetyNeoRevelation.LOGGER.debug("[state] Halo meteor owner={} pos={} remaining={}",
                player.getUUID(), pos, player.getData(HaloAttachments.PLAYER).getMeteorTime());
    }

    public static void protectFromMonolith(ObsidianMonolith monolith) {
        if (monolith.level().isClientSide || !(monolith.getTrueOwner() instanceof Player player)) return;
        if (HaloEquipment.hasFull(player)) {
            player.getData(HaloAttachments.PLAYER).setInvulTick(10);
            player.syncData(HaloAttachments.PLAYER);
        }
        if (player.tickCount % 25 == 0) player.heal(0.8F);
    }

    public static void applyBurnHex(LivingEntity target, DamageSource source) {
        if (target.level().isClientSide || !(source.getEntity() instanceof Player attacker)
                || !HaloEquipment.hasFull(attacker) || target.isInvulnerableTo(source)) return;
        target.addEffect(new MobEffectInstance(GoetyEffects.BURN_HEX, 100));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEffect(MobEffectEvent.Applicable event) {
        if (!(event.getEntity() instanceof Player player) || !HaloEquipment.hasFull(player)) return;
        Holder<MobEffect> effect = event.getEffectInstance().getEffect();
        if (!effect.is(MobEffects.NIGHT_VISION) && !effect.is(MobEffects.BAD_OMEN) && !HaloConfig.bypassesEffect(effect)) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }
}
