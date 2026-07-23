package io.github.gev414.biohazard.city;

import io.github.gev414.biohazard.config.CityOperationsConfig;
import io.github.gev414.biohazard.encounter.BuildingKey;
import io.github.gev414.biohazard.encounter.EncounterSavedData;
import io.github.gev414.biohazard.network.CityStatusPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Optional;
import java.util.UUID;

public final class CityZoneManager {

    public static Optional<CityZoneKey> registerSurvey(
            ServerLevel level,
            CitySurvey survey
    ) {
        if (!CityOperationsConfig.ENABLED.get()) {
            return Optional.empty();
        }

        CityZoneSavedData.SurveyRegistration registration =
                CityZoneSavedData.get(level.getServer()).registerSurvey(
                        level.dimension().location(),
                        survey
                );
        if (registration.currentDanger()
                > registration.previousDanger()) {
            InfectedCityScaling.updateLoadedInfected(level.getServer());
        }
        for (BuildingKey cleared : EncounterSavedData
                .get(level.getServer())
                .clearedBuildingKeys()) {
            recordClearedBuilding(level.getServer(), cleared);
        }
        return registration.zone();
    }

    public static void recordClearedBuilding(
            MinecraftServer server,
            BuildingKey building
    ) {
        if (!CityOperationsConfig.ENABLED.get()) {
            return;
        }

        CityZoneSavedData.ClearResult result =
                CityZoneSavedData.get(server)
                        .recordClearedBuilding(building);
        if (result.currentDanger() > result.previousDanger()) {
            InfectedCityScaling.updateLoadedInfected(server);
        }
    }

    public static int dangerLevelAt(
            ServerLevel level,
            int chunkX,
            int chunkZ
    ) {
        if (!CityOperationsConfig.ENABLED.get()) {
            return 0;
        }
        return CityZoneSavedData.get(level.getServer())
                .dangerLevelAt(level, chunkX, chunkZ);
    }

    public static Optional<CityZoneSavedData.ZoneStatus> status(
            MinecraftServer server,
            CityZoneKey key
    ) {
        if (!CityOperationsConfig.ENABLED.get()) {
            return Optional.empty();
        }
        return CityZoneSavedData.get(server).status(key);
    }

    public static void sendStatus(
            ServerPlayer player,
            CityZoneKey key
    ) {
        status(player.getServer(), key).ifPresentOrElse(
                cityStatus -> PacketDistributor.sendToPlayer(
                        player,
                        new CityStatusPayload(
                                true,
                                cityStatus.clearedBuildings(),
                                cityStatus.dangerLevel(),
                                cityStatus.maximumDangerLevel(),
                                healthPercent(cityStatus),
                                cityStatus.remainingUntilNextLevel()
                        )
                ),
                () -> sendNoStatus(player)
        );
    }

    public static void sendNoStatus(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, CityStatusPayload.noCity());
    }

    private static int healthPercent(
            CityZoneSavedData.ZoneStatus status
    ) {
        return (int) Math.round(
                status.dangerLevel()
                        * CityOperationsConfig.HEALTH_PER_DANGER_LEVEL.get()
                        * 100.0D
        );
    }

    public static boolean bindOperation(
            MinecraftServer server,
            UUID teamId,
            long taskId,
            CityZoneKey key
    ) {
        return CityOperationsConfig.ENABLED.get()
                && CityZoneSavedData.get(server).bindOperation(
                teamId,
                taskId,
                key
        );
    }

    public static long operationProgress(
            MinecraftServer server,
            UUID teamId,
            long taskId
    ) {
        if (!CityOperationsConfig.ENABLED.get()) {
            return 0L;
        }
        return CityZoneSavedData.get(server).operationProgress(
                teamId,
                taskId
        );
    }

    private CityZoneManager() {
    }
}
