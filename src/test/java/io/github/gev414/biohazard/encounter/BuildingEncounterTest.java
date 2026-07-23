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
                new EncounterSelection(true, true, 2),
                EncounterSpawnMode.WAVE
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
        assertEquals(EncounterSpawnMode.WAVE, loaded.spawnMode());
    }

    @Test
    void nonHauntedBuildingMaterializesAsSafe() {
        BuildingEncounter encounter = BuildingEncounter.materialize(
                ResourceLocation.parse("lostcities:test_building"),
                new EncounterSelection(false, false, 0),
                EncounterSpawnMode.INSTANT
        );

        assertEquals(EncounterPhase.SAFE, encounter.phase());
        assertFalse(encounter.phase().locksContainers());
        assertFalse(encounter.recordRegularDeath());
    }

    @Test
    void nonHauntedBossBuildingSkipsRegularWave() {
        BuildingEncounter encounter = BuildingEncounter.materialize(
                ResourceLocation.parse("lostcities:test_multibuilding"),
                new EncounterSelection(false, true, 0),
                EncounterSpawnMode.INSTANT
        );

        assertEquals(EncounterPhase.BOSS_PENDING, encounter.phase());
        assertTrue(encounter.phase().locksContainers());
        assertTrue(encounter.bossSelected());
        assertEquals(0, encounter.targetKills());
        assertFalse(encounter.beginInitialPopulation());
        assertFalse(encounter.recordRegularDeath());
    }

    @Test
    void bruteFinishClearsMixedEncounterWithRegularsRemaining() {
        BuildingEncounter encounter = BuildingEncounter.materialize(
                ResourceLocation.parse("lostcities:test_mixed_boss"),
                new EncounterSelection(true, true, 3),
                EncounterSpawnMode.INSTANT
        );

        assertTrue(encounter.beginBossWarning(200L));
        assertTrue(encounter.activateBoss(UUID.randomUUID()));
        assertEquals(0, encounter.regularDeaths());

        assertTrue(encounter.clear());
        assertEquals(EncounterPhase.CLEARED, encounter.phase());
        assertFalse(encounter.clear());
    }

    @Test
    void instantPopulationProgressSurvivesSerialization() {
        BuildingEncounter encounter = BuildingEncounter.materialize(
                ResourceLocation.parse("lostcities:test_instant"),
                new EncounterSelection(true, false, 3),
                EncounterSpawnMode.INSTANT
        );

        assertTrue(encounter.beginInitialPopulation());
        assertFalse(encounter.beginInitialPopulation());
        assertTrue(encounter.recordRegularSpawn());
        assertTrue(encounter.recordRegularSpawn());
        assertEquals(1, encounter.remainingInitialSpawns());

        BuildingEncounter loaded = BuildingEncounter.load(encounter.save());
        assertEquals(EncounterSpawnMode.INSTANT, loaded.spawnMode());
        assertTrue(loaded.initialPopulationAttempted());
        assertEquals(2, loaded.regularSpawns());
        assertEquals(1, loaded.remainingInitialSpawns());
    }

    @Test
    void versionOneEncounterLoadsAsWaveMode() {
        BuildingEncounter encounter = BuildingEncounter.materialize(
                ResourceLocation.parse("lostcities:test_legacy"),
                new EncounterSelection(true, false, 3),
                EncounterSpawnMode.WAVE
        );
        var legacyTag = encounter.save();
        legacyTag.remove("spawnMode");
        legacyTag.remove("regularSpawns");
        legacyTag.remove("initialPopulationAttempted");

        BuildingEncounter loaded = BuildingEncounter.load(legacyTag);
        assertEquals(EncounterSpawnMode.WAVE, loaded.spawnMode());
        assertEquals(0, loaded.regularSpawns());
        assertFalse(loaded.initialPopulationAttempted());
    }
}
