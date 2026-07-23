package io.github.gev414.biohazard.city;

import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CitySurveyTest {

    @Test
    void surveyFindsCardinallyConnectedCityFootprint() {
        Set<Long> city = Set.of(
                ChunkPos.asLong(0, 0),
                ChunkPos.asLong(1, 0),
                ChunkPos.asLong(1, 1),
                ChunkPos.asLong(2, 1)
        );
        CitySurvey survey = new CitySurvey(0, 0);

        finish(survey, city, false);

        assertTrue(survey.complete());
        assertFalse(survey.capped());
        assertEquals(city, survey.cityChunks());
    }

    @Test
    void diagonalConnectivityIsExplicit() {
        Set<Long> city = Set.of(
                ChunkPos.asLong(0, 0),
                ChunkPos.asLong(1, 1)
        );

        CitySurvey cardinal = new CitySurvey(0, 0);
        finish(cardinal, city, false);
        assertEquals(
                Set.of(ChunkPos.asLong(0, 0)),
                cardinal.cityChunks()
        );

        CitySurvey diagonal = new CitySurvey(0, 0);
        finish(diagonal, city, true);
        assertEquals(city, diagonal.cityChunks());
    }

    @Test
    void unboundedCityUsesTheHardCap() {
        CitySurvey survey = new CitySurvey(4, -3);

        while (!survey.complete()) {
            survey.advance(
                    (chunkX, chunkZ) -> true,
                    2,
                    5,
                    false
            );
        }

        assertTrue(survey.capped());
        assertEquals(5, survey.scannedChunks());
        assertEquals(5, survey.cityChunks().size());
    }

    private static void finish(
            CitySurvey survey,
            Set<Long> city,
            boolean diagonalConnectivity
    ) {
        while (!survey.complete()) {
            survey.advance(
                    (chunkX, chunkZ) -> city.contains(
                            ChunkPos.asLong(chunkX, chunkZ)
                    ),
                    3,
                    100,
                    diagonalConnectivity
            );
        }
    }
}
