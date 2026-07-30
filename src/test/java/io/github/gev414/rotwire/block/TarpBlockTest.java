package io.github.gev414.rotwire.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class TarpBlockTest {

    @Test
    void footprintContainsSixUniqueRoofCellsForEveryFacing() {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            Set<BlockPos> offsets = Arrays.stream(TarpBlock.Part.values())
                    .map(part -> part.offset(facing))
                    .collect(Collectors.toSet());

            assertEquals(6, offsets.size());
            assertEquals(3, offsets.stream()
                    .map(position -> facing.getAxis() == Direction.Axis.Z
                            ? position.getZ()
                            : position.getX())
                    .collect(Collectors.toSet())
                    .size());
            assertEquals(2, offsets.stream()
                    .map(position -> facing.getAxis() == Direction.Axis.Z
                            ? position.getX()
                            : position.getZ())
                    .collect(Collectors.toSet())
                    .size());
        }
    }

    @Test
    void everyPartResolvesBackToTheSameAnchor() {
        BlockPos anchor = new BlockPos(13, 72, -9);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            for (TarpBlock.Part part : TarpBlock.Part.values()) {
                BlockPos partPosition = TarpBlock.positionFor(
                        anchor,
                        facing,
                        part
                );
                BlockPos offset = part.offset(facing);

                assertEquals(
                        anchor,
                        partPosition.offset(
                                -offset.getX(),
                                -offset.getY(),
                                -offset.getZ()
                        )
                );
            }
        }
    }
}
