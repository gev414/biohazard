package io.github.gev414.biohazard.city;

import io.github.gev414.biohazard.encounter.BuildingKey;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.ChunkPos;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

final class CityZone {

    private final CityZoneKey key;
    private final Set<Long> footprint = new LinkedHashSet<>();
    private final Set<BuildingKey> clearedBuildings = new LinkedHashSet<>();

    CityZone(CityZoneKey key, Collection<Long> footprint) {
        this.key = key;
        this.footprint.addAll(footprint);
    }

    CityZoneKey key() {
        return key;
    }

    int clearedBuildingCount() {
        return clearedBuildings.size();
    }

    boolean addClearedBuilding(BuildingKey building) {
        return clearedBuildings.add(building);
    }

    boolean mergeFootprint(Collection<Long> chunks) {
        return footprint.addAll(chunks);
    }

    boolean containsBuilding(BuildingKey building, int fallbackSectorSize) {
        if (!key.dimension().equals(building.dimension())) {
            return false;
        }
        if (key.fallbackSector()) {
            return containsFallbackChunk(
                    building.rootChunkX(),
                    building.rootChunkZ(),
                    fallbackSectorSize
            );
        }
        return footprint.contains(ChunkPos.asLong(
                building.rootChunkX(),
                building.rootChunkZ()
        ));
    }

    boolean influencesFullZone(
            int chunkX,
            int chunkZ,
            int perimeter
    ) {
        int safePerimeter = Math.max(0, perimeter);
        for (int x = chunkX - safePerimeter;
             x <= chunkX + safePerimeter;
             x++) {
            for (int z = chunkZ - safePerimeter;
                 z <= chunkZ + safePerimeter;
                 z++) {
                if (footprint.contains(ChunkPos.asLong(x, z))) {
                    return true;
                }
            }
        }
        return false;
    }

    boolean containsFallbackChunk(
            int chunkX,
            int chunkZ,
            int fallbackSectorSize
    ) {
        int size = Math.max(1, fallbackSectorSize);
        return chunkX >= key.anchorChunkX()
                && chunkX < key.anchorChunkX() + size
                && chunkZ >= key.anchorChunkZ()
                && chunkZ < key.anchorChunkZ() + size;
    }

    CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.put("key", key.save());
        tag.putLongArray(
                "footprint",
                footprint.stream().mapToLong(Long::longValue).toArray()
        );

        ListTag cleared = new ListTag();
        for (BuildingKey building : clearedBuildings) {
            cleared.add(building.save());
        }
        tag.put("clearedBuildings", cleared);
        return tag;
    }

    static CityZone load(CompoundTag tag) {
        CityZone zone = new CityZone(
                CityZoneKey.load(tag.getCompound("key")),
                toSet(tag.getLongArray("footprint"))
        );
        ListTag cleared = tag.getList(
                "clearedBuildings",
                Tag.TAG_COMPOUND
        );
        for (int index = 0; index < cleared.size(); index++) {
            zone.clearedBuildings.add(BuildingKey.load(
                    cleared.getCompound(index)
            ));
        }
        return zone;
    }

    private static Set<Long> toSet(long[] values) {
        Set<Long> result = new LinkedHashSet<>();
        for (long value : values) {
            result.add(value);
        }
        return result;
    }
}
