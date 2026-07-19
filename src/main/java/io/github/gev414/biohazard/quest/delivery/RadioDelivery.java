package io.github.gev414.biohazard.quest.delivery;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class RadioDelivery {

    private final UUID id;
    private final UUID owner;
    private final long rewardId;
    private final String manifest;
    private final DeliveryCategory category;
    private final long orderedAt;
    private final long readyAt;
    private DeliveryKind kind;
    private List<ItemStack> items;
    private boolean notified;

    RadioDelivery(
            UUID id,
            UUID owner,
            long rewardId,
            String manifest,
            DeliveryCategory category,
            long orderedAt,
            long readyAt,
            DeliveryKind kind,
            List<ItemStack> items,
            boolean notified
    ) {
        this.id = id;
        this.owner = owner;
        this.rewardId = rewardId;
        this.manifest = manifest;
        this.category = category;
        this.orderedAt = orderedAt;
        this.readyAt = readyAt;
        this.kind = kind;
        this.items = copyItems(items);
        this.notified = notified;
    }

    UUID owner() {
        return owner;
    }

    UUID id() {
        return id;
    }

    DeliveryCategory category() {
        return category;
    }

    long readyAt() {
        return readyAt;
    }

    List<ItemStack> items() {
        return items;
    }

    boolean isChoice() {
        return kind == DeliveryKind.CHOICE;
    }

    boolean notified() {
        return notified;
    }

    boolean isReady(long gameTime) {
        return gameTime >= readyAt;
    }

    void markNotified() {
        notified = true;
    }

    void replaceItems(List<ItemStack> remaining) {
        items = copyItems(remaining);
    }

    void select(int index) {
        ItemStack selected = items.get(index).copy();
        items = List.of(selected);
        kind = DeliveryKind.ITEMS;
    }

    CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("id", id);
        tag.putUUID("owner", owner);
        tag.putLong("reward_id", rewardId);
        tag.putString("manifest", manifest);
        tag.putString("category", category.serializedName());
        tag.putLong("ordered_at", orderedAt);
        tag.putLong("ready_at", readyAt);
        tag.putString("kind", kind.name());
        tag.putBoolean("notified", notified);

        ListTag savedItems = new ListTag();
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                savedItems.add(stack.save(registries));
            }
        }
        tag.put("items", savedItems);
        return tag;
    }

    static RadioDelivery load(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        List<ItemStack> items = new ArrayList<>();
        ListTag savedItems = tag.getList("items", Tag.TAG_COMPOUND);
        for (int index = 0; index < savedItems.size(); index++) {
            ItemStack.parse(registries, savedItems.getCompound(index))
                    .filter(stack -> !stack.isEmpty())
                    .ifPresent(items::add);
        }

        return new RadioDelivery(
                tag.getUUID("id"),
                tag.getUUID("owner"),
                tag.getLong("reward_id"),
                tag.getString("manifest"),
                DeliveryCategory.fromName(tag.getString("category")),
                tag.getLong("ordered_at"),
                tag.getLong("ready_at"),
                DeliveryKind.fromName(tag.getString("kind")),
                items,
                tag.getBoolean("notified")
        );
    }

    private static List<ItemStack> copyItems(List<ItemStack> items) {
        return items.stream().map(ItemStack::copy).toList();
    }
}
