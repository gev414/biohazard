package io.github.gev414.biohazard.city;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CityDangerProgressionTest {

    @Test
    void dangerIncreasesEveryFiveUniqueClears() {
        assertEquals(0, CityDangerProgression.level(0, 5, 12));
        assertEquals(0, CityDangerProgression.level(4, 5, 12));
        assertEquals(1, CityDangerProgression.level(5, 5, 12));
        assertEquals(2, CityDangerProgression.level(14, 5, 12));
    }

    @Test
    void dangerStopsAtConfiguredMaximum() {
        assertEquals(3, CityDangerProgression.level(100, 5, 3));
        assertEquals(
                0,
                CityDangerProgression.remainingUntilNextLevel(
                        100,
                        5,
                        3
                )
        );
    }

    @Test
    void remainingClearsTracksNextEscalation() {
        assertEquals(
                5,
                CityDangerProgression.remainingUntilNextLevel(
                        0,
                        5,
                        12
                )
        );
        assertEquals(
                1,
                CityDangerProgression.remainingUntilNextLevel(
                        4,
                        5,
                        12
                )
        );
        assertEquals(
                5,
                CityDangerProgression.remainingUntilNextLevel(
                        5,
                        5,
                        12
                )
        );
    }
}
