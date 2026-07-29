package io.github.gev414.rotwire.config;

import io.github.gev414.rotwire.weather.WeatherGenerationRules;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class WeatherConfig {

    private static final ModConfigSpec.Builder BUILDER =
            new ModConfigSpec.Builder();

    public static ModConfigSpec SPEC;
    public static ModConfigSpec.BooleanValue ENABLED;
    public static ModConfigSpec.BooleanValue SEASONAL_WEIGHTING;
    public static ModConfigSpec.IntValue HAZARDOUS_START_DAY;
    public static ModConfigSpec.IntValue CLEAR_WEIGHT;
    public static ModConfigSpec.IntValue RAIN_WEIGHT;
    public static ModConfigSpec.IntValue STORM_WEIGHT;
    public static ModConfigSpec.IntValue CONTAMINATED_RAIN_WEIGHT;
    public static ModConfigSpec.IntValue CONTAMINATED_STORM_WEIGHT;
    public static ModConfigSpec.IntValue NORMAL_MINIMUM_DURATION_TICKS;
    public static ModConfigSpec.IntValue NORMAL_MAXIMUM_DURATION_TICKS;
    public static ModConfigSpec.IntValue HAZARDOUS_MINIMUM_DURATION_TICKS;
    public static ModConfigSpec.IntValue HAZARDOUS_MAXIMUM_DURATION_TICKS;
    public static ModConfigSpec.IntValue RAIN_GRACE_TICKS;
    public static ModConfigSpec.IntValue STORM_GRACE_TICKS;
    public static ModConfigSpec.IntValue RAIN_DAMAGE_INTERVAL_TICKS;
    public static ModConfigSpec.IntValue STORM_DAMAGE_INTERVAL_TICKS;
    public static ModConfigSpec.DoubleValue DAMAGE_AMOUNT;
    public static ModConfigSpec.DoubleValue RAIN_SUSPICION_MULTIPLIER;
    public static ModConfigSpec.DoubleValue STORM_SUSPICION_MULTIPLIER;
    public static ModConfigSpec.DoubleValue RAIN_ATTENTION_MULTIPLIER;
    public static ModConfigSpec.DoubleValue STORM_ATTENTION_MULTIPLIER;

    public static void initialize() {
        if (SPEC != null) {
            return;
        }

        BUILDER.comment(
                "Authoritative two-day weather forecasts, contaminated",
                "precipitation hazards, and ordinary-weather stealth benefits."
        ).push("weather");

        ENABLED = BUILDER
                .comment(
                        "Let Rotwire schedule Overworld weather and expose",
                        "today/tomorrow forecasts through calibrated radios."
                )
                .define("enabled", true);
        SEASONAL_WEIGHTING = BUILDER
                .comment(
                        "Use Serene Seasons, when installed, to adjust",
                        "weather-type weights without changing its visuals."
                )
                .define("seasonalWeighting", true);
        HAZARDOUS_START_DAY = BUILDER
                .comment(
                        "First world day that may receive contaminated weather.",
                        "Day zero is the world's first day."
                )
                .defineInRange("hazardousStartDay", 5, 0, 1_000_000);

        BUILDER.push("weights");
        CLEAR_WEIGHT = weight("clear", 40);
        RAIN_WEIGHT = weight("rain", 30);
        STORM_WEIGHT = weight("storm", 15);
        CONTAMINATED_RAIN_WEIGHT =
                weight("contaminatedRain", 10);
        CONTAMINATED_STORM_WEIGHT =
                weight("contaminatedStorm", 5);
        BUILDER.pop();

        BUILDER.push("duration");
        NORMAL_MINIMUM_DURATION_TICKS = BUILDER
                .comment("Minimum duration of ordinary rain or storms.")
                .defineInRange(
                        "normalMinimumTicks",
                        6_000,
                        200,
                        23_000
                );
        NORMAL_MAXIMUM_DURATION_TICKS = BUILDER
                .comment("Maximum duration of ordinary rain or storms.")
                .defineInRange(
                        "normalMaximumTicks",
                        12_000,
                        200,
                        23_000
                );
        HAZARDOUS_MINIMUM_DURATION_TICKS = BUILDER
                .comment("Minimum duration of contaminated precipitation.")
                .defineInRange(
                        "hazardousMinimumTicks",
                        4_000,
                        200,
                        23_000
                );
        HAZARDOUS_MAXIMUM_DURATION_TICKS = BUILDER
                .comment("Maximum duration of contaminated precipitation.")
                .defineInRange(
                        "hazardousMaximumTicks",
                        7_000,
                        200,
                        23_000
                );
        BUILDER.pop();

        BUILDER.push("exposure");
        RAIN_GRACE_TICKS = BUILDER
                .comment(
                        "Open-sky grace period in contaminated rain.",
                        "The default is four seconds."
                )
                .defineInRange("rainGraceTicks", 80, 0, 1_200);
        STORM_GRACE_TICKS = BUILDER
                .comment(
                        "Open-sky grace period in a contaminated storm.",
                        "The default is two seconds."
                )
                .defineInRange("stormGraceTicks", 40, 0, 1_200);
        RAIN_DAMAGE_INTERVAL_TICKS = BUILDER
                .comment("Ticks between contaminated-rain damage pulses.")
                .defineInRange(
                        "rainDamageIntervalTicks",
                        100,
                        1,
                        1_200
                );
        STORM_DAMAGE_INTERVAL_TICKS = BUILDER
                .comment("Ticks between contaminated-storm damage pulses.")
                .defineInRange(
                        "stormDamageIntervalTicks",
                        60,
                        1,
                        1_200
                );
        DAMAGE_AMOUNT = BUILDER
                .comment(
                        "Damage per pulse after the grace period.",
                        "Two damage points equal one full heart."
                )
                .defineInRange("damageAmount", 2.0D, 0.0D, 100.0D);
        BUILDER.pop();

        BUILDER.push("stealth");
        RAIN_SUSPICION_MULTIPLIER = multiplier(
                "rainSuspicionMultiplier",
                0.85D,
                "Visual suspicion gain during rain."
        );
        STORM_SUSPICION_MULTIPLIER = multiplier(
                "stormSuspicionMultiplier",
                0.70D,
                "Visual suspicion gain during thunderstorms."
        );
        RAIN_ATTENTION_MULTIPLIER = multiplier(
                "rainAttentionMultiplier",
                0.80D,
                "Investigation range of loud actions during rain."
        );
        STORM_ATTENTION_MULTIPLIER = multiplier(
                "stormAttentionMultiplier",
                0.60D,
                "Investigation range of loud actions during thunderstorms."
        );
        BUILDER.pop();

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    public static WeatherGenerationRules generationRules() {
        return new WeatherGenerationRules(
                HAZARDOUS_START_DAY.get(),
                CLEAR_WEIGHT.get(),
                RAIN_WEIGHT.get(),
                STORM_WEIGHT.get(),
                CONTAMINATED_RAIN_WEIGHT.get(),
                CONTAMINATED_STORM_WEIGHT.get(),
                NORMAL_MINIMUM_DURATION_TICKS.get(),
                NORMAL_MAXIMUM_DURATION_TICKS.get(),
                HAZARDOUS_MINIMUM_DURATION_TICKS.get(),
                HAZARDOUS_MAXIMUM_DURATION_TICKS.get()
        );
    }

    private static ModConfigSpec.IntValue weight(
            String key,
            int defaultValue
    ) {
        return BUILDER
                .comment(
                        "Relative daily selection weight for " + key + ".",
                        "All five weights may be tuned together."
                )
                .defineInRange(key, defaultValue, 0, 10_000);
    }

    private static ModConfigSpec.DoubleValue multiplier(
            String key,
            double defaultValue,
            String comment
    ) {
        return BUILDER
                .comment(comment, "One means no weather adjustment.")
                .defineInRange(key, defaultValue, 0.0D, 1.0D);
    }

    private WeatherConfig() {
    }
}
