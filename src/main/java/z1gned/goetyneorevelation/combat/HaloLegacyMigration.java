package z1gned.goetyneorevelation.combat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import z1gned.goetyneorevelation.GoetyNeoRevelation;
import z1gned.goetyneorevelation.state.HaloAttachments;

/** Named capability migration only. This does not promise arbitrary 1.20.1 world compatibility. */
public final class HaloLegacyMigration {
    public static void read(Player player, CompoundTag tag) {
        if (player.level().isClientSide || player.hasData(HaloAttachments.PLAYER)) return;
        CompoundTag capabilities = tag.getCompound("ForgeCaps");
        if (!capabilities.contains("goety_revelation:gr_player_cap", 10)) return;
        var state = player.getData(HaloAttachments.PLAYER);
        state.deserializeNBT(player.registryAccess(), capabilities.getCompound("goety_revelation:gr_player_cap"));
        player.syncData(HaloAttachments.PLAYER);
        GoetyNeoRevelation.LOGGER.debug("[state] import legacy GRPlayer capability entity={} invul={} revive={} meteoring={}",
                player.getUUID(), state.getInvulTick(), state.getHaloReviveCooldown(), state.getMeteorTime());
    }
    private HaloLegacyMigration() {}
}
