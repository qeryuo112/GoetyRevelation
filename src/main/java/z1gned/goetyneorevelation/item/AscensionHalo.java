package z1gned.goetyneorevelation.item;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/**
 * 完整晋升之环（Ascension Halo）。
 *
 * <p>属性：EPIC 稀有度、maxStack=1、fireResistant（由 RevelationFix 修改原行为补足）、不可附魔。
 * 物品实现 {@link ICurioItem} 以被 Curios 头饰槽识别。
 *
 * <p>公共类不读取客户端按键状态（Screen）。Shift/Alt 提示分支由客户端
 * {@code ClientHaloTooltip} 通过 {@link HaloTooltipKeys} 纯函数选择后追加，保证客户端隔离。
 */
public class AscensionHalo extends Item implements ICurioItem {

    public AscensionHalo() {
        super(new Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipLines,
                               TooltipFlag flag) {
        // 公共类不触达客户端按键；Shift/Alt 分支由客户端事件处理器追加。
        super.appendHoverText(stack, context, tooltipLines, flag);
    }
}
