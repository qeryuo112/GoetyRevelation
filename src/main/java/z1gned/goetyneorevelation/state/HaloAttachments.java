package z1gned.goetyneorevelation.state;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import z1gned.goetyneorevelation.GoetyNeoRevelation;

public final class HaloAttachments {
    private static final DeferredRegister<AttachmentType<?>> TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, GoetyNeoRevelation.MOD_ID);
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<HaloState>> PLAYER =
            TYPES.register("halo_player_state", () -> AttachmentType.serializable(HaloState::new)
                    .copyOnDeath().sync(HaloState.STREAM_CODEC).build());

    public static void register(IEventBus bus) {
        TYPES.register(bus);
        GoetyNeoRevelation.LOGGER.debug("[state] register persistent Halo attachment with native .259 S2C synchronization");
    }

    private HaloAttachments() {}
}
