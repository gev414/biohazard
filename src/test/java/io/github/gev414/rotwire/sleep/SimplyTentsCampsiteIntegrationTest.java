package io.github.gev414.rotwire.sleep;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimplyTentsCampsiteIntegrationTest {

    private static final String NAMESPACE = "simplytents";

    @Test
    void recognizesEverySupportedTentAnchor() {
        List<String> paths = List.of(
                "tent",
                "wall_tent",
                "roof_tent",
                "zip_tent",
                "small_tipi_tent",
                "duo_tent",
                "duo_wall_tent",
                "duo_roof_tent",
                "duo_zip_tent",
                "large_tent",
                "large_wall_tent",
                "large_roof_tent",
                "large_zip_tent",
                "tipi_tent",
                "yurt_tent"
        );

        for (String path : paths) {
            assertNotNull(footprint(path), path);
        }
    }

    @Test
    void rotatedZipTentContainsItsEntireInterior() {
        SimplyTentsCampsiteIntegration.TentFootprint footprint =
                footprint("duo_zip_tent");
        BlockPos anchor = new BlockPos(20, 70, 30);

        assertTrue(footprint.contains(
                anchor,
                anchor.offset(-3, -3, 2),
                Direction.EAST
        ));
        assertTrue(footprint.contains(
                anchor,
                anchor.offset(3, -3, -2),
                Direction.EAST
        ));
        assertFalse(footprint.contains(
                anchor,
                anchor.offset(0, -3, 3),
                Direction.EAST
        ));
    }

    @Test
    void yurtUsesItsRoundedInteriorInsteadOfSquareCorners() {
        SimplyTentsCampsiteIntegration.TentFootprint footprint =
                footprint("yurt_tent");
        BlockPos anchor = new BlockPos(0, 80, 0);

        assertTrue(footprint.contains(
                anchor,
                anchor.offset(4, -3, 2),
                Direction.NORTH
        ));
        assertTrue(footprint.contains(
                anchor,
                anchor.offset(3, -3, 3),
                Direction.NORTH
        ));
        assertTrue(footprint.contains(
                anchor,
                anchor.offset(2, -3, 4),
                Direction.NORTH
        ));
        assertFalse(footprint.contains(
                anchor,
                anchor.offset(4, -3, 4),
                Direction.NORTH
        ));
    }

    @Test
    void largeSheltersCannotUseAnUndersizedCampsiteRadius() {
        SimplyTentsCampsiteIntegration.Shelter largeTent =
                new SimplyTentsCampsiteIntegration.Shelter(
                        BlockPos.ZERO,
                        BlockPos.ZERO,
                        footprint("large_zip_tent").minimumCampsiteRadius(),
                        CampStatus.ShelterType.LARGE_TENT
                );
        SimplyTentsCampsiteIntegration.Shelter yurt =
                new SimplyTentsCampsiteIntegration.Shelter(
                        BlockPos.ZERO,
                        BlockPos.ZERO,
                        footprint("yurt_tent").minimumCampsiteRadius(),
                        CampStatus.ShelterType.YURT
                );

        assertEquals(5, largeTent.effectiveRadius(2));
        assertEquals(6, yurt.effectiveRadius(2));
        assertEquals(9, yurt.effectiveRadius(9));
    }

    @Test
    void campsiteCenterIsTheGroundBelowTheTentCore() {
        BlockPos anchor = new BlockPos(10, 64, -4);

        assertEquals(
                anchor.below(3),
                footprint("large_tent").campsiteCenter(anchor)
        );
        assertEquals(
                anchor.below(4),
                footprint("tipi_tent").campsiteCenter(anchor)
        );
    }

    @Test
    void readinessScanCanFindAPlayerAcrossTheLargestShelter() {
        assertEquals(11, CampsiteManager.readinessSearchRadius(1));
        assertEquals(37, CampsiteManager.readinessSearchRadius(32));
    }

    @Test
    void shelterTypesSurviveUnknownNetworkValues() {
        assertEquals(
                CampStatus.ShelterType.YURT,
                CampStatus.ShelterType.fromNetworkId(
                        CampStatus.ShelterType.YURT.ordinal()
                )
        );
        assertEquals(
                CampStatus.ShelterType.NONE,
                CampStatus.ShelterType.fromNetworkId(999)
        );
    }

    private static SimplyTentsCampsiteIntegration.TentFootprint footprint(
            String path
    ) {
        return SimplyTentsCampsiteIntegration.footprint(
                ResourceLocation.fromNamespaceAndPath(NAMESPACE, path)
        );
    }
}
