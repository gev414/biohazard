package io.github.gev414.rotwire.lostcities;

import mcjty.lostcities.api.ILostChunkInfo;
import mcjty.lostcities.api.ILostCities;
import mcjty.lostcities.api.ILostCityInformation;
import net.minecraft.server.level.ServerLevel;

public final class LostCitiesCityResolver {

    public static boolean isCityChunk(
            ServerLevel level,
            int chunkX,
            int chunkZ
    ) {
        ILostChunkInfo chunkInfo = chunkInfo(level, chunkX, chunkZ);
        return chunkInfo != null && chunkInfo.isCity();
    }

    public static boolean isStreetChunk(
            ServerLevel level,
            int chunkX,
            int chunkZ
    ) {
        ILostChunkInfo chunkInfo = chunkInfo(level, chunkX, chunkZ);
        return chunkInfo != null
                && chunkInfo.isCity()
                && chunkInfo.getBuildingId() == null;
    }

    private static ILostChunkInfo chunkInfo(
            ServerLevel level,
            int chunkX,
            int chunkZ
    ) {
        ILostCities lostCities = LostCitiesIntegration.api();
        if (lostCities == null) {
            return null;
        }

        ILostCityInformation cityInformation =
                lostCities.getLostInfo(level);
        if (cityInformation == null) {
            return null;
        }

        return cityInformation.getChunkInfo(
                chunkX,
                chunkZ
        );
    }

    private LostCitiesCityResolver() {
    }
}
