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
 * 破碎晋升之环（Broken Ascension Halo）。
 *
 * <p>属性：RARE 稀有度、maxStack=1、fireResistant、不可附魔。实现 {@link ICurioItem}。
 * 提示仅一条（tooltip.goety_revelation.tip13），不参与 Shift/Alt 分支。
 */
public class BrokenAscensionHalo extends Item implements ICurioItem {

    public BrokenAscensionHalo() {
        super(new Properties().stacksTo(1).rarity(Rarity.RARE).fireResistant());
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipLines,
                               TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltipLines, flag);
    }
}
