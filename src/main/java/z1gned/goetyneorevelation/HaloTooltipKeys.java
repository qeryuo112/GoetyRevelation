package z1gned.goetyneorevelation;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * 光环提示纯函数：给定按键状态，返回应展示的提示行。
 *
 * <p>与外层 AscensionHalo 行为一致，并套用 RevelationFix 的最终修改：
 * <ul>
 *   <li>默认（未按 Shift/Alt）：holdAltEffect + holdShift 两条（Fix 覆盖原 tip/tip0）。</li>
 *   <li>按住 Shift：tip1 .. tip12（12 行）。</li>
 *   <li>按住 Alt：tip1.ability .. tip19.ability（19 行）。Shift 优先于 Alt。</li>
 *   <li>破碎之环：仅 tip13 一条。</li>
 * </ul>
 *
 * <p>本类为可独立测试的纯函数（仅依赖公共 API，不引用客户端 Screen），
 * 供客户端 {@code ClientHaloTooltip} 与 GameTest 共用。
 */
public final class HaloTooltipKeys {

    /** 完整光环：Shift 提示（tip1..tip12）。 */
    public static final String[] COMPLETE_SHIFT = {
            "tooltip.goety_revelation.tip1", "tooltip.goety_revelation.tip2", "tooltip.goety_revelation.tip3",
            "tooltip.goety_revelation.tip4", "tooltip.goety_revelation.tip5", "tooltip.goety_revelation.tip6",
            "tooltip.goety_revelation.tip7", "tooltip.goety_revelation.tip8", "tooltip.goety_revelation.tip9",
            "tooltip.goety_revelation.tip10", "tooltip.goety_revelation.tip11", "tooltip.goety_revelation.tip12"
    };

    /** 完整光环：Alt 提示（tip1.ability..tip19.ability）。 */
    public static final String[] COMPLETE_ALT = {
            "tooltip.goety_revelation.tip1.ability", "tooltip.goety_revelation.tip2.ability",
            "tooltip.goety_revelation.tip3.ability", "tooltip.goety_revelation.tip4.ability",
            "tooltip.goety_revelation.tip5.ability", "tooltip.goety_revelation.tip6.ability",
            "tooltip.goety_revelation.tip7.ability", "tooltip.goety_revelation.tip8.ability",
            "tooltip.goety_revelation.tip9.ability", "tooltip.goety_revelation.tip10.ability",
            "tooltip.goety_revelation.tip11.ability", "tooltip.goety_revelation.tip12.ability",
            "tooltip.goety_revelation.tip13.ability", "tooltip.goety_revelation.tip14.ability",
            "tooltip.goety_revelation.tip15.ability", "tooltip.goety_revelation.tip16.ability",
            "tooltip.goety_revelation.tip17.ability", "tooltip.goety_revelation.tip18.ability",
            "tooltip.goety_revelation.tip19.ability"
    };

    /** 完整光环：默认提示第一条（Fix 覆盖原 tooltip.goety_revelation.tip）。 */
    public static final String COMPLETE_DEFAULT_A = "tooltip.revelationfix.holdAltEffect";

    /** 完整光环：默认提示第二条（Fix 覆盖原 tooltip.goety_revelation.tip0）。 */
    public static final String COMPLETE_DEFAULT_B = "tooltip.revelationfix.holdShift";

    /** 破碎光环唯一提示。 */
    public static final String BROKEN_TIP = "tooltip.goety_revelation.tip13";

    private HaloTooltipKeys() {
    }

    /**
     * 依据按键状态选择完整光环的提示行。Shift 优先于 Alt。
     *
     * @param shift 是否按住 Shift
     * @param alt   是否按住 Alt
     * @return 提示行列表（长度 12 / 19 / 2）
     */
    public static List<Component> completeLines(boolean shift, boolean alt) {
        List<Component> lines = new ArrayList<>();
        if (shift) {
            for (String key : COMPLETE_SHIFT) {
                lines.add(Component.translatable(key));
            }
        } else if (alt) {
            for (String key : COMPLETE_ALT) {
                lines.add(Component.translatable(key));
            }
        } else {
            lines.add(Component.translatable(COMPLETE_DEFAULT_A).withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable(COMPLETE_DEFAULT_B).withStyle(ChatFormatting.GRAY));
        }
        return lines;
    }

    /**
     * 破碎光环的提示行（始终仅一条 tip13）。
     */
    public static List<Component> brokenLines() {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(BROKEN_TIP));
        return lines;
    }
}
