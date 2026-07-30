package io.github.gev414.rotwire.config;

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
    public static ModConfigSpec.BooleanValue STREET_SPAWNS_ENABLED;
    public static ModConfigSpec.IntValue STREET_SPAWN_INTERVAL_TICKS;
    public static ModConfigSpec.DoubleValue STREET_SPAWN_CHANCE;
    public static ModConfigSpec.DoubleValue
            STREET_NIGHTTIME_CHANCE_MULTIPLIER;
    public static ModConfigSpec.IntValue STREET_ZOMBIE_CAP;
    public static ModConfigSpec.IntValue STREET_ZOMBIE_CAP_RADIUS;
    public static ModConfigSpec.IntValue STREET_MINIMUM_SPAWN_DISTANCE;
    public static ModConfigSpec.IntValue STREET_MAXIMUM_SPAWN_DISTANCE;
    public static ModConfigSpec.IntValue STREET_SPAWN_POSITION_ATTEMPTS;

    public static void initialize() {
        if (SPEC != null) {
            return;
        }

        BUILDER.comment(
                "Persistent Lost Cities operations and danger zones.",
                "Radio surveys are deliberately spread across server ticks."
        ).push("cityOperations");

        ENABLED = BUILDER
                .comment(
                        "Enable city surveying, progress, infected scaling, "
                                + "and street spawns."
                )
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

        BUILDER.push("streetSpawns");
        STREET_SPAWNS_ENABLED = BUILDER
                .comment(
                        "Spawn uncommon roaming zombies in Lost Cities street chunks.",
                        "These zombies are ambient mobs, not building encounters."
                )
                .define("enabled", true);
        STREET_SPAWN_INTERVAL_TICKS = BUILDER
                .comment("Ticks between one ambient spawn roll per player.")
                .defineInRange("intervalTicks", 200, 20, 72_000);
        STREET_SPAWN_CHANCE = BUILDER
                .comment("Chance that each player's interval roll attempts one spawn.")
                .defineInRange("chance", 0.15D, 0.0D, 1.0D);
        STREET_NIGHTTIME_CHANCE_MULTIPLIER = BUILDER
                .comment(
                        "Multiplier applied to the street spawn chance at night.",
                        "The resulting chance is capped at 1.0."
                )
                .defineInRange(
                        "nighttimeChanceMultiplier",
                        3.0D,
                        1.0D,
                        16.0D
                );
        STREET_ZOMBIE_CAP = BUILDER
                .comment(
                        "Maximum Rotwire street zombies near a player.",
                        "A value of zero disables spawning without changing enabled."
                )
                .defineInRange("nearbyCap", 4, 0, 128);
        STREET_ZOMBIE_CAP_RADIUS = BUILDER
                .comment("Radius used to enforce the nearby street-zombie cap.")
                .defineInRange("nearbyCapRadius", 96, 16, 256);
        STREET_MINIMUM_SPAWN_DISTANCE = BUILDER
                .comment("Minimum horizontal spawn distance from every player.")
                .defineInRange("minimumDistance", 28, 1, 128);
        STREET_MAXIMUM_SPAWN_DISTANCE = BUILDER
                .comment("Maximum horizontal spawn distance from the anchor player.")
                .defineInRange("maximumDistance", 64, 1, 256);
        STREET_SPAWN_POSITION_ATTEMPTS = BUILDER
                .comment("Candidate street positions tested per successful spawn roll.")
                .defineInRange("positionAttempts", 16, 1, 128);
        BUILDER.pop();

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    public static int minimumStreetSpawnDistance() {
        return Math.min(
                STREET_MINIMUM_SPAWN_DISTANCE.get(),
                STREET_MAXIMUM_SPAWN_DISTANCE.get()
        );
    }

    public static int maximumStreetSpawnDistance() {
        return Math.max(
                STREET_MINIMUM_SPAWN_DISTANCE.get(),
                STREET_MAXIMUM_SPAWN_DISTANCE.get()
        );
    }

    private CityOperationsConfig() {
    }
}
