package z1gned.goetyneorevelation.config;

import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.common.ModConfigSpec;

/** The implemented Halo slice of Fix common config; other groups remain unmigrated. */
public final class HaloConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue DAMAGE_CAP;
    public static final ModConfigSpec.BooleanValue UNDEAD_SUPPRESSION;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> BYPASS_EFFECTS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("Halo of Ascension");
        UNDEAD_SUPPRESSION = builder.comment("Original Fix: undead cannot attack complete Halo wearers, except Apostle.",
                        "When false, only undead within Goety lichPowerfulFoesHealth are suppressed.")
                .worldRestart().define("haloUndeadSuppression", true);
        DAMAGE_CAP = builder.comment("Maximum Halo damage before armor. Original Fix default: 20.0")
                .worldRestart().defineInRange("haloDamageCap", 20.0, 0.0, Double.MAX_VALUE);
        builder.pop();
        builder.push("Apollyon");
        BYPASS_EFFECTS = builder.comment("Effects allowed by the original shared whitelist.")
                .worldRestart().defineListAllowEmpty("whitelistMobEffects",
                        List.of("minecraft:night_vision", "cataclysm:ghost_sickness", "aquamirae:crystallization"),
                        value -> value instanceof String name && ResourceLocation.tryParse(name) != null);
        builder.pop();
        SPEC = builder.build();
    }

    public static boolean bypassesEffect(Holder<MobEffect> effect) {
        ResourceLocation id = BuiltInRegistries.MOB_EFFECT.getKey(effect.value());
        return id != null && BYPASS_EFFECTS.get().contains(id.toString());
    }

    private HaloConfig() {}
}
