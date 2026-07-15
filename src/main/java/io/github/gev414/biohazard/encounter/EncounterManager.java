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

    private static int ticksUntilUpdate;

    public static void tick(MinecraftServer server) {
        if (ticksUntilUpdate > 0) {
            ticksUntilUpdate--;
            return;
        }
        ticksUntilUpdate = EncounterConfig.UPDATE_INTERVAL_TICKS.get() - 1;

        if (!EncounterConfig.ENABLED.get()) {
            return;
        }

        updateOccupiedBuildings(server);
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
                    EncounterConfig.BOSS_CHANCE.get(),
                    EncounterConfig.minimumKills(),
                    EncounterConfig.maximumKills()
            );
            return BuildingEncounter.materialize(
                    building.buildingId(),
                    selection
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

    private static void updateOccupiedBuildings(MinecraftServer server) {
        Map<BuildingKey, OccupiedBuilding> occupied =
                new LinkedHashMap<>();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.isAlive() || player.isSpectator()) {
                continue;
            }

            ServerLevel level = player.serverLevel();
            LostCitiesBuildingResolver.resolveCurrentChunk(
                            level,
                            player.blockPosition()
                    )
                    .filter(building -> building.contains(
                            player.blockPosition()
                    ))
                    .ifPresent(building -> occupied
                            .computeIfAbsent(
                                    building.key(),
                                    ignored -> new OccupiedBuilding(
                                            level,
                                            building,
                                            new ArrayList<>()
                                    )
                            )
                            .occupants()
                            .add(player));
        }

        for (OccupiedBuilding building : occupied.values()) {
            updateBuilding(building);
        }
    }

    private static void updateBuilding(OccupiedBuilding occupied) {
        ServerLevel level = occupied.level();
        BuildingDescriptor building = occupied.building();

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

        if (materialized.created()
                && encounter.phase() == EncounterPhase.REGULAR_WAVE) {
            announce(
                    occupied.occupants(),
                    "message.biohazard.encounter.haunted"
            );
        }

        switch (encounter.phase()) {
            case SAFE, CLEARED -> {
            }
            case REGULAR_WAVE -> updateRegularWave(
                    occupied,
                    data,
                    encounter
            );
            case BOSS_PENDING -> updateBossPending(
                    occupied,
                    data,
                    encounter
            );
            case BOSS_ACTIVE -> {
                // A missing loaded entity means unloaded, not permission to
                // create another boss. Its persisted UUID remains authoritative.
            }
        }
    }

    private static void updateRegularWave(
            OccupiedBuilding occupied,
            EncounterSavedData data,
            BuildingEncounter encounter
    ) {
        int activeRegulars = EncounterSpawner.countLoadedRegulars(
                occupied.level(),
                occupied.building()
        );

        if (encounter.regularDeaths() >= encounter.targetKills()) {
            if (activeRegulars > 0) {
                return;
            }

            if (encounter.bossSelected()) {
                if (encounter.beginBossWarning(
                        occupied.level().getGameTime()
                                + EncounterConfig.BOSS_WARNING_TICKS.get()
                )) {
                    data.setDirty();
                    announce(
                            occupied.occupants(),
                            "message.biohazard.encounter.boss_warning"
                    );
                }
            } else if (encounter.clear()) {
                data.setDirty();
                announce(
                        occupied.occupants(),
                        "message.biohazard.encounter.cleared"
                );
            }
            return;
        }

        if (activeRegulars
                < EncounterConfig.MAX_ACTIVE_REGULAR_MOBS.get()) {
            EncounterSpawner.spawnRegular(
                    occupied.level(),
                    occupied.building(),
                    occupied.occupants()
            );
        }
    }

    private static void updateBossPending(
            OccupiedBuilding occupied,
            EncounterSavedData data,
            BuildingEncounter encounter
    ) {
        if (EncounterSpawner.countLoadedRegulars(
                occupied.level(),
                occupied.building()
        ) > 0) {
            return;
        }

        Optional<BruteEntity> existingBoss =
                EncounterSpawner.findLoadedBoss(
                        occupied.level(),
                        occupied.building()
                );
        if (existingBoss.isPresent()) {
            if (encounter.activateBoss(existingBoss.get().getUUID())) {
                data.setDirty();
            }
            return;
        }

        if (occupied.level().getGameTime()
                < encounter.bossReadyGameTime()) {
            return;
        }

        EncounterSpawner.spawnBoss(
                occupied.level(),
                occupied.building(),
                occupied.occupants()
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

    private record OccupiedBuilding(
            ServerLevel level,
            BuildingDescriptor building,
            List<ServerPlayer> occupants
    ) {
    }

    private EncounterManager() {
    }
}
