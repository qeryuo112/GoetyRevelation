package z1gned.goetyneorevelation.mixin;

import java.util.Stack;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.Polarice3.Goety.init.ModSounds;
import z1gned.goetyneorevelation.combat.HaloCombat;
import z1gned.goetyneorevelation.combat.HaloEquipment;

@Mixin(value = Player.class, remap = false)
public abstract class HaloPlayerMixin extends LivingEntity {
    protected HaloPlayerMixin(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
    }

    @Inject(method = "isInvulnerableTo(Lnet/minecraft/world/damagesource/DamageSource;)Z",
            at = @At("RETURN"), cancellable = true, require = 1, remap = false)
    private void goetyneo$immune(DamageSource source, CallbackInfoReturnable<Boolean> callback) {
        if (HaloCombat.isImmune((Player) (Object) this, source)) callback.setReturnValue(true);
    }

    @ModifyArg(method = "hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"),
            index = 1, require = 1, remap = false)
    private float goetyneo$entryDamage(float amount) {
        // Entry policy does not use source-specific exclusions in the original Player hook.
        return HaloCombat.modifyPlayerEntryDamage((Player) (Object) this, null, amount);
    }

    @Inject(method = "actuallyHurt(Lnet/minecraft/world/damagesource/DamageSource;F)V",
            at = @At("HEAD"), require = 1, remap = false)
    private void goetyneo$beforeArmor(DamageSource source, float amount, CallbackInfo callback) {
        Player player = (Player) (Object) this;
        if (!player.level().isClientSide && !damageContainers.isEmpty()) {
            DamageContainer container = damageContainers.peek();
            container.setNewDamage(HaloCombat.beforeArmor(player, source, container.getNewDamage()));
        }
    }

    @Inject(method = "getDeathSound()Lnet/minecraft/sounds/SoundEvent;", at = @At("RETURN"),
            cancellable = true, require = 1, remap = false)
    private void goetyneo$deathSound(CallbackInfoReturnable<SoundEvent> callback) {
        if (HaloEquipment.hasFull((Player) (Object) this)) callback.setReturnValue(ModSounds.APOSTLE_DEATH.get());
    }

    @Inject(method = "getHurtSound(Lnet/minecraft/world/damagesource/DamageSource;)Lnet/minecraft/sounds/SoundEvent;",
            at = @At("RETURN"), cancellable = true, require = 1, remap = false)
    private void goetyneo$hurtSound(DamageSource source, CallbackInfoReturnable<SoundEvent> callback) {
        if (HaloEquipment.hasFull((Player) (Object) this)) callback.setReturnValue(ModSounds.APOSTLE_HURT.get());
    }
}
