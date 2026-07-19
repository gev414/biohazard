package io.github.gev414.biohazard.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class RadioQuestConfig {

    private static final ModConfigSpec.Builder BUILDER =
            new ModConfigSpec.Builder();

    public static ModConfigSpec SPEC;
    public static ModConfigSpec.IntValue TRANSMITTER_RANGE;
    public static ModConfigSpec.IntValue CALIBRATION_TICKS;
    public static ModConfigSpec.IntValue SUPPLIES_DELAY_SECONDS;
    public static ModConfigSpec.IntValue AMMUNITION_DELAY_SECONDS;
    public static ModConfigSpec.IntValue MEDICAL_DELAY_SECONDS;
    public static ModConfigSpec.IntValue EQUIPMENT_DELAY_SECONDS;
    public static ModConfigSpec.IntValue FIREARM_DELAY_SECONDS;

    public static void initialize() {
        if (SPEC != null) {
            return;
        }

        BUILDER.comment(
                "Radio-gated FTB Quests and persistent courier deliveries.",
                "Times use server game time, so offline real time does not skip them."
        ).push("radioQuests");

        TRANSMITTER_RANGE = BUILDER
                .comment("Maximum block distance for accepting or turning in contracts.")
                .defineInRange("transmitterRange", 6, 1, 32);
        CALIBRATION_TICKS = BUILDER
                .comment("Connection delay after placing or relocating a transmitter.")
                .defineInRange("calibrationTicks", 1_200, 0, 72_000);

        BUILDER.push("deliverySeconds");
        SUPPLIES_DELAY_SECONDS = delay("supplies", 120);
        AMMUNITION_DELAY_SECONDS = delay("ammunition", 120);
        MEDICAL_DELAY_SECONDS = delay("medical", 180);
        EQUIPMENT_DELAY_SECONDS = delay("equipment", 240);
        FIREARM_DELAY_SECONDS = delay("firearm", 300);
        BUILDER.pop();

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    private static ModConfigSpec.IntValue delay(String name, int seconds) {
        return BUILDER
                .comment("Courier delay for " + name + " deliveries.")
                .defineInRange(name, seconds, 0, 86_400);
    }

    private RadioQuestConfig() {
    }
}
