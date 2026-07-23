package io.github.gev414.biohazard.lostcities;

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
        ILostCities lostCities = LostCitiesIntegration.api();
        if (lostCities == null) {
            return false;
        }

        ILostCityInformation cityInformation =
                lostCities.getLostInfo(level);
        if (cityInformation == null) {
            return false;
        }

        ILostChunkInfo chunkInfo = cityInformation.getChunkInfo(
                chunkX,
                chunkZ
        );
        return chunkInfo != null && chunkInfo.isCity();
    }

    private LostCitiesCityResolver() {
    }
}
