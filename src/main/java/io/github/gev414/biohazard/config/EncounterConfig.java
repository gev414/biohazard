package io.github.gev414.biohazard.config;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public final class EncounterConfig {

    private static final ModConfigSpec.Builder BUILDER =
            new ModConfigSpec.Builder();

    public static ModConfigSpec SPEC;

    public static ModConfigSpec.BooleanValue ENABLED;
    public static ModConfigSpec.DoubleValue HAUNTED_CHANCE;
    public static ModConfigSpec.DoubleValue BOSS_CHANCE;
    public static ModConfigSpec.IntValue MIN_REGULAR_KILLS;
    public static ModConfigSpec.IntValue MAX_REGULAR_KILLS;
    public static ModConfigSpec.IntValue MAX_ACTIVE_REGULAR_MOBS;
    public static ModConfigSpec.IntValue UPDATE_INTERVAL_TICKS;
    public static ModConfigSpec.DoubleValue MIN_SPAWN_DISTANCE;
    public static ModConfigSpec.DoubleValue MAX_SPAWN_DISTANCE;
    public static ModConfigSpec.IntValue SPAWN_POSITION_ATTEMPTS;
    public static ModConfigSpec.IntValue BOSS_WARNING_TICKS;
    public static ModConfigSpec.BooleanValue LOCK_RANDOMIZABLE_CONTAINERS;
    public static ModConfigSpec.BooleanValue ANNOUNCE_STATE_CHANGES;
    public static ModConfigSpec.ConfigValue<List<? extends String>> REGULAR_MOBS;
    public static ModConfigSpec.ConfigValue<List<? extends String>> EXCLUDED_BUILDINGS;

    public static void initialize() {
        if (SPEC != null) {
            return;
        }

        BUILDER.comment(
                "Lost Cities haunted-building encounters. Selection and kill",
                "targets are persisted when a building is first materialized;",
                "changing those values only affects newly discovered buildings."
        ).push("encounters");

        ENABLED = BUILDER
                .comment("Master switch for new spawning and container locking.")
                .define("enabled", true);
        HAUNTED_CHANCE = BUILDER
                .comment("Chance that a newly discovered real building is haunted.")
                .defineInRange("hauntedChance", 0.70D, 0.0D, 1.0D);
        BOSS_CHANCE = BUILDER
                .comment("Chance that a haunted building has a Brute finale.")
                .defineInRange("bossChance", 0.20D, 0.0D, 1.0D);
        MIN_REGULAR_KILLS = BUILDER
                .comment("Minimum regular kills required, inclusive.")
                .defineInRange("minRegularKills", 8, 0, 10_000);
        MAX_REGULAR_KILLS = BUILDER
                .comment("Maximum regular kills required, inclusive.")
                .defineInRange("maxRegularKills", 15, 0, 10_000);
        MAX_ACTIVE_REGULAR_MOBS = BUILDER
                .comment("Maximum loaded regular encounter mobs per building.")
                .defineInRange("maxActiveRegularMobs", 4, 1, 128);
        UPDATE_INTERVAL_TICKS = BUILDER
                .comment("Ticks between occupied-building scans and spawn attempts.")
                .defineInRange("updateIntervalTicks", 200, 1, 72_000);
        MIN_SPAWN_DISTANCE = BUILDER
                .comment("Minimum regular/boss spawn distance from an occupant.")
                .defineInRange("minSpawnDistance", 8.0D, 0.0D, 128.0D);
        MAX_SPAWN_DISTANCE = BUILDER
                .comment("Maximum regular/boss spawn distance from an occupant.")
                .defineInRange("maxSpawnDistance", 16.0D, 0.0D, 128.0D);
        SPAWN_POSITION_ATTEMPTS = BUILDER
                .comment("Bounded candidate positions tried per building update.")
                .defineInRange("spawnPositionAttempts", 16, 1, 128);
        BOSS_WARNING_TICKS = BUILDER
                .comment("Delay after the regular wave empties before the Brute spawns.")
                .defineInRange("bossWarningTicks", 200, 1, 1_200);
        LOCK_RANDOMIZABLE_CONTAINERS = BUILDER
                .comment(
                        "Lock generated/randomizable containers until the encounter clears.",
                        "This conservative scope does not lock arbitrary machine inventories."
                )
                .define("lockRandomizableContainers", true);
        ANNOUNCE_STATE_CHANGES = BUILDER
                .comment("Send occupants haunted, boss-warning, and cleared messages.")
                .define("announceStateChanges", true);
        REGULAR_MOBS = BUILDER
                .comment(
                        "Entity ids used for the regular wave.",
                        "The Biohazard Brute is always rejected from this list."
                )
                .defineList(
                        "regularMobs",
                        List.of("minecraft:zombie", "minecraft:husk"),
                        () -> "minecraft:zombie",
                        value -> value instanceof String
                );
        EXCLUDED_BUILDINGS = BUILDER
                .comment(
                        "Lost Cities building or multibuilding ids that are always safe.",
                        "Example: lostcities:building_name"
                )
                .defineListAllowEmpty(
                        "excludedBuildings",
                        List.of(),
                        () -> "lostcities:building_name",
                        value -> value instanceof String
                );

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    public static int minimumKills() {
        return Math.min(MIN_REGULAR_KILLS.get(), MAX_REGULAR_KILLS.get());
    }

    public static int maximumKills() {
        return Math.max(MIN_REGULAR_KILLS.get(), MAX_REGULAR_KILLS.get());
    }

    public static double minimumSpawnDistance() {
        return Math.min(MIN_SPAWN_DISTANCE.get(), MAX_SPAWN_DISTANCE.get());
    }

    public static double maximumSpawnDistance() {
        return Math.max(MIN_SPAWN_DISTANCE.get(), MAX_SPAWN_DISTANCE.get());
    }

    public static boolean isExcluded(ResourceLocation buildingId) {
        String id = buildingId.toString();
        return EXCLUDED_BUILDINGS.get().stream()
                .anyMatch(id::equals);
    }

    private EncounterConfig() {
    }
}
