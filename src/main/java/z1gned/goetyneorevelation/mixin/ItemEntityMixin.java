package z1gned.goetyneorevelation.mixin;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import z1gned.goetyneorevelation.GoetyNeoRevelation;

/** Only the two legacy Halo item entities are protected, never all dropped items. */
@Mixin(value = ItemEntity.class, remap = false)
public abstract class ItemEntityMixin {
    @Shadow private int pickupDelay;

    @Unique
    private static final ResourceLocation goetyneo$ascensionHalo =
            ResourceLocation.fromNamespaceAndPath(GoetyNeoRevelation.LEGACY_NAMESPACE, "ascension_halo");
    @Unique
    private static final ResourceLocation goetyneo$brokenHalo =
            ResourceLocation.fromNamespaceAndPath(GoetyNeoRevelation.LEGACY_NAMESPACE, "broken_halo");

    @Unique
    private boolean goetyneo$isProtectedHalo() {
        ItemEntity entity = (ItemEntity) (Object) this;
        if (entity.getItem().isEmpty()) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(entity.getItem().getItem());
        return id.equals(goetyneo$ascensionHalo) || id.equals(goetyneo$brokenHalo);
    }

    // Keep the outer artifact's unconditional damage rejection, including bypass sources.
    @Inject(method = "hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z",
            at = @At("HEAD"), cancellable = true, require = 1, remap = false)
    private void goetyneo$protectHaloItems(DamageSource source, float amount,
                                           CallbackInfoReturnable<Boolean> callback) {
        if (goetyneo$isProtectedHalo()) {
            GoetyNeoRevelation.LOGGER.debug("[state] rejected dropped Halo damage: entity={} source={} amount={}",
                    ((ItemEntity) (Object) this).getUUID(), source.getMsgId(), amount);
            callback.setReturnValue(false);
        }
    }

    // Apply before vanilla expiry: a freshly reloaded Age=6000 item must not expire first.
    @Inject(method = "tick()V", at = @At("HEAD"), require = 1, remap = false)
    private void goetyneo$preserveHaloLifetime(CallbackInfo callback) {
        ItemEntity entity = (ItemEntity) (Object) this;
        if (!goetyneo$isProtectedHalo()) return;
        int vanillaLifespan = entity.getItem().getEntityLifespan(entity.level());
        // /give intentionally makes an unpickable age=(vanilla lifespan-1) display.
        // Match both sentinels, not pickup delay alone: real reserved drops may also be unpickable.
        // Restore normal expiry for ghosts already saved by the previous protection code.
        if (pickupDelay == Short.MAX_VALUE && entity.getAge() >= vanillaLifespan - 1) {
            if (entity.lifespan != vanillaLifespan) {
                GoetyNeoRevelation.LOGGER.debug("[state] restore command display expiry: entity={} age={} from={} to={}",
                        entity.getUUID(), entity.getAge(), entity.lifespan, vanillaLifespan);
                entity.lifespan = vanillaLifespan;
            }
            return;
        }
        if (entity.lifespan != Integer.MAX_VALUE) {
            GoetyNeoRevelation.LOGGER.debug("[state] extend dropped Halo lifetime: entity={} from={} to={}",
                    entity.getUUID(), entity.lifespan, Integer.MAX_VALUE);
            entity.lifespan = Integer.MAX_VALUE;
        }
    }
}
