package z1gned.goetyneorevelation;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import z1gned.goetyneorevelation.item.ModItems;

/**
 * Goety: Neo Revelation —— NeoForge 1.21.1 移植工程入口（P1 限定纵切）。
 *
 * <p>本批仅实现：两个光环的物品注册（旧命名空间 goety_revelation）、
 * 七项根级 NeoForge 配置、Shift/Alt 提示（客户端隔离）与掉落实体免疫 Mixin。
 * 不实现 state/damage 战斗、Shader/AI/实体等后续批次内容。
 */
@Mod(GoetyNeoRevelation.MOD_ID)
public class GoetyNeoRevelation {

    /**
     * 模组标识（用于 mods.toml、事件总线、GameTest 命名空间）。
     */
    public static final String MOD_ID = "goety_neorevelation";

    /**
     * 旧资源/注册命名空间（决定物品 ResourceLocation 前缀，与 MOD_ID 不同）。
     * 决定 goety_revelation:ascension_halo / goety_revelation:broken_halo 的注册路径。
     */
    public static final String LEGACY_NAMESPACE = "goety_revelation";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public GoetyNeoRevelation(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("[registry] Goety: Neo Revelation 正在加载 ...");

        // 物品注册：命名空间使用旧 goety_revelation，事件总线绑定。
        ModItems.ITEMS.register(modEventBus);
        z1gned.goetyneorevelation.state.HaloAttachments.register(modEventBus);
        z1gned.goetyneorevelation.entity.ModEntities.register(modEventBus);

        // 配置注册：七项根级原配置键，配置文件沿用原 goety_revelation.toml。
        // 注意：仅 COMMON 类型，不声称 combat 等后续消费已实现。
        z1gned.goetyneorevelation.config.ModConfig.registerLifecycle(modEventBus);
        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.COMMON,
                z1gned.goetyneorevelation.config.ModConfig.SPEC, "goety_revelation.toml");
        LOGGER.debug("[config] 已注册 COMMON 配置 goety_revelation.toml");
        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.COMMON,
                z1gned.goetyneorevelation.config.HaloConfig.SPEC, "goety_neorevelation-halo.toml");

        // 客户端提示处理器由 @EventBusSubscriber(value = Dist.CLIENT) 自动在客户端注册，
        // 公共代码（本类）不引用 Screen / DistExecutor，保持客户端隔离。
    }
}
