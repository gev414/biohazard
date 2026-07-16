package io.github.gev414.biohazard.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HordeAtmosphereFogTest {

    @Test
    void inactiveDayNeverProducesFog() {
        assertEquals(0.0F, HordeAtmosphereFog.strength(
                false,
                false,
                18_000L,
                24_000,
                18_000,
                12_000
        ));
    }

    @Test
    void scheduledFogFadesFromNoonToMidnight() {
        assertEquals(0.0F, strengthAt(5_999L));
        assertEquals(0.0F, strengthAt(6_000L));
        assertEquals(0.5F, strengthAt(12_000L), 0.0001F);
        assertEquals(1.0F, strengthAt(17_999L), 0.0001F);
    }

    @Test
    void inactiveFogClearsAtEventStartAndAfterEventEnds() {
        assertEquals(0.0F, strengthAt(18_000L));
        assertEquals(0.0F, strengthAt(23_999L));
    }

    @Test
    void activeCommandHordeUsesFullFogImmediately() {
        assertEquals(1.0F, HordeAtmosphereFog.strength(
                true,
                true,
                1_000L,
                24_000,
                18_000,
                12_000
        ));
    }

    @Test
    void fogNeverExpandsAnAlreadyCloserPlane() {
        assertEquals(64.0F, HordeAtmosphereFog.blendTowardCloserPlane(
                64.0F,
                96.0F,
                1.0F
        ));
        assertEquals(96.0F, HordeAtmosphereFog.blendTowardCloserPlane(
                256.0F,
                96.0F,
                1.0F
        ));
        assertEquals(176.0F, HordeAtmosphereFog.blendTowardCloserPlane(
                256.0F,
                96.0F,
                0.5F
        ));
    }

    private static float strengthAt(long dayTime) {
        return HordeAtmosphereFog.strength(
                true,
                false,
                dayTime,
                24_000,
                18_000,
                12_000
        );
    }
}
