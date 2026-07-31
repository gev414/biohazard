package io.github.gev414.rotwire.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class SurvivalSystemsConfig {

    private static final ModConfigSpec.Builder BUILDER =
            new ModConfigSpec.Builder();

    public static ModConfigSpec SPEC;

    public static ModConfigSpec.BooleanValue ENABLED;
    public static ModConfigSpec.IntValue UPDATE_INTERVAL_TICKS;

    public static ModConfigSpec.DoubleValue LIGHT_MAX_WEIGHT;
    public static ModConfigSpec.DoubleValue BURDENED_MAX_WEIGHT;
    public static ModConfigSpec.DoubleValue HEAVY_MAX_WEIGHT;
    public static ModConfigSpec.DoubleValue BURDENED_SPEED_PENALTY;
    public static ModConfigSpec.DoubleValue HEAVY_SPEED_PENALTY;
    public static ModConfigSpec.DoubleValue OVERLOADED_SPEED_PENALTY;
    public static ModConfigSpec.DoubleValue TINY_ITEM_WEIGHT;
    public static ModConfigSpec.DoubleValue LIGHT_ITEM_WEIGHT;
    public static ModConfigSpec.DoubleValue DEFAULT_ITEM_WEIGHT;
    public static ModConfigSpec.DoubleValue DENSE_ITEM_WEIGHT;
    public static ModConfigSpec.DoubleValue VERY_DENSE_ITEM_WEIGHT;
    public static ModConfigSpec.DoubleValue BLOCK_ITEM_WEIGHT;
    public static ModConfigSpec.DoubleValue UNSTACKABLE_ITEM_WEIGHT;
    public static ModConfigSpec.DoubleValue LIGHT_EQUIPMENT_WEIGHT;
    public static ModConfigSpec.DoubleValue ARMOR_ITEM_WEIGHT;
    public static ModConfigSpec.DoubleValue FIREARM_ITEM_WEIGHT;
    public static ModConfigSpec.DoubleValue HEAVY_EQUIPMENT_WEIGHT;
    public static ModConfigSpec.DoubleValue BACKPACK_BASE_WEIGHT;
    public static ModConfigSpec.DoubleValue BACKPACK_FLUID_WEIGHT_PER_BUCKET;

    public static ModConfigSpec.IntValue AWARENESS_SCAN_INTERVAL_TICKS;
    public static ModConfigSpec.DoubleValue DETECTION_RANGE;
    public static ModConfigSpec.DoubleValue CLOSE_DETECTION_RANGE;
    public static ModConfigSpec.DoubleValue FIELD_OF_VIEW_DEGREES;
    public static ModConfigSpec.DoubleValue SUSPICION_PER_SECOND;
    public static ModConfigSpec.DoubleValue SUSPICION_DECAY_PER_SECOND;
    public static ModConfigSpec.DoubleValue BRUTE_DETECTION_MULTIPLIER;
    public static ModConfigSpec.IntValue ALERT_MEMORY_TICKS;
    public static ModConfigSpec.IntValue LOUD_ACTION_GRACE_TICKS;

    public static ModConfigSpec.DoubleValue SUPPRESSED_FIRE_RANGE;
    public static ModConfigSpec.DoubleValue UNSUPPRESSED_FIRE_RANGE;
    public static ModConfigSpec.DoubleValue MELEE_ATTENTION_RANGE;
    public static ModConfigSpec.DoubleValue BLOCK_BREAK_ATTENTION_RANGE;
    public static ModConfigSpec.BooleanValue REPLACE_ZOMBIE_TACTICS_MARKERS;

    public static ModConfigSpec.BooleanValue KNOCKBACK_ENABLED;
    public static ModConfigSpec.DoubleValue ZOMBIE_KNOCKBACK_RETENTION;
    public static ModConfigSpec.DoubleValue PLAYER_KNOCKBACK_RETENTION;

    public static ModConfigSpec.BooleanValue SLEEP_SURVIVAL_ENABLED;
    public static ModConfigSpec.IntValue SLEEP_EFFECT_DURATION_TICKS;
    public static ModConfigSpec.IntValue SLEEP_EFFECT_PULSE_INTERVAL_TICKS;
    public static ModConfigSpec.IntValue SLEEP_METER_POINTS_PER_PULSE;
    public static ModConfigSpec.IntValue SLEEP_CAMPSITE_RADIUS;
    public static ModConfigSpec.IntValue
            SLEEP_CAMPSITE_FOOD_NUTRITION_THRESHOLD;

    public static void initialize() {
        if (SPEC != null) {
            return;
        }

        BUILDER.comment(
                "Encumbrance, stealth, attention, and sleep survival."
        ).push("survivalSystems");

        ENABLED = BUILDER
                .comment(
                        "Enable encumbrance, stealth, attention, and sleep",
                        "survival systems."
                )
                .define("enabled", true);
        UPDATE_INTERVAL_TICKS = BUILDER
                .comment("Ticks between player weight recalculations.")
                .defineInRange("updateIntervalTicks", 10, 1, 200);

        BUILDER.push("encumbrance");
        LIGHT_MAX_WEIGHT = BUILDER
                .comment(
                        "Maximum carried weight that remains Light.",
                        "Only crouched Light players qualify for quiet movement."
                )
                .defineInRange("lightMaxWeight", 16.0D, 0.0D, 10_000.0D);
        BURDENED_MAX_WEIGHT = BUILDER
                .comment("Maximum carried weight that remains Burdened.")
                .defineInRange("burdenedMaxWeight", 25.0D, 0.0D, 10_000.0D);
        HEAVY_MAX_WEIGHT = BUILDER
                .comment("Maximum carried weight that remains Heavy.")
                .defineInRange("heavyMaxWeight", 40.0D, 0.0D, 10_000.0D);
        BURDENED_SPEED_PENALTY = speedPenalty(
                "burdenedSpeedPenalty",
                0.10D
        );
        HEAVY_SPEED_PENALTY = speedPenalty(
                "heavySpeedPenalty",
                0.20D
        );
        OVERLOADED_SPEED_PENALTY = speedPenalty(
                "overloadedSpeedPenalty",
                0.35D
        );

        BUILDER.comment(
                "Stackable item weights are per individual item and scale",
                "linearly with count. Equipment categories are also per item",
                "but normally apply to unstackable items."
        ).push("weights");
        TINY_ITEM_WEIGHT = itemWeight(
                "tinyItem",
                0.02D
        );
        LIGHT_ITEM_WEIGHT = itemWeight(
                "lightItem",
                0.04D
        );
        DEFAULT_ITEM_WEIGHT = itemWeight(
                "defaultItem",
                0.06D
        );
        DENSE_ITEM_WEIGHT = itemWeight(
                "denseItem",
                0.08D
        );
        VERY_DENSE_ITEM_WEIGHT = itemWeight(
                "veryDenseItem",
                0.12D
        );
        BLOCK_ITEM_WEIGHT = itemWeight(
                "blockItem",
                0.10D
        );
        UNSTACKABLE_ITEM_WEIGHT = itemWeight(
                "unstackableItem",
                0.75D
        );
        LIGHT_EQUIPMENT_WEIGHT = itemWeight(
                "lightEquipment",
                1.25D
        );
        ARMOR_ITEM_WEIGHT = itemWeight(
                "armorItem",
                2.0D
        );
        FIREARM_ITEM_WEIGHT = itemWeight(
                "firearmItem",
                2.5D
        );
        HEAVY_EQUIPMENT_WEIGHT = itemWeight(
                "heavyEquipment",
                4.0D
        );
        BACKPACK_BASE_WEIGHT = itemWeight(
                "backpackBase",
                2.0D
        );
        BACKPACK_FLUID_WEIGHT_PER_BUCKET = itemWeight(
                "backpackFluidPerBucket",
                1.0D
        );
        BUILDER.pop();
        BUILDER.pop();

        BUILDER.push("stealth");
        AWARENESS_SCAN_INTERVAL_TICKS = BUILDER
                .comment("Ticks between progressive visual-awareness scans.")
                .defineInRange("scanIntervalTicks", 5, 1, 100);
        DETECTION_RANGE = BUILDER
                .comment("Maximum visual detection range for quiet players.")
                .defineInRange("detectionRange", 24.0D, 1.0D, 128.0D);
        CLOSE_DETECTION_RANGE = BUILDER
                .comment("Range where direct sight immediately detects a player.")
                .defineInRange("closeDetectionRange", 2.5D, 0.0D, 32.0D);
        FIELD_OF_VIEW_DEGREES = BUILDER
                .comment("Total visual field of view used for suspicion.")
                .defineInRange("fieldOfViewDegrees", 140.0D, 1.0D, 360.0D);
        SUSPICION_PER_SECOND = BUILDER
                .comment("Base suspicion gained per second of favorable sight.")
                .defineInRange("suspicionPerSecond", 35.0D, 0.0D, 1_000.0D);
        SUSPICION_DECAY_PER_SECOND = BUILDER
                .comment("Suspicion lost per second without favorable sight.")
                .defineInRange("suspicionDecayPerSecond", 20.0D, 0.0D, 1_000.0D);
        BRUTE_DETECTION_MULTIPLIER = BUILDER
                .comment("Brute suspicion-gain multiplier.")
                .defineInRange("bruteDetectionMultiplier", 2.5D, 1.0D, 100.0D);
        ALERT_MEMORY_TICKS = BUILDER
                .comment("Ticks an alerted infected remembers a quiet player.")
                .defineInRange("alertMemoryTicks", 400, 1, 72_000);
        LOUD_ACTION_GRACE_TICKS = BUILDER
                .comment(
                        "Ticks after a loud action before crouching can become",
                        "quiet again."
                )
                .defineInRange("loudActionGraceTicks", 40, 0, 1_200);
        BUILDER.pop();

        BUILDER.push("attention");
        SUPPRESSED_FIRE_RANGE = BUILDER
                .comment("Attention radius of a suppressed PointBlank shot.")
                .defineInRange("suppressedFireRange", 12.0D, 0.0D, 256.0D);
        UNSUPPRESSED_FIRE_RANGE = BUILDER
                .comment("Attention radius of an unsuppressed PointBlank shot.")
                .defineInRange("unsuppressedFireRange", 96.0D, 0.0D, 256.0D);
        MELEE_ATTENTION_RANGE = BUILDER
                .comment("Attention radius when a player damages an infected.")
                .defineInRange("meleeRange", 16.0D, 0.0D, 128.0D);
        BLOCK_BREAK_ATTENTION_RANGE = BUILDER
                .comment("Attention radius when a non-instant block is broken.")
                .defineInRange("blockBreakRange", 20.0D, 0.0D, 128.0D);
        REPLACE_ZOMBIE_TACTICS_MARKERS = BUILDER
                .comment(
                        "Suppress ZombieTactics' automatic markers and let",
                        "Rotwire create markers only for configured loud events."
                )
                .define("replaceZombieTacticsMarkers", true);
        BUILDER.pop();

        BUILDER.push("knockback");
        KNOCKBACK_ENABLED = BUILDER
                .comment(
                        "Reduce incoming knockback for zombies and players.",
                        "This changes knockback strength, not damage."
                )
                .define("enabled", true);
        ZOMBIE_KNOCKBACK_RETENTION = BUILDER
                .comment(
                        "Fraction of normal knockback retained by zombies.",
                        "Applies to Zombie subclasses, including husks,",
                        "drowned, zombie villagers, and Rotwire brutes."
                )
                .defineInRange("zombieRetention", 0.15D, 0.0D, 1.0D);
        PLAYER_KNOCKBACK_RETENTION = BUILDER
                .comment(
                        "Fraction of normal knockback retained by players."
                )
                .defineInRange("playerRetention", 0.30D, 0.0D, 1.0D);
        BUILDER.pop();

        BUILDER.push("sleepSurvival");
        SLEEP_SURVIVAL_ENABLED = BUILDER
                .comment(
                        "Enable Restless Sleep for skipped nights in a",
                        "Traveler's sleeping bag and New Dawn for enduring",
                        "an entire night awake."
                )
                .define("enabled", true);
        SLEEP_EFFECT_DURATION_TICKS = BUILDER
                .comment(
                        "Duration of Restless Sleep and New Dawn.",
                        "1000 ticks is 50 seconds."
                )
                .defineInRange("effectDurationTicks", 1000, 20, 72_000);
        SLEEP_EFFECT_PULSE_INTERVAL_TICKS = BUILDER
                .comment(
                        "Ticks between hunger and thirst changes.",
                        "200 ticks is 10 seconds."
                )
                .defineInRange("pulseIntervalTicks", 200, 1, 1_200);
        SLEEP_METER_POINTS_PER_PULSE = BUILDER
                .comment(
                        "Hunger and thirst points changed per pulse.",
                        "Two internal points equal one full HUD icon."
                )
                .defineInRange("meterPointsPerPulse", 2, 1, 20);
        SLEEP_CAMPSITE_RADIUS = BUILDER
                .comment(
                        "Maximum distance from a campsite shelter center to",
                        "the lit campfire and deployed Traveler's Backpack.",
                        "SimplyTents shelters enforce a size-based minimum",
                        "so the radius always covers their full footprint."
                )
                .defineInRange("campsiteRadius", 12, 1, 32);
        SLEEP_CAMPSITE_FOOD_NUTRITION_THRESHOLD = BUILDER
                .comment(
                        "A campsite backpack must contain food totaling",
                        "strictly more nutrition than this value. The smallest",
                        "qualifying ration is consumed after the night skips."
                )
                .defineInRange(
                        "campsiteFoodNutritionThreshold",
                        5,
                        0,
                        100
                );
        BUILDER.pop();

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    private static ModConfigSpec.DoubleValue speedPenalty(
            String name,
            double defaultValue
    ) {
        return BUILDER
                .comment("Fractional movement-speed penalty; 0.10 means 10%.")
                .defineInRange(name, defaultValue, 0.0D, 0.95D);
    }

    private static ModConfigSpec.DoubleValue itemWeight(
            String name,
            double defaultValue
    ) {
        return BUILDER.defineInRange(
                name,
                defaultValue,
                0.0D,
                1_000.0D
        );
    }

    private SurvivalSystemsConfig() {
    }
}
