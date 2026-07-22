package io.github.gev414.biohazard.lostcities;

import io.github.gev414.biohazard.encounter.BuildingDescriptor;
import io.github.gev414.biohazard.encounter.BuildingKey;
import mcjty.lostcities.api.ILostChunkInfo;
import mcjty.lostcities.api.ILostCities;
import mcjty.lostcities.api.ILostCityInformation;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class LostCitiesBuildingResolver {

    private static final int FLOOR_HEIGHT = 6;

    public static Optional<BuildingDescriptor> resolveCurrentChunk(
            ServerLevel level,
            BlockPos position
    ) {
        ILostCities lostCities = LostCitiesIntegration.api();
        if (lostCities == null) {
            return Optional.empty();
        }

        ILostCityInformation cityInformation =
                lostCities.getLostInfo(level);
        if (cityInformation == null) {
            return Optional.empty();
        }

        return resolveChunk(
                level,
                cityInformation,
                position.getX() >> 4,
                position.getZ() >> 4
        );
    }

    public static Optional<BuildingDescriptor> resolveNearest(
            ServerLevel level,
            BlockPos position,
            double radius
    ) {
        ILostCities lostCities = LostCitiesIntegration.api();
        if (lostCities == null) {
            return Optional.empty();
        }

        ILostCityInformation cityInformation =
                lostCities.getLostInfo(level);
        if (cityInformation == null) {
            return Optional.empty();
        }

        double effectiveRadius = Math.max(0.0D, radius);
        int minimumChunkX = (int) Math.floor(
                (position.getX() - effectiveRadius) / 16.0D
        );
        int maximumChunkX = (int) Math.floor(
                (position.getX() + effectiveRadius) / 16.0D
        );
        int minimumChunkZ = (int) Math.floor(
                (position.getZ() - effectiveRadius) / 16.0D
        );
        int maximumChunkZ = (int) Math.floor(
                (position.getZ() + effectiveRadius) / 16.0D
        );

        Map<BuildingKey, BuildingDescriptor> candidates =
                new LinkedHashMap<>();
        for (int chunkX = minimumChunkX;
             chunkX <= maximumChunkX;
             chunkX++) {
            for (int chunkZ = minimumChunkZ;
                 chunkZ <= maximumChunkZ;
                 chunkZ++) {
                resolveChunk(level, cityInformation, chunkX, chunkZ)
                        .ifPresent(building -> candidates.putIfAbsent(
                                building.key(),
                                building
                        ));
            }
        }

        double maximumDistanceSquared =
                effectiveRadius * effectiveRadius;
        BuildingDescriptor nearest = null;
        double nearestDistanceSquared = Double.POSITIVE_INFINITY;
        for (BuildingDescriptor candidate : candidates.values()) {
            double distanceSquared = candidate.distanceToSqr(position);
            if (distanceSquared <= maximumDistanceSquared
                    && distanceSquared < nearestDistanceSquared) {
                nearest = candidate;
                nearestDistanceSquared = distanceSquared;
            }
        }
        return Optional.ofNullable(nearest);
    }

    private static Optional<BuildingDescriptor> resolveChunk(
            ServerLevel level,
            ILostCityInformation cityInformation,
            int chunkX,
            int chunkZ
    ) {
        ILostChunkInfo chunkInfo = cityInformation.getChunkInfo(
                chunkX,
                chunkZ
        );
        if (chunkInfo == null) {
            return Optional.empty();
        }

        ResourceLocation buildingId = chunkInfo.getBuildingId();
        if (buildingId == null) {
            return Optional.empty();
        }

        int rootChunkX = chunkX;
        int rootChunkZ = chunkZ;
        int widthChunks = 1;
        int depthChunks = 1;

        ILostChunkInfo.MultiBuildingInfo multiBuilding =
                chunkInfo.getMultiBuildingInfo();
        if (multiBuilding != null) {
            rootChunkX -= multiBuilding.offsetX();
            rootChunkZ -= multiBuilding.offsetZ();
            widthChunks = Math.max(1, multiBuilding.w());
            depthChunks = Math.max(1, multiBuilding.h());
            buildingId = multiBuilding.buildingType();
        }

        int baseY = cityInformation.getRealHeight(
                chunkInfo.getCityLevel()
        );
        int minY = Math.max(
                level.getMinBuildHeight(),
                baseY - chunkInfo.getNumCellars() * FLOOR_HEIGHT
        );
        int maxYExclusive = Math.min(
                level.getMaxBuildHeight(),
                baseY + (chunkInfo.getNumFloors() + 1) * FLOOR_HEIGHT
        );
        if (minY >= maxYExclusive) {
            return Optional.empty();
        }

        BuildingKey key = new BuildingKey(
                level.dimension().location(),
                rootChunkX,
                rootChunkZ
        );
        return Optional.of(new BuildingDescriptor(
                key,
                buildingId,
                widthChunks,
                depthChunks,
                minY,
                maxYExclusive
        ));
    }

    private LostCitiesBuildingResolver() {
    }
}
