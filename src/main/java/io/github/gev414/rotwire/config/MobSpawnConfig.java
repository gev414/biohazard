package io.github.gev414.rotwire.config;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public final class MobSpawnConfig {

    private static final ModConfigSpec.Builder BUILDER =
            new ModConfigSpec.Builder();

    public static ModConfigSpec SPEC;
    public static ModConfigSpec.BooleanValue UNDERGROUND_RESTRICTIONS_ENABLED;
    public static ModConfigSpec.BooleanValue RESTRICT_SKELETONS;
    public static ModConfigSpec.BooleanValue RESTRICT_CREEPERS;
    public static ModConfigSpec.IntValue MINIMUM_DEPTH_BELOW_SEA_LEVEL;
    public static ModConfigSpec.ConfigValue<List<? extends String>>
            UNDERGROUND_RESTRICTION_DIMENSIONS;
    public static ModConfigSpec.BooleanValue WILDERNESS_ZOMBIES_ENABLED;
    public static ModConfigSpec.IntValue WILDERNESS_SPAWN_INTERVAL_TICKS;
    public static ModConfigSpec.DoubleValue WILDERNESS_SPAWN_CHANCE;
    public static ModConfigSpec.IntValue WILDERNESS_ZOMBIE_CAP;
    public static ModConfigSpec.IntValue WILDERNESS_ZOMBIE_CAP_RADIUS;
    public static ModConfigSpec.IntValue WILDERNESS_MINIMUM_SPAWN_DISTANCE;
    public static ModConfigSpec.IntValue WILDERNESS_MAXIMUM_SPAWN_DISTANCE;
    public static ModConfigSpec.IntValue WILDERNESS_SPAWN_POSITION_ATTEMPTS;
    public static ModConfigSpec.ConfigValue<List<? extends String>>
            WILDERNESS_DIMENSIONS;

    public static void initialize() {
        if (SPEC != null) {
            return;
        }

        BUILDER.comment(
                "Rotwire-owned hostile-mob spawning rules."
        ).push("mobSpawning");

        BUILDER.comment(
                "Natural-spawn depth restrictions.",
                "Explicit sources such as spawners, commands, spawn eggs,",
                "structures, and scripted events are not affected."
        ).push("undergroundRestrictions");
        UNDERGROUND_RESTRICTIONS_ENABLED = BUILDER
                .comment("Enable Rotwire's natural mob-spawn restrictions.")
                .define("enabled", true);
        RESTRICT_SKELETONS = BUILDER
                .comment(
                        "Keep naturally spawned vanilla skeleton-family mobs",
                        "underground: skeletons, strays, bogged, and",
                        "wither skeletons."
                )
                .define("restrictSkeletons", true);
        RESTRICT_CREEPERS = BUILDER
                .comment(
                        "Keep naturally spawned vanilla creepers underground."
                )
                .define("restrictCreepers", true);
        MINIMUM_DEPTH_BELOW_SEA_LEVEL = BUILDER
                .comment(
                        "Minimum blocks below a dimension's sea level at which",
                        "restricted mobs may spawn naturally.",
                        "At the Overworld's sea level of 63, the default permits",
                        "spawns at Y 47 and lower."
                )
                .defineInRange(
                        "minimumDepthBelowSeaLevel",
                        16,
                        0,
                        384
                );
        UNDERGROUND_RESTRICTION_DIMENSIONS = BUILDER
                .comment(
                        "Dimension IDs where the restrictions apply.",
                        "Unlisted dimensions retain their normal spawn rules."
                )
                .defineList(
                        "dimensions",
                        List.of("minecraft:overworld"),
                        () -> "minecraft:overworld",
                        MobSpawnConfig::isDimensionId
                );
        BUILDER.pop();

        BUILDER.comment(
                "Scarce outdoor zombies beyond Lost Cities.",
                "These use surface placement and bypass the darkness check."
        ).push("wildernessZombies");
        WILDERNESS_ZOMBIES_ENABLED = BUILDER
                .comment("Allow occasional zombies in outdoor wilderness.")
                .define("enabled", true);
        WILDERNESS_SPAWN_INTERVAL_TICKS = BUILDER
                .comment("Ticks between one wilderness spawn roll per player.")
                .defineInRange("intervalTicks", 200, 20, 72_000);
        WILDERNESS_SPAWN_CHANCE = BUILDER
                .comment(
                        "Chance that each interval roll searches for one zombie.",
                        "The default averages one successful roll every",
                        "six to seven minutes per eligible player."
                )
                .defineInRange("chance", 0.025D, 0.0D, 1.0D);
        WILDERNESS_ZOMBIE_CAP = BUILDER
                .comment(
                        "Maximum Rotwire wilderness zombies near a player.",
                        "A value of zero disables spawning without changing enabled."
                )
                .defineInRange("nearbyCap", 2, 0, 128);
        WILDERNESS_ZOMBIE_CAP_RADIUS = BUILDER
                .comment("Horizontal radius used for the wilderness cap.")
                .defineInRange("nearbyCapRadius", 128, 16, 256);
        WILDERNESS_MINIMUM_SPAWN_DISTANCE = BUILDER
                .comment("Minimum horizontal spawn distance from every player.")
                .defineInRange("minimumDistance", 32, 1, 128);
        WILDERNESS_MAXIMUM_SPAWN_DISTANCE = BUILDER
                .comment("Maximum horizontal distance from the anchor player.")
                .defineInRange("maximumDistance", 72, 1, 256);
        WILDERNESS_SPAWN_POSITION_ATTEMPTS = BUILDER
                .comment("Outdoor positions tested after a successful roll.")
                .defineInRange("positionAttempts", 24, 1, 128);
        WILDERNESS_DIMENSIONS = BUILDER
                .comment(
                        "Dimension IDs where wilderness zombies may appear.",
                        "Unlisted dimensions retain their normal spawning."
                )
                .defineList(
                        "dimensions",
                        List.of("minecraft:overworld"),
                        () -> "minecraft:overworld",
                        MobSpawnConfig::isDimensionId
                );
        BUILDER.pop();

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    public static boolean restrictionsApplyTo(ResourceLocation dimension) {
        String id = dimension.toString();
        return UNDERGROUND_RESTRICTION_DIMENSIONS.get().stream()
                .anyMatch(id::equals);
    }

    public static boolean wildernessZombiesApplyTo(
            ResourceLocation dimension
    ) {
        String id = dimension.toString();
        return WILDERNESS_DIMENSIONS.get().stream().anyMatch(id::equals);
    }

    public static int minimumWildernessSpawnDistance() {
        return Math.min(
                WILDERNESS_MINIMUM_SPAWN_DISTANCE.get(),
                WILDERNESS_MAXIMUM_SPAWN_DISTANCE.get()
        );
    }

    public static int maximumWildernessSpawnDistance() {
        return Math.max(
                WILDERNESS_MINIMUM_SPAWN_DISTANCE.get(),
                WILDERNESS_MAXIMUM_SPAWN_DISTANCE.get()
        );
    }

    private static boolean isDimensionId(Object value) {
        return value instanceof String id
                && ResourceLocation.tryParse(id) != null;
    }

    private MobSpawnConfig() {
    }
}
