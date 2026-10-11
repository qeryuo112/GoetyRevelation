package z1gned.goetyneorevelation;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Goety: Neo Revelation —— NeoForge 1.21.1 移植工程入口。
 *
 * <p>移植工作从这里展开：注册表、事件、客户端渲染等。
 */
@Mod(GoetyNeoRevelation.MOD_ID)
public class GoetyNeoRevelation {

    public static final String MOD_ID = "goety_neorevelation";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public GoetyNeoRevelation(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Goety: Neo Revelation 正在加载 ...");
    }
}
