package io.github.gev414.biohazard.city;

import io.github.gev414.biohazard.config.CityOperationsConfig;
import io.github.gev414.biohazard.encounter.BuildingKey;
import io.github.gev414.biohazard.lostcities.LostCitiesCityResolver;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class CityZoneSavedData extends SavedData {

    private static final String FILE_NAME = "biohazard_city_zones";
    private static final Factory<CityZoneSavedData> FACTORY =
            new Factory<>(
                    CityZoneSavedData::new,
                    CityZoneSavedData::load,
                    DataFixTypes.LEVEL
            );

    private final Map<CityZoneKey, CityZone> zones =
            new LinkedHashMap<>();
    private final Set<BuildingKey> allClearedBuildings =
            new LinkedHashSet<>();
    private final Map<OperationKey, OperationBinding> operationBindings =
            new LinkedHashMap<>();

    public static CityZoneSavedData get(MinecraftServer server) {
        return server.overworld()
                .getDataStorage()
                .computeIfAbsent(FACTORY, FILE_NAME);
    }

    public SurveyRegistration registerSurvey(
            ResourceLocation dimension,
            CitySurvey survey
    ) {
        if (survey.cityChunks().isEmpty()) {
            return SurveyRegistration.empty();
        }

        CityZoneKey key = survey.capped()
                ? fallbackKey(dimension, survey)
                : connectedCityKey(dimension, survey.cityChunks());
        CityZone zone = zones.get(key);
        int previousDanger = zone == null ? 0 : dangerLevel(zone);
        boolean changed = false;

        if (zone == null) {
            zone = new CityZone(key, survey.cityChunks());
            zones.put(key, zone);
            changed = true;
        } else if (zone.mergeFootprint(survey.cityChunks())) {
            changed = true;
        }

        int fallbackSize =
                CityOperationsConfig.FALLBACK_SECTOR_SIZE_CHUNKS.get();
        for (BuildingKey building : allClearedBuildings) {
            if (zone.containsBuilding(building, fallbackSize)
                    && zone.addClearedBuilding(building)) {
                changed = true;
            }
        }

        if (changed) {
            setDirty();
        }
        return new SurveyRegistration(
                Optional.of(key),
                previousDanger,
                dangerLevel(zone)
        );
    }

    public ClearResult recordClearedBuilding(BuildingKey building) {
        if (!allClearedBuildings.add(building)) {
            return ClearResult.unchanged();
        }

        setDirty();
        Optional<CityZone> resolved = findZone(building);
        if (resolved.isEmpty()) {
            return new ClearResult(true, Optional.empty(), 0, 0);
        }

        CityZone zone = resolved.get();
        int previousDanger = dangerLevel(zone);
        if (zone.addClearedBuilding(building)) {
            setDirty();
        }
        return new ClearResult(
                true,
                Optional.of(zone.key()),
                previousDanger,
                dangerLevel(zone)
        );
    }

    public Optional<ZoneStatus> status(CityZoneKey key) {
        CityZone zone = zones.get(key);
        if (zone == null) {
            return Optional.empty();
        }
        int cleared = zone.clearedBuildingCount();
        int perLevel =
                CityOperationsConfig.CLEARED_BUILDINGS_PER_LEVEL.get();
        int maximum = CityOperationsConfig.MAX_DANGER_LEVEL.get();
        return Optional.of(new ZoneStatus(
                key,
                cleared,
                CityDangerProgression.level(cleared, perLevel, maximum),
                maximum,
                CityDangerProgression.remainingUntilNextLevel(
                        cleared,
                        perLevel,
                        maximum
                )
        ));
    }

    public int dangerLevelAt(
            ServerLevel level,
            int chunkX,
            int chunkZ
    ) {
        ResourceLocation dimension = level.dimension().location();
        int perimeter =
                CityOperationsConfig.INFLUENCE_PERIMETER_CHUNKS.get();
        int fallbackSize =
                CityOperationsConfig.FALLBACK_SECTOR_SIZE_CHUNKS.get();
        int highest = 0;

        for (CityZone zone : zones.values()) {
            if (!zone.key().dimension().equals(dimension)) {
                continue;
            }

            boolean influenced = zone.key().fallbackSector()
                    ? influencesFallbackZone(
                    level,
                    zone,
                    chunkX,
                    chunkZ,
                    perimeter,
                    fallbackSize
            )
                    : zone.influencesFullZone(
                    chunkX,
                    chunkZ,
                    perimeter
            );
            if (influenced) {
                highest = Math.max(highest, dangerLevel(zone));
            }
        }
        return highest;
    }

    public boolean bindOperation(
            UUID teamId,
            long taskId,
            CityZoneKey zoneKey
    ) {
        CityZone zone = zones.get(zoneKey);
        if (zone == null) {
            return false;
        }
        operationBindings.put(
                new OperationKey(teamId, taskId),
                new OperationBinding(
                        zoneKey,
                        zone.clearedBuildingCount()
                )
        );
        setDirty();
        return true;
    }

    public long operationProgress(UUID teamId, long taskId) {
        OperationBinding binding = operationBindings.get(
                new OperationKey(teamId, taskId)
        );
        if (binding == null) {
            return 0L;
        }
        CityZone zone = zones.get(binding.zone());
        if (zone == null) {
            return 0L;
        }
        return Math.max(
                0,
                zone.clearedBuildingCount() - binding.baselineCleared()
        );
    }

    @Override
    public CompoundTag save(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        ListTag savedZones = new ListTag();
        for (CityZone zone : zones.values()) {
            savedZones.add(zone.save());
        }
        tag.put("zones", savedZones);

        ListTag cleared = new ListTag();
        for (BuildingKey building : allClearedBuildings) {
            cleared.add(building.save());
        }
        tag.put("allClearedBuildings", cleared);

        ListTag bindings = new ListTag();
        for (Map.Entry<OperationKey, OperationBinding> entry
                : operationBindings.entrySet()) {
            CompoundTag savedBinding = new CompoundTag();
            savedBinding.putUUID("teamId", entry.getKey().teamId());
            savedBinding.putLong("taskId", entry.getKey().taskId());
            savedBinding.put("zone", entry.getValue().zone().save());
            savedBinding.putInt(
                    "baselineCleared",
                    entry.getValue().baselineCleared()
            );
            bindings.add(savedBinding);
        }
        tag.put("operationBindings", bindings);
        return tag;
    }

    private Optional<CityZone> findZone(BuildingKey building) {
        int fallbackSize =
                CityOperationsConfig.FALLBACK_SECTOR_SIZE_CHUNKS.get();

        Optional<CityZone> exact = zones.values().stream()
                .filter(zone -> !zone.key().fallbackSector())
                .filter(zone -> zone.containsBuilding(
                        building,
                        fallbackSize
                ))
                .findFirst();
        if (exact.isPresent()) {
            return exact;
        }
        return zones.values().stream()
                .filter(zone -> zone.key().fallbackSector())
                .filter(zone -> zone.containsBuilding(
                        building,
                        fallbackSize
                ))
                .findFirst();
    }

    private static boolean influencesFallbackZone(
            ServerLevel level,
            CityZone zone,
            int chunkX,
            int chunkZ,
            int perimeter,
            int fallbackSize
    ) {
        for (int x = chunkX - perimeter;
             x <= chunkX + perimeter;
             x++) {
            for (int z = chunkZ - perimeter;
                 z <= chunkZ + perimeter;
                 z++) {
                if (zone.containsFallbackChunk(x, z, fallbackSize)
                        && LostCitiesCityResolver.isCityChunk(
                        level,
                        x,
                        z
                )) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int dangerLevel(CityZone zone) {
        return CityDangerProgression.level(
                zone.clearedBuildingCount(),
                CityOperationsConfig.CLEARED_BUILDINGS_PER_LEVEL.get(),
                CityOperationsConfig.MAX_DANGER_LEVEL.get()
        );
    }

    private static CityZoneKey connectedCityKey(
            ResourceLocation dimension,
            Collection<Long> cityChunks
    ) {
        long anchor = cityChunks.stream()
                .min(Comparator
                        .comparingInt((Long packed) ->
                                ChunkPos.getX(packed))
                        .thenComparingInt(ChunkPos::getZ))
                .orElseThrow();
        return new CityZoneKey(
                dimension,
                ChunkPos.getX(anchor),
                ChunkPos.getZ(anchor),
                false
        );
    }

    private static CityZoneKey fallbackKey(
            ResourceLocation dimension,
            CitySurvey survey
    ) {
        int size = CityOperationsConfig
                .FALLBACK_SECTOR_SIZE_CHUNKS
                .get();
        return new CityZoneKey(
                dimension,
                Math.floorDiv(survey.originChunkX(), size) * size,
                Math.floorDiv(survey.originChunkZ(), size) * size,
                true
        );
    }

    private static CityZoneSavedData load(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        CityZoneSavedData data = new CityZoneSavedData();

        ListTag savedZones = tag.getList("zones", Tag.TAG_COMPOUND);
        for (int index = 0; index < savedZones.size(); index++) {
            try {
                CityZone zone = CityZone.load(
                        savedZones.getCompound(index)
                );
                data.zones.put(zone.key(), zone);
            } catch (RuntimeException ignored) {
                // Keep valid zones when one persisted entry is malformed.
            }
        }

        ListTag cleared = tag.getList(
                "allClearedBuildings",
                Tag.TAG_COMPOUND
        );
        for (int index = 0; index < cleared.size(); index++) {
            try {
                data.allClearedBuildings.add(BuildingKey.load(
                        cleared.getCompound(index)
                ));
            } catch (RuntimeException ignored) {
                // Keep valid building keys.
            }
        }

        ListTag bindings = tag.getList(
                "operationBindings",
                Tag.TAG_COMPOUND
        );
        for (int index = 0; index < bindings.size(); index++) {
            CompoundTag savedBinding = bindings.getCompound(index);
            try {
                OperationKey operation = new OperationKey(
                        savedBinding.getUUID("teamId"),
                        savedBinding.getLong("taskId")
                );
                OperationBinding binding = new OperationBinding(
                        CityZoneKey.load(
                                savedBinding.getCompound("zone")
                        ),
                        savedBinding.getInt("baselineCleared")
                );
                data.operationBindings.put(operation, binding);
            } catch (RuntimeException ignored) {
                // Keep valid operation bindings.
            }
        }
        return data;
    }

    public record SurveyRegistration(
            Optional<CityZoneKey> zone,
            int previousDanger,
            int currentDanger
    ) {

        static SurveyRegistration empty() {
            return new SurveyRegistration(Optional.empty(), 0, 0);
        }
    }

    public record ClearResult(
            boolean newlyTracked,
            Optional<CityZoneKey> zone,
            int previousDanger,
            int currentDanger
    ) {

        static ClearResult unchanged() {
            return new ClearResult(false, Optional.empty(), 0, 0);
        }
    }

    public record ZoneStatus(
            CityZoneKey key,
            int clearedBuildings,
            int dangerLevel,
            int maximumDangerLevel,
            int remainingUntilNextLevel
    ) {
    }

    private record OperationKey(UUID teamId, long taskId) {
    }

    private record OperationBinding(
            CityZoneKey zone,
            int baselineCleared
    ) {
    }
}
