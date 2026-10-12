package z1gned.goetyneorevelation.item;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import z1gned.goetyneorevelation.GoetyNeoRevelation;

/**
 * 两个光环物品的注册表。命名空间为旧 {@link GoetyNeoRevelation#LEGACY_NAMESPACE}，
 * 因此注册 ID 为 goety_revelation:ascension_halo 与 goety_revelation:broken_halo。
 *
 * <p>仅本批注册的内容；其他 Goety 物品不在 P1 范围。
 */
public final class ModItems {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(GoetyNeoRevelation.LEGACY_NAMESPACE);

    /** 完整晋升之环：EPIC、不可堆叠(1)、防火、不可附魔。 */
    public static final DeferredItem<AscensionHalo> ASCENSION_HALO =
            ITEMS.register("ascension_halo", AscensionHalo::new);

    /** 破碎之环：RARE、不可堆叠(1)、防火、不可附魔。 */
    public static final DeferredItem<BrokenAscensionHalo> BROKEN_HALO =
            ITEMS.register("broken_halo", BrokenAscensionHalo::new);

    private ModItems() {
    }
}
