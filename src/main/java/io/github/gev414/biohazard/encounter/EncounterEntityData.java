package io.github.gev414.biohazard.encounter;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

import java.util.Locale;
import java.util.Optional;

public final class EncounterEntityData {

    private static final String ROOT_TAG = "biohazardEncounter";

    public static void mark(
            Entity entity,
            BuildingKey key,
            Role role
    ) {
        CompoundTag marker = key.save();
        marker.putString(
                "role",
                role.name().toLowerCase(Locale.ROOT)
        );
        entity.getPersistentData().put(ROOT_TAG, marker);
    }

    public static Optional<Marker> read(Entity entity) {
        CompoundTag persistentData = entity.getPersistentData();
        if (!persistentData.contains(ROOT_TAG, CompoundTag.TAG_COMPOUND)) {
            return Optional.empty();
        }

        CompoundTag marker = persistentData.getCompound(ROOT_TAG);
        try {
            BuildingKey key = BuildingKey.load(marker);
            Role role = Role.valueOf(
                    marker.getString("role")
                            .toUpperCase(Locale.ROOT)
            );
            return Optional.of(new Marker(key, role));
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    public static boolean matches(
            Entity entity,
            BuildingKey key,
            Role role
    ) {
        return read(entity)
                .map(marker -> marker.key().equals(key)
                        && marker.role() == role)
                .orElse(false);
    }

    public enum Role {
        REGULAR,
        BOSS
    }

    public record Marker(BuildingKey key, Role role) {
    }

    private EncounterEntityData() {
    }
}
