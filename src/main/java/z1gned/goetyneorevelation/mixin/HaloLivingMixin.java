package z1gned.goetyneorevelation.mixin;

import java.util.Stack;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.nbt.CompoundTag;
import z1gned.goetyneorevelation.combat.HaloLegacyMigration;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import z1gned.goetyneorevelation.combat.HaloCombat;
import z1gned.goetyneorevelation.combat.HaloEquipment;

/** Small vanilla hooks whose original timing cannot be represented by Pre/Post events. */
@Mixin(value = LivingEntity.class, remap = false)
public abstract class HaloLivingMixin {
    @Shadow protected Stack<DamageContainer> damageContainers;

    @Inject(method = "canAttack(Lnet/minecraft/world/entity/LivingEntity;)Z",
            at = @At("HEAD"), cancellable = true, require = 1, remap = false)
    private void goetyneo$suppressUndeadAttack(LivingEntity target, CallbackInfoReturnable<Boolean> callback) {
        if (HaloCombat.suppressUndeadAttack((LivingEntity) (Object) this, target)) {
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "actuallyHurt(Lnet/minecraft/world/damagesource/DamageSource;F)V",
            at = @At("HEAD"), require = 1, remap = false)
    private void goetyneo$beforeArmor(DamageSource source, float amount, CallbackInfo callback) {
        LivingEntity entity = (LivingEntity) (Object) this;
        // Player overrides actuallyHurt and is handled separately, never apply both hooks.
        if (!entity.level().isClientSide && !(entity instanceof Player) && !damageContainers.isEmpty()) {
            DamageContainer container = damageContainers.peek();
            container.setNewDamage(HaloCombat.beforeArmor(entity, source, container.getNewDamage()));
        }
    }

    @Inject(method = "checkTotemDeathProtection(Lnet/minecraft/world/damagesource/DamageSource;)Z",
            at = @At("RETURN"), cancellable = true, require = 1, remap = false)
    private void goetyneo$revive(DamageSource source, CallbackInfoReturnable<Boolean> callback) {
        if ((Object) this instanceof Player player && HaloCombat.revive(player, source)) {
            // Do not invent a vanilla-totem priority or void exception absent from fix2.
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z",
            at = @At("HEAD"), require = 1, remap = false)
    private void goetyneo$burnHex(DamageSource source, float amount, CallbackInfoReturnable<Boolean> callback) {
        HaloCombat.applyBurnHex((LivingEntity) (Object) this, source);
    }

    @Inject(method = "readAdditionalSaveData(Lnet/minecraft/nbt/CompoundTag;)V",
            at = @At("RETURN"), require = 1, remap = false)
    private void goetyneo$readLegacyState(CompoundTag tag, CallbackInfo callback) {
        if ((Object) this instanceof Player player) HaloLegacyMigration.read(player, tag);
    }

    @Inject(method = "canStandOnFluid(Lnet/minecraft/world/level/material/FluidState;)Z",
            at = @At("HEAD"), cancellable = true, require = 1, remap = false)
    private void goetyneo$walkOnLava(FluidState fluid, CallbackInfoReturnable<Boolean> callback) {
        if ((Object) this instanceof Player player && HaloEquipment.hasFull(player)) {
            callback.setReturnValue(fluid.is(Fluids.LAVA) && !player.isCrouching());
        }
    }
}
