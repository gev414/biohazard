package io.github.gev414.biohazard.encounter;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EncounterSelectionTest {

    @Test
    void selectionIsDeterministicForOnePhysicalBuilding() {
        BuildingKey key = new BuildingKey(
                ResourceLocation.parse("minecraft:overworld"),
                12,
                -4
        );

        EncounterSelection first = EncounterSelection.select(
                123456789L,
                key,
                0.70D,
                0.20D,
                8,
                15
        );
        EncounterSelection second = EncounterSelection.select(
                123456789L,
                key,
                0.70D,
                0.20D,
                8,
                15
        );

        assertEquals(first, second);
    }

    @Test
    void killTargetUsesBothInclusiveEndpoints() {
        Set<Integer> observedTargets = new HashSet<>();
        for (int chunkX = -100; chunkX <= 100; chunkX++) {
            BuildingKey key = new BuildingKey(
                    ResourceLocation.parse("minecraft:overworld"),
                    chunkX,
                    chunkX * 31
            );
            EncounterSelection selection = EncounterSelection.select(
                    42L,
                    key,
                    1.0D,
                    0.20D,
                    8,
                    15
            );
            assertTrue(selection.targetKills() >= 8);
            assertTrue(selection.targetKills() <= 15);
            observedTargets.add(selection.targetKills());
        }

        assertTrue(observedTargets.contains(8));
        assertTrue(observedTargets.contains(15));
    }

    @Test
    void safeSelectionNeverCarriesBossOrKills() {
        BuildingKey key = new BuildingKey(
                ResourceLocation.parse("minecraft:overworld"),
                0,
                0
        );

        EncounterSelection selection = EncounterSelection.select(
                99L,
                key,
                0.0D,
                1.0D,
                8,
                15
        );

        assertFalse(selection.haunted());
        assertFalse(selection.bossSelected());
        assertEquals(0, selection.targetKills());
    }

    @Test
    void dimensionParticipatesInTheDeterministicSeed() {
        Set<EncounterSelection> overworld = selectionsForDimension(
                "minecraft:overworld"
        );
        Set<EncounterSelection> nether = selectionsForDimension(
                "minecraft:the_nether"
        );

        assertNotEquals(overworld, nether);
    }

    private static Set<EncounterSelection> selectionsForDimension(
            String dimension
    ) {
        Set<EncounterSelection> selections = new HashSet<>();
        for (int coordinate = 0; coordinate < 32; coordinate++) {
            selections.add(EncounterSelection.select(
                    7L,
                    new BuildingKey(
                            ResourceLocation.parse(dimension),
                            coordinate,
                            -coordinate
                    ),
                    1.0D,
                    0.5D,
                    coordinate,
                    coordinate
            ));
        }
        return selections;
    }
}
