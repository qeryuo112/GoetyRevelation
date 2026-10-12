package z1gned.goetyneorevelation.gametest;

import io.netty.buffer.Unpooled;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.attachment.AttachmentSyncHandler;
import z1gned.goetyneorevelation.GoetyNeoRevelation;
import z1gned.goetyneorevelation.combat.HaloLegacyMigration;
import z1gned.goetyneorevelation.state.HaloAttachments;
import z1gned.goetyneorevelation.state.HaloState;

@GameTestHolder(GoetyNeoRevelation.MOD_ID)
@PrefixGameTestTemplate(false)
public class P2StateGameTests {
    @GameTest(template = "empty")
    public static void nbt_defaults_and_unknown_fields(GameTestHelper helper) {
        HaloState state = new HaloState();
        state.deserializeNBT(helper.getLevel().registryAccess(), new CompoundTag());
        helper.assertTrue(state.isArmorClimbing(), "缺键默认ArmorClimbing=true，不得读成false");
        CompoundTag legacy = new CompoundTag();
        legacy.putInt("PurpleInvulnerableTime", 400);
        legacy.putInt("HaloRvvCooldown", 36000);
        legacy.putInt("RvvMeteorTime", 6000);
        legacy.putString("FutureExtraField", "keep-me");
        legacy.putBoolean("ArmorClimbing", false);
        state.deserializeNBT(helper.getLevel().registryAccess(), legacy);
        HaloState restored = new HaloState();
        restored.deserializeNBT(helper.getLevel().registryAccess(), state.serializeNBT(helper.getLevel().registryAccess()));
        helper.assertTrue(restored.getInvulTick() == 400 && restored.getHaloReviveCooldown() == 36000
                && restored.getMeteorTime() == 6000 && !restored.isArmorClimbing(), "原字段NBT往返必须准确");
        helper.assertTrue(restored.serializeNBT(helper.getLevel().registryAccess()).getString("FutureExtraField").equals("keep-me"),
                "暂未使用额外字段不得丢弃");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void attachment_isolation_and_clone(GameTestHelper helper) {
        var first = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var second = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var state = first.getData(HaloAttachments.PLAYER);
        state.setHaloReviveCooldown(36000);
        helper.assertTrue(state != second.getData(HaloAttachments.PLAYER), "每玩家必须不同状态对象");
        helper.assertTrue(second.getData(HaloAttachments.PLAYER).getHaloReviveCooldown() == 0, "状态不得跨玩家污染");
        second.copyAttachmentsFrom(first, true);
        var copy = second.getData(HaloAttachments.PLAYER);
        helper.assertTrue(copy != state && copy.getHaloReviveCooldown() == 36000, "死亡复制必须深拷贝");
        copy.setHaloReviveCooldown(12);
        helper.assertTrue(state.getHaloReviveCooldown() == 36000, "克隆对象修改不得反写旧玩家");
        second.copyAttachmentsFrom(first, false);
        helper.assertTrue(second.getData(HaloAttachments.PLAYER).getHaloReviveCooldown() == 36000, "End返回复制不可累加");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void native_entity_persistence_and_legacy(GameTestHelper helper) {
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.getData(HaloAttachments.PLAYER).setInvulTick(13);
        player.getData(HaloAttachments.PLAYER).setHaloReviveCooldown(25);
        CompoundTag saved = new CompoundTag();
        player.saveWithoutId(saved);
        var restored = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        restored.load(saved);
        helper.assertTrue(restored.getData(HaloAttachments.PLAYER).getInvulTick() == 13
                && restored.getData(HaloAttachments.PLAYER).getHaloReviveCooldown() == 25, "Entity真实保存重读必须带native附件");
        var legacyPlayer = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        CompoundTag legacy = new CompoundTag();
        CompoundTag cap = new CompoundTag();
        cap.putInt("HaloRvvCooldown", 1000);
        CompoundTag forgeCaps = new CompoundTag();
        forgeCaps.put("goety_revelation:gr_player_cap", cap);
        legacy.put("ForgeCaps", forgeCaps);
        HaloLegacyMigration.read(legacyPlayer, legacy);
        helper.assertTrue(legacyPlayer.getData(HaloAttachments.PLAYER).getHaloReviveCooldown() == 1000,
                "旧明确命名ForgeCaps需迁移");
        cap.putInt("HaloRvvCooldown", 50);
        HaloLegacyMigration.read(legacyPlayer, legacy);
        helper.assertTrue(legacyPlayer.getData(HaloAttachments.PLAYER).getHaloReviveCooldown() == 1000,
                "新附件存在后不得用旧cap覆盖");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void native_sync_codec_roundtrip(GameTestHelper helper) {
        HaloState state = new HaloState();
        state.setInvulTick(400); state.setHaloReviveCooldown(36000); state.setMeteorTime(6000);
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            HaloState.STREAM_CODEC.encode(buffer, state);
            helper.assertTrue(buffer.readableBytes() == 26, "固定26字节同步，禁止无界NBT包");
            HaloState decoded = HaloState.STREAM_CODEC.decode(buffer);
            helper.assertTrue(decoded != state && decoded.getInvulTick() == 400 && decoded.getHaloReviveCooldown() == 36000
                    && decoded.getMeteorTime() == 6000, "原生sync使用的真实stream codec往返准确");
        } finally { buffer.release(); }
        helper.succeed();
    }
}
