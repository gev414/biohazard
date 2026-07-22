package io.github.gev414.biohazard.encounter;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;

import java.util.Objects;

public record BuildingDescriptor(
        BuildingKey key,
        ResourceLocation buildingId,
        int widthChunks,
        int depthChunks,
        int minY,
        int maxYExclusive
) {

    public static final int FLOOR_HEIGHT = 6;

    public BuildingDescriptor {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(buildingId, "buildingId");
        if (widthChunks < 1 || depthChunks < 1) {
            throw new IllegalArgumentException(
                    "Building dimensions must be positive"
            );
        }
        if (minY >= maxYExclusive) {
            throw new IllegalArgumentException(
                    "Building vertical bounds must not be empty"
            );
        }
    }

    public AABB bounds() {
        return new AABB(
                key.rootChunkX() * 16.0D,
                minY,
                key.rootChunkZ() * 16.0D,
                (key.rootChunkX() + widthChunks) * 16.0D,
                maxYExclusive,
                (key.rootChunkZ() + depthChunks) * 16.0D
        );
    }

    public boolean contains(BlockPos pos) {
        int minX = key.rootChunkX() * 16;
        int minZ = key.rootChunkZ() * 16;
        int maxXExclusive =
                (key.rootChunkX() + widthChunks) * 16;
        int maxZExclusive =
                (key.rootChunkZ() + depthChunks) * 16;

        return pos.getX() >= minX
                && pos.getX() < maxXExclusive
                && pos.getY() >= minY
                && pos.getY() < maxYExclusive
                && pos.getZ() >= minZ
                && pos.getZ() < maxZExclusive;
    }

    public int interiorFloorCount() {
        return Math.max(
                0,
                (maxYExclusive - minY - FLOOR_HEIGHT) / FLOOR_HEIGHT
        );
    }

    public boolean isMultiChunk() {
        return widthChunks * depthChunks > 1;
    }

    public int interiorFloorMinY(int floorIndex) {
        if (floorIndex < 0 || floorIndex >= interiorFloorCount()) {
            throw new IndexOutOfBoundsException("Invalid interior floor index");
        }
        return minY + floorIndex * FLOOR_HEIGHT;
    }

    public int interiorFloorMaxYExclusive(int floorIndex) {
        return interiorFloorMinY(floorIndex) + FLOOR_HEIGHT;
    }

    public boolean containsInterior(BlockPos pos) {
        return contains(pos)
                && pos.getY() < minY + interiorFloorCount() * FLOOR_HEIGHT;
    }

    public double distanceToSqr(BlockPos pos) {
        AABB bounds = bounds();
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.5D;
        double z = pos.getZ() + 0.5D;
        double dx = axisDistance(x, bounds.minX, bounds.maxX);
        double dy = axisDistance(y, bounds.minY, bounds.maxY);
        double dz = axisDistance(z, bounds.minZ, bounds.maxZ);
        return dx * dx + dy * dy + dz * dz;
    }

    private static double axisDistance(
            double coordinate,
            double minimum,
            double maximum
    ) {
        if (coordinate < minimum) {
            return minimum - coordinate;
        }
        if (coordinate > maximum) {
            return coordinate - maximum;
        }
        return 0.0D;
    }
}
