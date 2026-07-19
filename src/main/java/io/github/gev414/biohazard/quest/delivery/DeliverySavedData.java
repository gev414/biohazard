package io.github.gev414.biohazard.quest.delivery;

import io.github.gev414.biohazard.Biohazard;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

final class DeliverySavedData extends SavedData {

    private static final String FILE_NAME = "biohazard_radio_deliveries";
    private static final Factory<DeliverySavedData> FACTORY = new Factory<>(
            DeliverySavedData::new,
            DeliverySavedData::load,
            DataFixTypes.LEVEL
    );

    private final List<RadioDelivery> deliveries = new ArrayList<>();

    static DeliverySavedData get(MinecraftServer server) {
        return server.overworld()
                .getDataStorage()
                .computeIfAbsent(FACTORY, FILE_NAME);
    }

    List<RadioDelivery> deliveries() {
        return deliveries;
    }

    void add(RadioDelivery delivery) {
        deliveries.add(delivery);
        setDirty();
    }

    @Override
    public CompoundTag save(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        ListTag entries = new ListTag();
        for (RadioDelivery delivery : deliveries) {
            entries.add(delivery.save(registries));
        }
        tag.put("deliveries", entries);
        return tag;
    }

    private static DeliverySavedData load(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        DeliverySavedData data = new DeliverySavedData();
        ListTag entries = tag.getList("deliveries", Tag.TAG_COMPOUND);
        for (int index = 0; index < entries.size(); index++) {
            try {
                RadioDelivery delivery = RadioDelivery.load(
                        entries.getCompound(index),
                        registries
                );
                if (!delivery.items().isEmpty()) {
                    data.deliveries.add(delivery);
                }
            } catch (RuntimeException exception) {
                // One malformed delivery must not invalidate the entire mailbox.
                Biohazard.LOGGER.error(
                        "Skipping malformed radio delivery at index {}",
                        index,
                        exception
                );
            }
        }
        return data;
    }
}
