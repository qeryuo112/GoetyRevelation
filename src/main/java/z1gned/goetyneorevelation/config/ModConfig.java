package z1gned.goetyneorevelation.config;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import z1gned.goetyneorevelation.GoetyNeoRevelation;

/**
 * 原模组七项配置（NeoForge 原生表达）。
 *
 * <p>依据外层字节码审计，七项键均为<strong>根级</strong>键（define 在 build 之前完成，
 * 原 General push 在全部 define 之后为空壳，不影响键位置）。APOLLYON_HEALTH 默认值
 * 由 RevelationFix 改为 666（非反编译显示的 560）。
 *
 * <p>本批仅加载与提供默认值；不声称 combat 等后续消费逻辑已实现。
 */
public class ModConfig {

    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    // ===== 七项根级原配置键 =====
    public static final ModConfigSpec.ConfigValue<Boolean> PLAYER_INVULNERABLE;
    public static final ModConfigSpec.ConfigValue<Boolean> ENABLE_PLAYER_AMBIENT;
    public static final ModConfigSpec.ConfigValue<Double> HELLFIRE_DAMAGE_AMOUNT;
    public static final ModConfigSpec.ConfigValue<Double> HELLCLOUD_DAMAGE_AMOUNT;
    public static final ModConfigSpec.ConfigValue<Double> APOLLYON_HEALTH;
    public static final ModConfigSpec.ConfigValue<Double> APOLLYON_HURT_LIMIT;
    public static final ModConfigSpec.ConfigValue<Integer> SUMMON_APOLLYON_RANDOM_VALUE;

    static {
        PLAYER_INVULNERABLE = BUILDER
                .comment("Determines whether the player will be invincible when there is an Obsidian monolith, Default: true")
                .define("player_invincible", true);

        ENABLE_PLAYER_AMBIENT = BUILDER
                .comment("Enable player ambient sound, Default: true")
                .define("player_ambient_sound", true);

        HELLFIRE_DAMAGE_AMOUNT = BUILDER
                .comment("Sets the damage percentage Hellfire deals each time, Default: 0.0444")
                .define("hellfire_damage_amount", 0.0444);

        HELLCLOUD_DAMAGE_AMOUNT = BUILDER
                .comment("Sets the damage percentage Hellcloud deals each time, Default: 0.001")
                .define("hellcloud_damage_amount", 0.001);

        APOLLYON_HEALTH = BUILDER
                .comment("Sets the apollyon's health, Default: 666")
                .define("apollyon_health", 666.0);

        APOLLYON_HURT_LIMIT = BUILDER
                .comment("Limit the amount of damage Apollyon takes each time. Default: 20.0")
                .define("apollyon_hurt_limit", 20.0);

        SUMMON_APOLLYON_RANDOM_VALUE = BUILDER
                .comment("The probability of summoning Apollyon during the ritual of summoning an apostle (Chance: 1 / value)")
                .defineInRange("summon_apollyon_random_value", 100, 1, Integer.MAX_VALUE);

        // define 完成后最后 build（全部为根级键，不套用空壳 General 分类）
        SPEC = BUILDER.build();
    }

    public static void registerLifecycle(IEventBus modEventBus) {
        modEventBus.addListener((ModConfigEvent.Loading event) -> logConfig("load", event));
        modEventBus.addListener((ModConfigEvent.Reloading event) -> logConfig("reload", event));
    }

    private static void logConfig(String operation, ModConfigEvent event) {
        if (event.getConfig().getSpec() == SPEC) {
            GoetyNeoRevelation.LOGGER.debug(
                    "[config] {} file={} rootKeys={} apollyonHealth={} summonBound={} hellfire={} hellcloud={}",
                    operation, event.getConfig().getFileName(), SPEC.getValues().valueMap().keySet(),
                    APOLLYON_HEALTH.get(), SUMMON_APOLLYON_RANDOM_VALUE.get(),
                    HELLFIRE_DAMAGE_AMOUNT.get(), HELLCLOUD_DAMAGE_AMOUNT.get());
        }
    }

    private ModConfig() {
    }
}
