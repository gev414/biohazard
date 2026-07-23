package io.github.gev414.biohazard.encounter;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

public final class EncounterSavedData extends SavedData {

    private static final String FILE_NAME =
            "biohazard_building_encounters";
    private static final Factory<EncounterSavedData> FACTORY =
            new Factory<>(
                    EncounterSavedData::new,
                    EncounterSavedData::load,
                    DataFixTypes.LEVEL
            );

    private final Map<BuildingKey, BuildingEncounter> encounters =
            new LinkedHashMap<>();

    public static EncounterSavedData get(MinecraftServer server) {
        return server.overworld()
                .getDataStorage()
                .computeIfAbsent(FACTORY, FILE_NAME);
    }

    public Optional<BuildingEncounter> find(BuildingKey key) {
        return Optional.ofNullable(encounters.get(key));
    }

    public Set<BuildingKey> clearedBuildingKeys() {
        Set<BuildingKey> cleared = new LinkedHashSet<>();
        for (Map.Entry<BuildingKey, BuildingEncounter> entry
                : encounters.entrySet()) {
            if (entry.getValue().phase() == EncounterPhase.CLEARED) {
                cleared.add(entry.getKey());
            }
        }
        return Set.copyOf(cleared);
    }

    public MaterializedEncounter getOrCreate(
            BuildingKey key,
            Supplier<BuildingEncounter> factory
    ) {
        BuildingEncounter existing = encounters.get(key);
        if (existing != null) {
            return new MaterializedEncounter(existing, false);
        }

        BuildingEncounter created = factory.get();
        encounters.put(key, created);
        setDirty();
        return new MaterializedEncounter(created, true);
    }

    @Override
    public CompoundTag save(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        ListTag entries = new ListTag();
        for (Map.Entry<BuildingKey, BuildingEncounter> entry
                : encounters.entrySet()) {
            CompoundTag savedEntry = new CompoundTag();
            savedEntry.put("key", entry.getKey().save());
            savedEntry.put("encounter", entry.getValue().save());
            entries.add(savedEntry);
        }
        tag.put("encounters", entries);
        return tag;
    }

    private static EncounterSavedData load(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        EncounterSavedData data = new EncounterSavedData();
        ListTag entries = tag.getList(
                "encounters",
                Tag.TAG_COMPOUND
        );
        for (int index = 0; index < entries.size(); index++) {
            CompoundTag savedEntry = entries.getCompound(index);
            try {
                BuildingKey key = BuildingKey.load(
                        savedEntry.getCompound("key")
                );
                BuildingEncounter encounter = BuildingEncounter.load(
                        savedEntry.getCompound("encounter")
                );
                data.encounters.put(key, encounter);
            } catch (RuntimeException ignored) {
                // Ignore malformed entries while keeping the rest of the save usable.
            }
        }
        return data;
    }

    public record MaterializedEncounter(
            BuildingEncounter encounter,
            boolean created
    ) {
    }
}
