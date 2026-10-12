package z1gned.goetyneorevelation.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import z1gned.goetyneorevelation.GoetyNeoRevelation;
import z1gned.goetyneorevelation.HaloTooltipKeys;
import z1gned.goetyneorevelation.item.ModItems;

/**
 * 客户端光环提示处理器。
 *
 * <p>仅客户端加载（含 {@link Screen} 按键读取），不污染公共类。
 * 通过 {@link HaloTooltipKeys} 纯函数选择提示行，追加到物品提示末尾。
 *
 * <p>使用 {@link EventBusSubscriber} 的 {@code value = Dist.CLIENT} + GAME 总线自动在客户端注册，
 * 公共代码无需引用 DistExecutor / Screen，保持客户端隔离（NeoForge 21.1 已移除 DistExecutor）。
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = GoetyNeoRevelation.MOD_ID, value = Dist.CLIENT,
        bus = EventBusSubscriber.Bus.GAME)
public class ClientHaloTooltip {

    private static final ResourceLocation ASCENSION_HALO_ID =
            ResourceLocation.fromNamespaceAndPath(GoetyNeoRevelation.LEGACY_NAMESPACE, "ascension_halo");
    private static final ResourceLocation BROKEN_HALO_ID =
            ResourceLocation.fromNamespaceAndPath(GoetyNeoRevelation.LEGACY_NAMESPACE, "broken_halo");

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent evt) {
        ItemStack stack = evt.getItemStack();
        if (stack.isEmpty()) {
            return;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id.equals(ASCENSION_HALO_ID)) {
            evt.getToolTip().addAll(HaloTooltipKeys.completeLines(Screen.hasShiftDown(), Screen.hasAltDown()));
        } else if (id.equals(BROKEN_HALO_ID)) {
            evt.getToolTip().addAll(HaloTooltipKeys.brokenLines());
        }
    }
}
