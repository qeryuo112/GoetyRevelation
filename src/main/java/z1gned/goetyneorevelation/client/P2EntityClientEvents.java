package z1gned.goetyneorevelation.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import z1gned.goetyneorevelation.GoetyNeoRevelation;
import z1gned.goetyneorevelation.client.model.WitherServantModel;
import z1gned.goetyneorevelation.client.render.WitherServantRenderer;
import z1gned.goetyneorevelation.entity.ModEntities;

/**
 * P2 客户端渲染注册（MOD 事件总线，仅客户端）。自动订阅，无需修改主类。
 *
 * <p>注册 {@link WitherServantModel} 的模型层与 {@link WitherServantRenderer}，
 * 避免进入世界时因缺乏渲染器而崩溃。
 */
@EventBusSubscriber(modid = GoetyNeoRevelation.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class P2EntityClientEvents {

    @SubscribeEvent
    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(WitherServantRenderer.LAYER, WitherServantModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.WITHER_SERVANT.get(), WitherServantRenderer::new);
    }
}
