package io.github.gev414.biohazard.encounter;

import io.github.gev414.biohazard.config.EncounterConfig;
import io.github.gev414.biohazard.entity.BruteEntity;
import io.github.gev414.biohazard.lostcities.LostCitiesBuildingResolver;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class EncounterManager {

    private static int ticksUntilActivationScan;
    private static int ticksUntilUpdate;

    public static void tick(MinecraftServer server) {
        if (!EncounterConfig.ENABLED.get()) {
            ticksUntilActivationScan = 0;
            ticksUntilUpdate = 0;
            return;
        }

        if (ticksUntilUpdate > 0) {
            ticksUntilUpdate--;
        }
        if (ticksUntilActivationScan > 0) {
            ticksUntilActivationScan--;
            return;
        }
        ticksUntilActivationScan =
                EncounterConfig.ACTIVATION_SCAN_INTERVAL_TICKS.get() - 1;

        boolean scheduledUpdate = ticksUntilUpdate <= 0;
        if (scheduledUpdate) {
            ticksUntilUpdate =
                    EncounterConfig.UPDATE_INTERVAL_TICKS.get() - 1;
        }
        updateActivatedBuildings(server, scheduledUpdate);
    }

    public static EncounterSavedData.MaterializedEncounter materialize(
            ServerLevel level,
            BuildingDescriptor building
    ) {
        EncounterSavedData data = EncounterSavedData.get(
                level.getServer()
        );
        return data.getOrCreate(building.key(), () -> {
            EncounterSelection selection = EncounterSelection.select(
                    level.getSeed(),
                    building.key(),
                    EncounterConfig.HAUNTED_CHANCE.get(),
                    building.isMultiChunk()
                            ? EncounterConfig.LARGE_BUILDING_BOSS_CHANCE.get()
                            : EncounterConfig.BOSS_CHANCE.get(),
                    building.isMultiChunk(),
                    EncounterConfig.minimumKills(),
                    EncounterConfig.maximumKills()
            );
            return BuildingEncounter.materialize(
                    building.buildingId(),
                    selection,
                    EncounterConfig.SPAWN_MODE.get()
            );
        });
    }

    public static void recordDeath(
            MinecraftServer server,
            EncounterEntityData.Marker marker
    ) {
        EncounterSavedData data = EncounterSavedData.get(server);
        Optional<BuildingEncounter> existing = data.find(marker.key());
        if (existing.isEmpty()) {
            return;
        }

        BuildingEncounter encounter = existing.get();
        boolean changed = switch (marker.role()) {
            case REGULAR -> encounter.recordRegularDeath();
            case BOSS -> encounter.clear();
        };
        if (changed) {
            data.setDirty();
        }
    }

    private static void updateActivatedBuildings(
            MinecraftServer server,
            boolean scheduledUpdate
    ) {
        Map<BuildingKey, ActivatedBuilding> activated =
                new LinkedHashMap<>();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.isAlive() || player.isSpectator()) {
                continue;
            }

            ServerLevel level = player.serverLevel();
            LostCitiesBuildingResolver.resolveNearest(
                            level,
                            player.blockPosition(),
                            EncounterConfig.ACTIVATION_RADIUS.get()
                    )
                    .ifPresent(building -> activated
                            .computeIfAbsent(
                                    building.key(),
                                    ignored -> new ActivatedBuilding(
                                            level,
                                            building,
                                            new ArrayList<>()
                                    )
                            )
                            .nearbyPlayers()
                            .add(player));
        }

        for (ActivatedBuilding building : activated.values()) {
            updateBuilding(building, scheduledUpdate);
        }
    }

    private static void updateBuilding(
            ActivatedBuilding activated,
            boolean scheduledUpdate
    ) {
        ServerLevel level = activated.level();
        BuildingDescriptor building = activated.building();

        EncounterSavedData data = EncounterSavedData.get(
                level.getServer()
        );
        Optional<BuildingEncounter> existing = data.find(
                building.key()
        );
        if (existing.isPresent()
                ? EncounterConfig.isExcluded(
                existing.get().buildingId()
        )
                : EncounterConfig.isExcluded(building.buildingId())) {
            return;
        }

        EncounterSavedData.MaterializedEncounter materialized =
                materialize(level, building);
        BuildingEncounter encounter = materialized.encounter();

        switch (encounter.phase()) {
            case SAFE, CLEARED -> {
            }
            case REGULAR_WAVE -> {
                boolean populationNotStarted =
                        encounter.spawnMode() == EncounterSpawnMode.INSTANT
                                && !encounter.initialPopulationAttempted();
                if (materialized.created()
                        || scheduledUpdate
                        || populationNotStarted) {
                    updateRegularWave(
                            activated,
                            data,
                            encounter
                    );
                }
            }
            case BOSS_PENDING -> {
                if (materialized.created() || scheduledUpdate) {
                    updateBossPending(
                            activated,
                            data,
                            encounter
                    );
                }
            }
            case BOSS_ACTIVE -> {
                // A missing loaded entity means unloaded, not permission to
                // create another boss. Its persisted UUID remains authoritative.
            }
        }
    }

    private static void updateRegularWave(
            ActivatedBuilding activated,
            EncounterSavedData data,
            BuildingEncounter encounter
    ) {
        int activeRegulars = EncounterSpawner.countLoadedRegulars(
                activated.level(),
                activated.building()
        );

        if (encounter.regularDeaths() >= encounter.targetKills()) {
            /*
             * Boss buildings begin their finale immediately after reaching
             * the kill target. Existing regular mobs remain in the building,
             * but no replacements are spawned.
             */
            if (encounter.bossSelected()) {
                if (encounter.beginBossWarning(
                        activated.level().getGameTime()
                                + EncounterConfig.BOSS_WARNING_TICKS.get()
                )) {
                    data.setDirty();
                    announce(
                            activated.nearbyPlayers(),
                            "message.biohazard.encounter.boss_warning"
                    );
                }

                return;
            }

            if (activeRegulars > 0) {
                return;
            }

            if (encounter.clear()) {
                data.setDirty();
                announce(
                        activated.nearbyPlayers(),
                        "message.biohazard.encounter.cleared"
                );
            }

            return;
        }

        if (encounter.spawnMode() == EncounterSpawnMode.INSTANT) {
            if (encounter.beginInitialPopulation()) {
                data.setDirty();
            }
            int spawned = EncounterSpawner.spawnInitialPopulation(
                    activated.level(),
                    activated.building(),
                    activated.nearbyPlayers(),
                    encounter.remainingInitialSpawns()
            );
            for (int index = 0; index < spawned; index++) {
                if (encounter.recordRegularSpawn()) {
                    data.setDirty();
                }
            }
            return;
        }

        if (activeRegulars
                < EncounterConfig.MAX_ACTIVE_REGULAR_MOBS.get()) {
            EncounterSpawner.spawnRegular(
                    activated.level(),
                    activated.building(),
                    activated.nearbyPlayers()
            );
        }
    }

    private static void updateBossPending(
            ActivatedBuilding activated,
            EncounterSavedData data,
            BuildingEncounter encounter
    ) {
        Optional<BruteEntity> existingBoss =
                EncounterSpawner.findLoadedBoss(
                        activated.level(),
                        activated.building()
                );
        if (existingBoss.isPresent()) {
            if (encounter.activateBoss(existingBoss.get().getUUID())) {
                data.setDirty();
            }
            return;
        }

        if (activated.level().getGameTime()
                < encounter.bossReadyGameTime()) {
            return;
        }

        EncounterSpawner.spawnBoss(
                activated.level(),
                activated.building(),
                activated.nearbyPlayers()
        ).ifPresent(brute -> {
            if (encounter.activateBoss(brute.getUUID())) {
                data.setDirty();
            }
        });
    }

    private static void announce(
            List<ServerPlayer> players,
            String translationKey
    ) {
        if (!EncounterConfig.ANNOUNCE_STATE_CHANGES.get()) {
            return;
        }
        Component message = Component.translatable(translationKey);
        for (ServerPlayer player : players) {
            player.sendSystemMessage(message);
        }
    }

    private record ActivatedBuilding(
            ServerLevel level,
            BuildingDescriptor building,
            List<ServerPlayer> nearbyPlayers
    ) {
    }

    private EncounterManager() {
    }
}
