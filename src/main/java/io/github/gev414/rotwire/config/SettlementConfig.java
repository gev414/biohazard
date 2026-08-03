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
    public static ModConfigSpec.IntValue PISTOLMAN_CALL_RATION_REQUIREMENT;
    public static ModConfigSpec.IntValue PISTOLMAN_CALL_AMMUNITION_REQUIREMENT;
    public static ModConfigSpec.IntValue MAX_PISTOLMEN;
    public static ModConfigSpec.IntValue SHOTGUNNER_CALL_RATION_REQUIREMENT;
    public static ModConfigSpec.IntValue SHOTGUNNER_CALL_AMMUNITION_REQUIREMENT;
    public static ModConfigSpec.IntValue MAX_SHOTGUNNERS;
    public static ModConfigSpec.DoubleValue SURVIVOR_CAMP_RETREAT_DISTANCE;
    public static ModConfigSpec.IntValue SURVIVOR_HOSTILE_AWARENESS_RADIUS;
    public static ModConfigSpec.IntValue SURVIVOR_THREAT_SCAN_INTERVAL_TICKS;
    public static ModConfigSpec.IntValue SURVIVOR_CONTACT_MEMORY_TICKS;
    public static ModConfigSpec.IntValue SURVIVOR_REPOSITION_INTERVAL_TICKS;
    public static ModConfigSpec.IntValue SURVIVOR_REPOSITION_ATTEMPTS;
    public static ModConfigSpec.IntValue SURVIVOR_REPOSITION_RADIUS;
    public static ModConfigSpec.IntValue SURVIVOR_PATHS_PER_TICK;
    public static ModConfigSpec.IntValue SURVIVOR_RETURN_PATH_RETRY_TICKS;
    public static ModConfigSpec.DoubleValue RIFLEMAN_MINIMUM_ENGAGEMENT_DISTANCE;
    public static ModConfigSpec.DoubleValue PISTOLMAN_MINIMUM_ENGAGEMENT_DISTANCE;
    public static ModConfigSpec.DoubleValue SHOTGUNNER_MINIMUM_ENGAGEMENT_DISTANCE;
    public static ModConfigSpec.BooleanValue SIEGES_ENABLED;
    public static ModConfigSpec.IntValue SIEGE_MINIMUM_POPULATION;
    public static ModConfigSpec.IntValue SIEGE_INTERVAL_TICKS;
    public static ModConfigSpec.IntValue SIEGE_WARNING_TICKS;
    public static ModConfigSpec.IntValue SIEGE_DURATION_TICKS;
    public static ModConfigSpec.IntValue SIEGE_RECOVERY_TICKS;
    public static ModConfigSpec.IntValue SIEGE_SPAWN_INTERVAL_TICKS;
    public static ModConfigSpec.IntValue SIEGE_ZOMBIES_PER_WAVE;
    public static ModConfigSpec.IntValue SIEGE_NEARBY_CAP;
    public static ModConfigSpec.IntValue SIEGE_MINIMUM_SPAWN_DISTANCE;
    public static ModConfigSpec.IntValue SIEGE_MAXIMUM_SPAWN_DISTANCE;
    public static ModConfigSpec.IntValue SIEGE_SPAWN_POSITION_ATTEMPTS;
    public static ModConfigSpec.IntValue SIEGE_FRAGILE_BREACH_TICKS;
    public static ModConfigSpec.IntValue SIEGE_STANDARD_BREACH_TICKS;
    public static ModConfigSpec.IntValue SIEGE_REINFORCED_BREACH_TICKS;
    public static ModConfigSpec.BooleanValue SIEGE_STRUCTURAL_BREACH_ENABLED;
    public static ModConfigSpec.IntValue SIEGE_STRUCTURAL_FAILURE_THRESHOLD;
    public static ModConfigSpec.IntValue SIEGE_STRUCTURAL_INDIVIDUAL_FAILURE_THRESHOLD;
    public static ModConfigSpec.IntValue SIEGE_STRUCTURAL_BASE_TICKS;
    public static ModConfigSpec.IntValue SIEGE_STRUCTURAL_HARDNESS_TICKS;
    public static ModConfigSpec.IntValue SIEGE_STRUCTURAL_MAX_TICKS;
    public static ModConfigSpec.IntValue SIEGE_STRUCTURAL_MAX_DEPTH;
    public static ModConfigSpec.IntValue SIEGE_STRUCTURAL_MAX_CAMP_DISTANCE;
    public static ModConfigSpec.IntValue SIEGE_STRUCTURAL_MAX_CONTRIBUTORS;
    public static ModConfigSpec.IntValue
            SIEGE_STRUCTURAL_PROGRESS_DECAY_DELAY_TICKS;
    public static ModConfigSpec.IntValue SIEGE_PLAYER_ACTIVATION_DISTANCE;
    public static ModConfigSpec.IntValue SIEGE_RESOURCE_DRAIN_INTERVAL_TICKS;
    public static ModConfigSpec.IntValue SIEGE_RESOURCE_DRAIN_RATIONS;

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
        SURVIVOR_THREAT_SCAN_INTERVAL_TICKS = BUILDER
                .comment(
                        "Ticks between close-range hostile searches while a "
                                + "survivor has no target. Individual "
                                + "survivors begin on a random offset."
                )
                .defineInRange("threatScanIntervalTicks", 12, 4, 100);
        SURVIVOR_CONTACT_MEMORY_TICKS = BUILDER
                .comment(
                        "Ticks an armed survivor remembers a recently known "
                                + "hostile after it leaves close awareness "
                                + "and line of sight."
                )
                .defineInRange("contactMemoryTicks", 120, 20, 600);
        SURVIVOR_REPOSITION_INTERVAL_TICKS = BUILDER
                .comment(
                        "Minimum ticks between attempts to find a nearby "
                                + "firing position after line of sight is lost."
                )
                .defineInRange("repositionIntervalTicks", 30, 10, 200);
        SURVIVOR_REPOSITION_ATTEMPTS = BUILDER
                .comment(
                        "Maximum inexpensive candidate positions sampled per "
                                + "reposition attempt. Only the best candidate "
                                + "receives a path calculation."
                )
                .defineInRange("repositionAttempts", 6, 1, 12);
        SURVIVOR_REPOSITION_RADIUS = BUILDER
                .comment(
                        "Maximum radius in blocks for a tactical reposition."
                )
                .defineInRange("repositionRadius", 8, 3, 16);
        SURVIVOR_PATHS_PER_TICK = BUILDER
                .comment(
                        "Maximum survivor paths calculated in one level tick.",
                        "Return, combat reposition, and roaming share this budget."
                )
                .defineInRange("pathCalculationsPerTick", 1, 1, 8);
        SURVIVOR_RETURN_PATH_RETRY_TICKS = BUILDER
                .comment(
                        "Base delay after an unsuccessful camp-return path.",
                        "Approach points rotate between attempts."
                )
                .defineInRange("returnPathRetryTicks", 40, 5, 1_200);
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
        RIFLEMAN_MINIMUM_ENGAGEMENT_DISTANCE = BUILDER
                .comment(
                        "Preferred minimum distance in blocks a rifleman "
                                + "tries to maintain from its target."
                )
                .defineInRange("minimumEngagementDistance", 8.0D, 3.0D, 32.0D);
        BUILDER.pop();
        BUILDER.push("pistolman");
        PISTOLMAN_CALL_RATION_REQUIREMENT = BUILDER
                .comment("Stored nutrition required to call an M1911A1 pistolman.")
                .defineInRange("callRationRequirement", 100, 0, 100_000);
        PISTOLMAN_CALL_AMMUNITION_REQUIREMENT = BUILDER
                .comment(".45 ACP rounds required in camp stores to call a pistolman.")
                .defineInRange("callAmmunitionRequirement", 7, 7, 100_000);
        MAX_PISTOLMEN = BUILDER
                .comment("Maximum M1911A1 pistolmen per settlement.")
                .defineInRange("maximumPerSettlement", 1, 1, 1_024);
        PISTOLMAN_MINIMUM_ENGAGEMENT_DISTANCE = BUILDER
                .comment(
                        "Preferred minimum distance in blocks a pistolman "
                                + "tries to maintain from its target."
                )
                .defineInRange("minimumEngagementDistance", 6.0D, 3.0D, 24.0D);
        BUILDER.pop();
        BUILDER.push("shotgunner");
        SHOTGUNNER_CALL_RATION_REQUIREMENT = BUILDER
                .comment("Stored nutrition required to call an M870 shotgunner.")
                .defineInRange("callRationRequirement", 100, 0, 100_000);
        SHOTGUNNER_CALL_AMMUNITION_REQUIREMENT = BUILDER
                .comment("12 gauge shells required in camp stores to call a shotgunner.")
                .defineInRange("callAmmunitionRequirement", 6, 6, 100_000);
        MAX_SHOTGUNNERS = BUILDER
                .comment("Maximum M870 shotgunners per settlement.")
                .defineInRange("maximumPerSettlement", 1, 1, 1_024);
        SHOTGUNNER_MINIMUM_ENGAGEMENT_DISTANCE = BUILDER
                .comment(
                        "Preferred minimum distance in blocks a shotgunner "
                                + "tries to maintain from its target."
                )
                .defineInRange("minimumEngagementDistance", 5.0D, 3.0D, 20.0D);
        BUILDER.pop();
        BUILDER.push("sieges");
        SIEGES_ENABLED = BUILDER
                .comment("Allow operational settlements to receive zombie sieges.")
                .define("enabled", true);
        SIEGE_MINIMUM_POPULATION = BUILDER
                .comment("Living survivors required for an operational settlement.")
                .defineInRange("minimumPopulation", 3, 1, 1_024);
        SIEGE_INTERVAL_TICKS = BUILDER
                .comment("Ticks between the end of recovery and the next siege warning.")
                .defineInRange("intervalTicks", 72_000, 1_200, 2_592_000);
        SIEGE_WARNING_TICKS = BUILDER
                .comment("Warning duration before an announced siege begins.")
                .defineInRange("warningTicks", 12_000, 200, 72_000);
        SIEGE_DURATION_TICKS = BUILDER
                .comment("How long an active siege sends new infected at the camp.")
                .defineInRange("durationTicks", 6_000, 200, 72_000);
        SIEGE_RECOVERY_TICKS = BUILDER
                .comment("Recovery duration after the assault before normal scheduling resumes.")
                .defineInRange("recoveryTicks", 6_000, 0, 72_000);
        SIEGE_SPAWN_INTERVAL_TICKS = BUILDER
                .comment("Ticks between attempted siege-zombie waves.")
                .defineInRange("spawnIntervalTicks", 600, 20, 1_200);
        SIEGE_ZOMBIES_PER_WAVE = BUILDER
                .comment("Siege zombies attempted together at each spawn interval.")
                .defineInRange("zombiesPerWave", 24, 1, 32);
        SIEGE_NEARBY_CAP = BUILDER
                .comment("Maximum active siege zombies around one primary camp.")
                .defineInRange("nearbyCap", 72, 1, 128);
        SIEGE_MINIMUM_SPAWN_DISTANCE = BUILDER
                .comment("Minimum horizontal siege spawn distance from the main tent.")
                .defineInRange("minimumSpawnDistance", 90, 32, 128);
        SIEGE_MAXIMUM_SPAWN_DISTANCE = BUILDER
                .comment("Maximum horizontal siege spawn distance from the main tent.")
                .defineInRange("maximumSpawnDistance", 100, 32, 160);
        SIEGE_SPAWN_POSITION_ATTEMPTS = BUILDER
                .comment("Outdoor positions checked for each siege spawn attempt.")
                .defineInRange("spawnPositionAttempts", 16, 1, 128);
        SIEGE_FRAGILE_BREACH_TICKS = BUILDER
                .comment("Ticks a siege zombie needs to break a fragile tagged obstacle.")
                .defineInRange("fragileBreachTicks", 40, 1, 1_200);
        SIEGE_STANDARD_BREACH_TICKS = BUILDER
                .comment("Ticks a siege zombie needs to break a standard tagged obstacle.")
                .defineInRange("standardBreachTicks", 100, 1, 1_200);
        SIEGE_REINFORCED_BREACH_TICKS = BUILDER
                .comment("Ticks a siege zombie needs to break a reinforced tagged obstacle.")
                .defineInRange("reinforcedBreachTicks", 200, 1, 1_200);
        SIEGE_STRUCTURAL_BREACH_ENABLED = BUILDER
                .comment(
                        "Allow a pursued survivor/player group with repeated route "
                                + "failures to open one guarded structural lane."
                )
                .define("structuralBreachingEnabled", true);
        SIEGE_STRUCTURAL_FAILURE_THRESHOLD = BUILDER
                .comment(
                        "Shared failed routes required before a siege group "
                                + "may breach ordinary solid blocks."
                )
                .defineInRange("structuralFailureThreshold", 3, 1, 100);
        SIEGE_STRUCTURAL_INDIVIDUAL_FAILURE_THRESHOLD = BUILDER
                .comment(
                        "Failed routes one infected needs before it may start "
                                + "a guarded structural breach without group consensus."
                )
                .defineInRange(
                        "structuralIndividualFailureThreshold",
                        4,
                        2,
                        100
                );
        SIEGE_STRUCTURAL_BASE_TICKS = BUILDER
                .comment("Base ticks required for one structural block.")
                .defineInRange("structuralBaseTicks", 200, 20, 2_400);
        SIEGE_STRUCTURAL_HARDNESS_TICKS = BUILDER
                .comment(
                        "Additional structural breach ticks per point of "
                                + "block destroy speed."
                )
                .defineInRange("structuralHardnessTicks", 200, 0, 2_400);
        SIEGE_STRUCTURAL_MAX_TICKS = BUILDER
                .comment("Maximum ticks required for one structural block.")
                .defineInRange("structuralMaximumTicks", 1_200, 20, 12_000);
        SIEGE_STRUCTURAL_MAX_DEPTH = BUILDER
                .comment(
                        "Maximum horizontal depth of the shared one-by-two "
                                + "breach lane."
                )
                .defineInRange("structuralMaximumDepth", 4, 1, 16);
        SIEGE_STRUCTURAL_MAX_CAMP_DISTANCE = BUILDER
                .comment(
                        "Maximum horizontal distance from the pursuit objective at "
                                + "which structural breaching is permitted."
                )
                .defineInRange("structuralMaximumCampDistance", 48, 8, 128);
        SIEGE_STRUCTURAL_MAX_CONTRIBUTORS = BUILDER
                .comment(
                        "Maximum zombies that may add progress to the shared "
                                + "breach during one tick."
                )
                .defineInRange("structuralMaximumContributors", 2, 1, 4);
        SIEGE_STRUCTURAL_PROGRESS_DECAY_DELAY_TICKS = BUILDER
                .comment(
                        "Ticks that shared structural breach progress is "
                                + "retained without a contributing zombie."
                )
                .defineInRange(
                        "structuralProgressDecayDelayTicks",
                        200,
                        0,
                        2_400
                );
        SIEGE_PLAYER_ACTIVATION_DISTANCE = BUILDER
                .comment("A player must be this close to keep a settlement siege active.")
                .defineInRange("playerActivationDistance", 128, 32, 256);
        SIEGE_RESOURCE_DRAIN_INTERVAL_TICKS = BUILDER
                .comment("Ticks between raided ration withdrawals during an active siege.")
                .defineInRange("resourceDrainIntervalTicks", 400, 20, 7_200);
        SIEGE_RESOURCE_DRAIN_RATIONS = BUILDER
                .comment("Nutrition removed from active camp containers on each raid withdrawal.")
                .defineInRange("resourceDrainRations", 4, 0, 1_000);
        BUILDER.pop();
        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    private SettlementConfig() {
    }
}
