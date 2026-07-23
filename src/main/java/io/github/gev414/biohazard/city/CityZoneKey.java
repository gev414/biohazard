package io.github.gev414.biohazard.city;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public record CityZoneKey(
        ResourceLocation dimension,
        int anchorChunkX,
        int anchorChunkZ,
        boolean fallbackSector
) {

    public CityZoneKey {
        Objects.requireNonNull(dimension, "dimension");
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("dimension", dimension.toString());
        tag.putInt("anchorChunkX", anchorChunkX);
        tag.putInt("anchorChunkZ", anchorChunkZ);
        tag.putBoolean("fallbackSector", fallbackSector);
        return tag;
    }

    public static CityZoneKey load(CompoundTag tag) {
        return new CityZoneKey(
                ResourceLocation.parse(tag.getString("dimension")),
                tag.getInt("anchorChunkX"),
                tag.getInt("anchorChunkZ"),
                tag.getBoolean("fallbackSector")
        );
    }
}
