package io.github.gev414.rotwire.mob;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MobSpawnRestrictionsTest {

    @Test
    void selectsOnlyEnabledSkeletonsAndCreepers() {
        assertTrue(MobSpawnRestrictions.isRestricted(
                EntityType.SKELETON,
                true,
                true
        ));
        assertTrue(MobSpawnRestrictions.isRestricted(
                EntityType.CREEPER,
                true,
                true
        ));
        assertTrue(MobSpawnRestrictions.isRestricted(
                EntityType.STRAY,
                true,
                true
        ));
        assertTrue(MobSpawnRestrictions.isRestricted(
                EntityType.BOGGED,
                true,
                true
        ));
        assertTrue(MobSpawnRestrictions.isRestricted(
                EntityType.WITHER_SKELETON,
                true,
                true
        ));
        assertFalse(MobSpawnRestrictions.isRestricted(
                EntityType.ZOMBIE,
                true,
                true
        ));
        assertFalse(MobSpawnRestrictions.isRestricted(
                EntityType.SKELETON,
                false,
                true
        ));
        assertFalse(MobSpawnRestrictions.isRestricted(
                EntityType.CREEPER,
                true,
                false
        ));
    }

    @Test
    void calculatesInclusiveUndergroundLimitFromSeaLevel() {
        assertEquals(
                47,
                MobSpawnRestrictions.maximumNaturalSpawnY(63, 16)
        );
        assertEquals(
                63,
                MobSpawnRestrictions.maximumNaturalSpawnY(63, 0)
        );
        assertEquals(
                63,
                MobSpawnRestrictions.maximumNaturalSpawnY(63, -5)
        );
    }

    @Test
    void rejectsOnlyNaturalSpawnsAboveTheLimit() {
        assertTrue(MobSpawnRestrictions.rejectsSpawn(
                MobSpawnType.NATURAL,
                48,
                47
        ));
        assertFalse(MobSpawnRestrictions.rejectsSpawn(
                MobSpawnType.NATURAL,
                47,
                47
        ));
        assertFalse(MobSpawnRestrictions.rejectsSpawn(
                MobSpawnType.SPAWNER,
                80,
                47
        ));
        assertFalse(MobSpawnRestrictions.rejectsSpawn(
                MobSpawnType.EVENT,
                80,
                47
        ));
        assertFalse(MobSpawnRestrictions.rejectsSpawn(
                MobSpawnType.COMMAND,
                80,
                47
        ));
        assertFalse(MobSpawnRestrictions.rejectsSpawn(
                MobSpawnType.SPAWN_EGG,
                80,
                47
        ));
    }
}
