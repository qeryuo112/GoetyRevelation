package z1gned.goetyneorevelation.mixin;

import com.Polarice3.Goety.common.entities.hostile.servants.ObsidianMonolith;
import com.Polarice3.Goety.common.entities.neutral.AbstractObsidianMonolith;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import z1gned.goetyneorevelation.combat.HaloCombat;

/** The player-owned monolith part only; Apostle/Odamane replacements are later modules. */
@Mixin(value = AbstractObsidianMonolith.class, remap = false)
public abstract class HaloMonolithMixin {
    @Inject(method = "aiStep()V", at = @At(value = "INVOKE",
            target = "Lcom/Polarice3/Goety/common/entities/neutral/AbstractObsidianMonolith;getTrueOwner()Lnet/minecraft/world/entity/LivingEntity;"),
            require = 1, remap = false)
    private void goetyneo$protectOwner(CallbackInfo callback) {
        if ((Object) this instanceof ObsidianMonolith monolith) HaloCombat.protectFromMonolith(monolith);
    }
}
