package io.github.gev414.rotwire.mob;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SurfaceZombieSpawnerTest {

    @Test
    void schedulesOneRollAtEachConfiguredInterval() {
        assertTrue(SurfaceZombieSpawner.isSpawnTick(0L, 200));
        assertTrue(SurfaceZombieSpawner.isSpawnTick(200L, 200));
        assertTrue(SurfaceZombieSpawner.isSpawnTick(1_000L, 200));
        assertFalse(SurfaceZombieSpawner.isSpawnTick(199L, 200));
        assertFalse(SurfaceZombieSpawner.isSpawnTick(201L, 200));
        assertFalse(SurfaceZombieSpawner.isSpawnTick(200L, 0));
    }

    @Test
    void usesAnExclusiveChanceBoundary() {
        assertTrue(SurfaceZombieSpawner.passesSpawnRoll(
                0.024D,
                0.025D
        ));
        assertFalse(SurfaceZombieSpawner.passesSpawnRoll(
                0.025D,
                0.025D
        ));
        assertFalse(SurfaceZombieSpawner.passesSpawnRoll(
                0.0D,
                -1.0D
        ));
        assertTrue(SurfaceZombieSpawner.passesSpawnRoll(
                0.999D,
                2.0D
        ));
    }
}
