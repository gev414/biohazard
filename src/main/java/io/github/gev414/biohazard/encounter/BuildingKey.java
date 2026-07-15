package io.github.gev414.biohazard.encounter;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public record BuildingKey(
        ResourceLocation dimension,
        int rootChunkX,
        int rootChunkZ
) {

    public BuildingKey {
        Objects.requireNonNull(dimension, "dimension");
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("dimension", dimension.toString());
        tag.putInt("rootChunkX", rootChunkX);
        tag.putInt("rootChunkZ", rootChunkZ);
        return tag;
    }

    public static BuildingKey load(CompoundTag tag) {
        return new BuildingKey(
                ResourceLocation.parse(tag.getString("dimension")),
                tag.getInt("rootChunkX"),
                tag.getInt("rootChunkZ")
        );
    }
}
