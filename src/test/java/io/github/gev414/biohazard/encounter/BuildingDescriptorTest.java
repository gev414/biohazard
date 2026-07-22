package io.github.gev414.biohazard.encounter;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildingDescriptorTest {

    private static final BuildingDescriptor BUILDING =
            new BuildingDescriptor(
                    new BuildingKey(
                            ResourceLocation.parse("minecraft:overworld"),
                            2,
                            -1
                    ),
                    ResourceLocation.parse("lostcities:test_building"),
                    1,
                    1,
                    60,
                    84
            );

    @Test
    void positionInsideBuildingHasZeroDistance() {
        assertEquals(
                0.0D,
                BUILDING.distanceToSqr(new BlockPos(40, 70, -8))
        );
    }

    @Test
    void distanceUsesBuildingBoundsRatherThanChunkCenter() {
        assertEquals(
                16.5D * 16.5D,
                BUILDING.distanceToSqr(new BlockPos(64, 70, -8))
        );
    }

    @Test
    void interiorFloorsExcludeTheRoofBand() {
        assertEquals(3, BUILDING.interiorFloorCount());
        assertEquals(60, BUILDING.interiorFloorMinY(0));
        assertEquals(78, BUILDING.interiorFloorMaxYExclusive(2));
        assertTrue(BUILDING.containsInterior(new BlockPos(40, 73, -8)));
        assertFalse(BUILDING.containsInterior(new BlockPos(40, 79, -8)));
    }

    @Test
    void multiChunkClassificationUsesTheWholeFootprint() {
        assertFalse(BUILDING.isMultiChunk());

        BuildingDescriptor largeBuilding = new BuildingDescriptor(
                BUILDING.key(),
                BUILDING.buildingId(),
                2,
                1,
                BUILDING.minY(),
                BUILDING.maxYExclusive()
        );
        assertTrue(largeBuilding.isMultiChunk());
    }
}
