package z1gned.goetyneorevelation.combat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import z1gned.goetyneorevelation.item.ModItems;

/** Only equipped Curios count, exactly like the original Goety CuriosFinder. */
public final class HaloEquipment {
    private HaloEquipment() {}

    public static boolean hasFull(LivingEntity entity) {
        return !find(entity, ModItems.ASCENSION_HALO.get()).isEmpty();
    }

    public static boolean hasBroken(LivingEntity entity) {
        return !find(entity, ModItems.BROKEN_HALO.get()).isEmpty();
    }

    public static ItemStack find(LivingEntity entity, Item item) {
        if (!(entity instanceof Player)) {
            return ItemStack.EMPTY;
        }
        return CuriosApi.getCuriosInventory(entity)
                .flatMap(handler -> handler.findFirstCurio(stack -> !stack.isEmpty() && stack.is(item)))
                .map(result -> result.stack()).orElse(ItemStack.EMPTY);
    }
}
