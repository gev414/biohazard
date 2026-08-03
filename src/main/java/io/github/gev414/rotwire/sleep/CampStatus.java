package io.github.gev414.rotwire.sleep;

import net.minecraft.core.BlockPos;

public record CampStatus(
        ShelterType shelter,
        BlockPos center,
        int radius,
        boolean sleepingBagPresent,
        boolean litCampfirePresent,
        boolean containerPresent,
        boolean rationReady,
        int availableNutrition
) {

    public boolean sheltered() {
        return shelter != ShelterType.NONE;
    }

    public boolean active() {
        return sheltered()
                && sleepingBagPresent
                && litCampfirePresent
                && containerPresent
                && rationReady;
    }

    public static CampStatus inactive(BlockPos center, int radius) {
        return new CampStatus(
                ShelterType.NONE,
                center.immutable(),
                radius,
                false,
                false,
                false,
                false,
                0
        );
    }

    public enum ShelterType {
        NONE("screen.rotwire.camp_radio.shelter.none"),
        TARP("screen.rotwire.camp_radio.shelter.tarp"),
        COMPACT_TENT("screen.rotwire.camp_radio.shelter.compact"),
        SMALL_TIPI("screen.rotwire.camp_radio.shelter.small_tipi"),
        DUO_TENT("screen.rotwire.camp_radio.shelter.duo"),
        LARGE_TENT("screen.rotwire.camp_radio.shelter.large"),
        TIPI("screen.rotwire.camp_radio.shelter.tipi"),
        YURT("screen.rotwire.camp_radio.shelter.yurt");

        private final String translationKey;

        ShelterType(String translationKey) {
            this.translationKey = translationKey;
        }

        public String translationKey() {
            return translationKey;
        }

        public static ShelterType fromNetworkId(int id) {
            ShelterType[] values = values();
            return id >= 0 && id < values.length
                    ? values[id]
                    : NONE;
        }
    }
}
