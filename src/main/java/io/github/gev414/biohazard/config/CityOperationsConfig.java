package io.github.gev414.biohazard.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class CityOperationsConfig {

    private static final ModConfigSpec.Builder BUILDER =
            new ModConfigSpec.Builder();

    public static ModConfigSpec SPEC;
    public static ModConfigSpec.BooleanValue ENABLED;
    public static ModConfigSpec.IntValue SURVEY_CHUNKS_PER_TICK;
    public static ModConfigSpec.IntValue MAX_SURVEYED_CHUNKS;
    public static ModConfigSpec.BooleanValue DIAGONAL_CONNECTIVITY;
    public static ModConfigSpec.IntValue FALLBACK_SECTOR_SIZE_CHUNKS;
    public static ModConfigSpec.IntValue INFLUENCE_PERIMETER_CHUNKS;
    public static ModConfigSpec.IntValue CLEARED_BUILDINGS_PER_LEVEL;
    public static ModConfigSpec.IntValue MAX_DANGER_LEVEL;
    public static ModConfigSpec.DoubleValue HEALTH_PER_DANGER_LEVEL;
    public static ModConfigSpec.DoubleValue BRUTE_HEALTH_PER_DANGER_LEVEL;

    public static void initialize() {
        if (SPEC != null) {
            return;
        }

        BUILDER.comment(
                "Persistent Lost Cities operations and danger zones.",
                "Radio surveys are deliberately spread across server ticks."
        ).push("cityOperations");

        ENABLED = BUILDER
                .comment("Enable city surveying, progress, and infected scaling.")
                .define("enabled", true);

        BUILDER.push("survey");
        SURVEY_CHUNKS_PER_TICK = BUILDER
                .comment("Maximum candidate chunks inspected by each radio per tick.")
                .defineInRange("chunksPerTick", 16, 1, 256);
        MAX_SURVEYED_CHUNKS = BUILDER
                .comment(
                        "Hard cap for one connected-city survey.",
                        "Surveys that reach this cap use stable fallback sectors."
                )
                .defineInRange("maxChunks", 16_384, 64, 262_144);
        DIAGONAL_CONNECTIVITY = BUILDER
                .comment("Whether diagonally touching city chunks are connected.")
                .define("diagonalConnectivity", false);
        FALLBACK_SECTOR_SIZE_CHUNKS = BUILDER
                .comment("Width and depth of a capped survey's fallback sector.")
                .defineInRange("fallbackSectorSizeChunks", 32, 8, 256);
        BUILDER.pop();

        BUILDER.push("danger");
        INFLUENCE_PERIMETER_CHUNKS = BUILDER
                .comment(
                        "Extra chunk perimeter where a city's danger affects infected.",
                        "Five chunks is 80 blocks and covers the current horde distance."
                )
                .defineInRange("influencePerimeterChunks", 5, 0, 32);
        CLEARED_BUILDINGS_PER_LEVEL = BUILDER
                .comment("Unique cleared encounter buildings required per level.")
                .defineInRange("clearedBuildingsPerLevel", 5, 1, 1_000);
        MAX_DANGER_LEVEL = BUILDER
                .comment("Maximum danger level for an individual city zone.")
                .defineInRange("maxLevel", 12, 0, 100);
        HEALTH_PER_DANGER_LEVEL = BUILDER
                .comment(
                        "Maximum-health increase per danger level for infected.",
                        "0.10 means ten percent of base maximum health."
                )
                .defineInRange("healthPerLevel", 0.10D, 0.0D, 10.0D);
        BRUTE_HEALTH_PER_DANGER_LEVEL = BUILDER
                .comment(
                        "Maximum-health increase per danger level for the Brute.",
                        "Kept separate so its high base health can be tuned."
                )
                .defineInRange("bruteHealthPerLevel", 0.10D, 0.0D, 10.0D);
        BUILDER.pop();

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    private CityOperationsConfig() {
    }
}
