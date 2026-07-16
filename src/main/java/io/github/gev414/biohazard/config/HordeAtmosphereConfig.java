package io.github.gev414.biohazard.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class HordeAtmosphereConfig {

    private static final ModConfigSpec.Builder BUILDER =
            new ModConfigSpec.Builder();

    public static ModConfigSpec SPEC;

    public static ModConfigSpec.BooleanValue ENABLED;
    public static ModConfigSpec.IntValue FADE_DURATION_TICKS;
    public static ModConfigSpec.DoubleValue TARGET_NEAR_PLANE;
    public static ModConfigSpec.DoubleValue TARGET_FAR_PLANE;

    public static void initialize() {
        if (SPEC != null) {
            return;
        }

        BUILDER.comment(
                "Client-side fog announcing a scheduled horde.",
                "The server remains authoritative for horde-day state and",
                "The Hordes' configured day length and event start time."
        ).push("hordeAtmosphere");

        ENABLED = BUILDER
                .comment("Whether the pre-horde atmospheric fog is enabled.")
                .define("enabled", true);
        FADE_DURATION_TICKS = BUILDER
                .comment(
                        "Ticks before The Hordes' configured start time when",
                        "fog begins fading in. The default starts at noon for",
                        "a midnight horde in a standard 24000-tick day."
                )
                .defineInRange("fadeDurationTicks", 12_000, 0, 72_000);
        TARGET_NEAR_PLANE = BUILDER
                .comment("Fog start distance in blocks at full strength.")
                .defineInRange("targetNearPlane", 24.0D, 0.0D, 1_024.0D);
        TARGET_FAR_PLANE = BUILDER
                .comment("Fog end distance in blocks at full strength.")
                .defineInRange("targetFarPlane", 96.0D, 1.0D, 4_096.0D);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    private HordeAtmosphereConfig() {
    }
}
