package io.github.gev414.rotwire.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Shared settlement resource rules. Population systems use these values before
 * their own recruitment and role rules are introduced.
 */
public final class SettlementConfig {

    private static final ModConfigSpec.Builder BUILDER =
            new ModConfigSpec.Builder();

    public static ModConfigSpec SPEC;
    public static ModConfigSpec.IntValue RATIONS_PER_SETTLER_PER_DAY;
    public static ModConfigSpec.IntValue STOCKPILE_SCAN_INTERVAL_TICKS;
    public static ModConfigSpec.IntValue CIVILIAN_CALL_RATION_REQUIREMENT;
    public static ModConfigSpec.IntValue MAX_CIVILIAN_SURVIVORS;
    public static ModConfigSpec.IntValue CIVILIAN_CITY_ROAM_RADIUS;
    public static ModConfigSpec.IntValue CIVILIAN_HOSTILE_RETREAT_RADIUS;
    public static ModConfigSpec.IntValue RIFLEMAN_CALL_RATION_REQUIREMENT;
    public static ModConfigSpec.IntValue RIFLEMAN_CALL_AMMUNITION_REQUIREMENT;
    public static ModConfigSpec.IntValue MAX_RIFLEMEN;
    public static ModConfigSpec.IntValue RIFLEMAN_PRECISE_RANGE;
    public static ModConfigSpec.DoubleValue RIFLEMAN_LONG_RANGE_MISS_CHANCE;
    public static ModConfigSpec.DoubleValue RIFLEMAN_HEADSHOT_CHANCE;
    public static ModConfigSpec.DoubleValue SURVIVOR_CAMP_RETREAT_DISTANCE;
    public static ModConfigSpec.IntValue SURVIVOR_HOSTILE_AWARENESS_RADIUS;

    public static void initialize() {
        if (SPEC != null) {
            return;
        }

        BUILDER.comment(
                "Persistent city settlement hub and stockpile rules."
        ).push("settlements");
        RATIONS_PER_SETTLER_PER_DAY = BUILDER
                .comment(
                        "Food nutrition points consumed by each civilian or "
                                + "guard "
                                + "at every Minecraft-day boundary."
                )
                .defineInRange("rationsPerSettlerPerDay", 1, 0, 1_000);
        STOCKPILE_SCAN_INTERVAL_TICKS = BUILDER
                .comment(
                        "How often active campsite containers are scanned "
                                + "for the shared stockpile."
                )
                .defineInRange("stockpileScanIntervalTicks", 100, 20, 1_200);
        BUILDER.push("civilianSurvivor");
        CIVILIAN_CALL_RATION_REQUIREMENT = BUILDER
                .comment(
                        "Stored nutrition required before a primary Camp Hub "
                                + "can call a civilian survivor. The food is "
                                + "not spent immediately; it proves the camp "
                                + "can support ongoing upkeep."
                )
                .defineInRange("callRationRequirement", 100, 0, 100_000);
        MAX_CIVILIAN_SURVIVORS = BUILDER
                .comment(
                        "Temporary vertical-slice limit for civilian "
                                + "survivors per settlement."
                )
                .defineInRange("maximumPerSettlement", 1, 1, 1_024);
        CIVILIAN_CITY_ROAM_RADIUS = BUILDER
                .comment(
                        "Maximum horizontal distance in blocks a calm "
                                + "civilian may roam from its assigned camp."
                )
                .defineInRange("cityRoamRadius", 64, 8, 512);
        CIVILIAN_HOSTILE_RETREAT_RADIUS = BUILDER
                .comment(
                        "Nearby hostile radius in blocks that sends a "
                                + "civilian back to camp."
                )
                .defineInRange("hostileRetreatRadius", 18, 4, 64);
        BUILDER.pop();
        BUILDER.push("survivorAwareness");
        SURVIVOR_CAMP_RETREAT_DISTANCE = BUILDER
                .comment(
                        "Distance in blocks from the main shelter center at "
                                + "which an emergency retreat is complete."
                )
                .defineInRange("campRetreatDistance", 3.5D, 2.0D, 8.0D);
        SURVIVOR_HOSTILE_AWARENESS_RADIUS = BUILDER
                .comment(
                        "Nearby hostile awareness radius. This ignores line "
                                + "of sight so threats cannot approach from "
                                + "behind, but is deliberately limited to "
                                + "10-20 blocks."
                )
                .defineInRange("hostileAwarenessRadius", 16, 10, 20);
        BUILDER.pop();
        BUILDER.push("rifleman");
        RIFLEMAN_CALL_RATION_REQUIREMENT = BUILDER
                .comment(
                        "Stored nutrition required before a primary Camp Hub "
                                + "can call a rifleman. This is a support "
                                + "requirement, not an immediate food cost."
                )
                .defineInRange("callRationRequirement", 100, 0, 100_000);
        RIFLEMAN_CALL_AMMUNITION_REQUIREMENT = BUILDER
                .comment(
                        "7.62x51 PointBlank rounds required in the camp "
                                + "stockpile when calling a rifleman. Five "
                                + "rounds are loaded into the Mosin."
                )
                .defineInRange("callAmmunitionRequirement", 10, 5, 100_000);
        MAX_RIFLEMEN = BUILDER
                .comment(
                        "Temporary vertical-slice limit for Mosin riflemen "
                                + "per settlement."
                )
                .defineInRange("maximumPerSettlement", 1, 1, 1_024);
        RIFLEMAN_PRECISE_RANGE = BUILDER
                .comment(
                        "Range in blocks where a rifleman has near-perfect "
                                + "aim with the Mosin."
                )
                .defineInRange("preciseRange", 40, 4, 256);
        RIFLEMAN_LONG_RANGE_MISS_CHANCE = BUILDER
                .comment(
                        "Chance from 0.0 to 1.0 for an intentional miss "
                                + "beyond the precise rifleman range."
                )
                .defineInRange("longRangeMissChance", 0.10D, 0.0D, 1.0D);
        RIFLEMAN_HEADSHOT_CHANCE = BUILDER
                .comment(
                        "Base chance from 0.0 to 1.0 that a rifleman aims "
                                + "for a headshot. Future survivor levels "
                                + "can modify this value."
                )
                .defineInRange("headshotChance", 0.15D, 0.0D, 1.0D);
        BUILDER.pop();
        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    private SettlementConfig() {
    }
}
