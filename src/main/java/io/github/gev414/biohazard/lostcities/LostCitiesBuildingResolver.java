package io.github.gev414.biohazard.lostcities;

import io.github.gev414.biohazard.encounter.BuildingDescriptor;
import io.github.gev414.biohazard.encounter.BuildingKey;
import mcjty.lostcities.api.ILostChunkInfo;
import mcjty.lostcities.api.ILostCities;
import mcjty.lostcities.api.ILostCityInformation;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

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

        int chunkX = position.getX() >> 4;
        int chunkZ = position.getZ() >> 4;
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
