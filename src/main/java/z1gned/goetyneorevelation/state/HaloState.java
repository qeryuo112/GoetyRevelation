package z1gned.goetyneorevelation.state;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.common.util.INBTSerializable;

/** Server state with the exact GRPlayerCapability persistence keys, not global shared timers. */
public final class HaloState implements INBTSerializable<CompoundTag> {
    private int invulTick;
    private int haloReviveCooldown;
    private int meteorTime;
    private int odamaneFlags;
    private boolean armorClimbing = true;
    private int extraMagicShield;
    private boolean preparingEndRitual;
    private int spectreShieldCooldown;
    private CompoundTag unknownData = new CompoundTag();

    public static final StreamCodec<RegistryFriendlyByteBuf, HaloState> STREAM_CODEC = new StreamCodec<>() {
        @Override public HaloState decode(RegistryFriendlyByteBuf buffer) {
            HaloState state = new HaloState();
            state.invulTick = buffer.readInt();
            state.haloReviveCooldown = buffer.readInt();
            state.meteorTime = buffer.readInt();
            state.odamaneFlags = buffer.readInt();
            state.armorClimbing = buffer.readBoolean();
            state.extraMagicShield = buffer.readInt();
            state.preparingEndRitual = buffer.readBoolean();
            state.spectreShieldCooldown = buffer.readInt();
            return state;
        }
        @Override public void encode(RegistryFriendlyByteBuf buffer, HaloState state) {
            buffer.writeInt(state.invulTick);
            buffer.writeInt(state.haloReviveCooldown);
            buffer.writeInt(state.meteorTime);
            buffer.writeInt(state.odamaneFlags);
            buffer.writeBoolean(state.armorClimbing);
            buffer.writeInt(state.extraMagicShield);
            buffer.writeBoolean(state.preparingEndRitual);
            buffer.writeInt(state.spectreShieldCooldown);
        }
    };

    @Override public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = unknownData.copy();
        tag.putInt("PurpleInvulnerableTime", invulTick);
        tag.putInt("HaloRvvCooldown", haloReviveCooldown);
        tag.putInt("RvvMeteorTime", meteorTime);
        tag.putInt("OdamaneExpandedFlags", odamaneFlags);
        tag.putBoolean("ArmorClimbing", armorClimbing);
        tag.putInt("ExtraMagicShield", extraMagicShield);
        tag.putBoolean("PreparingEndRitual", preparingEndRitual);
        tag.putInt("spectreDarkmageMagicShieldCooldown", spectreShieldCooldown);
        return tag;
    }

    @Override public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        unknownData = tag.copy();
        invulTick = tag.getInt("PurpleInvulnerableTime");
        haloReviveCooldown = tag.getInt("HaloRvvCooldown");
        meteorTime = tag.getInt("RvvMeteorTime");
        odamaneFlags = tag.getInt("OdamaneExpandedFlags");
        armorClimbing = !tag.contains("ArmorClimbing") || tag.getBoolean("ArmorClimbing");
        extraMagicShield = tag.getInt("ExtraMagicShield");
        preparingEndRitual = tag.getBoolean("PreparingEndRitual");
        spectreShieldCooldown = tag.getInt("spectreDarkmageMagicShieldCooldown");
    }

    public int getInvulTick() { return invulTick; }
    public void setInvulTick(int value) { invulTick = value; }
    public int getHaloReviveCooldown() { return haloReviveCooldown; }
    public void setHaloReviveCooldown(int value) { haloReviveCooldown = value; }
    public int getMeteorTime() { return meteorTime; }
    public void setMeteorTime(int value) { meteorTime = value; }
    public boolean isArmorClimbing() { return armorClimbing; }
}
