package z1gned.goetyneorevelation.entity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import z1gned.goetyneorevelation.GoetyNeoRevelation;

/**
 * P2 实体注册表。
 *
 * <p>命名空间沿用旧 {@link GoetyNeoRevelation#LEGACY_NAMESPACE}（goety_revelation），
 * 因此注册出的实体 ResourceLocation 为 {@code goety_revelation:wither_servant}，与原始模组一致。
 *
 * <p>{@link #register(IEventBus)} 必须由主代理在 {@code GoetyNeoRevelation} 构造器中以 MOD 事件总线调用，
 * 以完成实体注册与属性创建事件绑定。本文件不修改主类，由 p1 代理负责接线。
 */
public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPE =
            DeferredRegister.create(Registries.ENTITY_TYPE, GoetyNeoRevelation.LEGACY_NAMESPACE);

    public static final DeferredHolder<EntityType<?>, EntityType<WitherServant>> WITHER_SERVANT =
            ENTITY_TYPE.register("wither_servant", () -> EntityType.Builder.of(WitherServant::new, MobCategory.MONSTER)
                    .sized(0.9F, 3.5F)
                    .clientTrackingRange(8)
                    .build("wither_servant"));

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPE.register(modEventBus);
        modEventBus.addListener(ModEntities::onEntityAttributeCreation);
    }

    private static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        // Original ModMain registered the vanilla Wither builder, not its unused custom helper.
        event.put(WITHER_SERVANT.get(), WitherBoss.createAttributes().build());
    }
}
