package io.github.gev414.biohazard.encounter;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildingEncounterTest {

    @Test
    void bossStateAndUuidSurviveSerialization() {
        BuildingEncounter encounter = BuildingEncounter.materialize(
                ResourceLocation.parse("lostcities:test_multibuilding"),
                new EncounterSelection(true, true, 2)
        );

        assertTrue(encounter.recordRegularDeath());
        assertTrue(encounter.recordRegularDeath());
        assertFalse(encounter.recordRegularDeath());
        assertTrue(encounter.beginBossWarning(500L));

        UUID bossUuid = UUID.randomUUID();
        assertTrue(encounter.activateBoss(bossUuid));

        BuildingEncounter loaded = BuildingEncounter.load(
                encounter.save()
        );
        assertEquals(EncounterPhase.BOSS_ACTIVE, loaded.phase());
        assertEquals(2, loaded.targetKills());
        assertEquals(2, loaded.regularDeaths());
        assertEquals(bossUuid, loaded.bossUuid());
        assertTrue(loaded.bossSelected());
    }

    @Test
    void nonHauntedBuildingMaterializesAsSafe() {
        BuildingEncounter encounter = BuildingEncounter.materialize(
                ResourceLocation.parse("lostcities:test_building"),
                new EncounterSelection(false, false, 0)
        );

        assertEquals(EncounterPhase.SAFE, encounter.phase());
        assertFalse(encounter.phase().locksContainers());
        assertFalse(encounter.recordRegularDeath());
    }
}
