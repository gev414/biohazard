package io.github.gev414.biohazard.config;

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
    public static ModConfigSpec.DoubleValue DEFAULT_STACK_WEIGHT;
    public static ModConfigSpec.DoubleValue LIGHT_STACK_WEIGHT;
    public static ModConfigSpec.DoubleValue HEAVY_STACK_WEIGHT;
    public static ModConfigSpec.DoubleValue VERY_HEAVY_STACK_WEIGHT;
    public static ModConfigSpec.DoubleValue ARMOR_STACK_WEIGHT;
    public static ModConfigSpec.DoubleValue FIREARM_STACK_WEIGHT;
    public static ModConfigSpec.DoubleValue BLOCK_STACK_WEIGHT;
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

    public static void initialize() {
        if (SPEC != null) {
            return;
        }

        BUILDER.comment(
                "Encumbrance, stealth awareness, and noise-driven attention."
        ).push("survivalSystems");

        ENABLED = BUILDER
                .comment("Enable encumbrance, stealth, and attention systems.")
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
                "A stack's count scales its category weight from 25% for a",
                "nearly empty stack to 100% for a full stack."
        ).push("weights");
        DEFAULT_STACK_WEIGHT = stackWeight(
                "defaultStack",
                1.0D
        );
        LIGHT_STACK_WEIGHT = stackWeight(
                "lightStack",
                0.5D
        );
        HEAVY_STACK_WEIGHT = stackWeight(
                "heavyStack",
                2.0D
        );
        VERY_HEAVY_STACK_WEIGHT = stackWeight(
                "veryHeavyStack",
                4.0D
        );
        ARMOR_STACK_WEIGHT = stackWeight(
                "armorStack",
                2.0D
        );
        FIREARM_STACK_WEIGHT = stackWeight(
                "firearmStack",
                2.5D
        );
        BLOCK_STACK_WEIGHT = stackWeight(
                "blockStack",
                1.5D
        );
        BACKPACK_BASE_WEIGHT = stackWeight(
                "backpackBase",
                2.0D
        );
        BACKPACK_FLUID_WEIGHT_PER_BUCKET = stackWeight(
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
                        "Biohazard create markers only for configured loud events."
                )
                .define("replaceZombieTacticsMarkers", true);
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

    private static ModConfigSpec.DoubleValue stackWeight(
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
