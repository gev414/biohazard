package io.github.gev414.rotwire.settlement;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * Finds the physical food stores that belong to active camps. Item stacks are
 * the authority: settlement data caches observed nutrition for the radio
 * screen, plus prepared portions from whole food items already consumed.
 */
final class SettlementStockpile {

    private static final ResourceLocation MOSIN_AMMUNITION_ID =
            ResourceLocation.fromNamespaceAndPath(
                    "pointblank",
                    "ammo762x51"
            );

    static StockpileSnapshot inspect(
            ServerLevel level,
            Collection<SettlementRadioStatus> radios
    ) {
        return summarize(findSources(level, radios));
    }

    static StockpileConsumption consume(
            ServerLevel level,
            Collection<SettlementRadioStatus> radios,
            int requestedRations
    ) {
        int remaining = Math.max(0, requestedRations);
        List<StorageSource> sources = findSources(level, radios);
        int removedNutrition = 0;
        for (FoodSlot food : foodSlots(sources)) {
            if (remaining <= 0) {
                break;
            }
            ItemStack current = food.source().stackInSlot(food.slot());
            FoodProperties properties = current.getFoodProperties(null);
            if (properties == null
                    || properties.nutrition() != food.nutrition()) {
                continue;
            }
            int requestedItems = Math.min(
                    current.getCount(),
                    requiredItems(remaining, food.nutrition())
            );
            ItemStack extracted = food.source().extract(
                    food.slot(),
                    requestedItems
            );
            if (extracted.isEmpty()) {
                continue;
            }
            int nutrition = saturatedMultiply(
                    extracted.getCount(),
                    food.nutrition()
            );
            removedNutrition = saturatedAdd(removedNutrition, nutrition);
            remaining -= nutrition;
            returnContainers(level, food.source(), extracted);
        }
        return new StockpileConsumption(
                summarize(sources),
                removedNutrition
        );
    }

    /**
     * Removes real 7.62x51 PointBlank rounds from the shared camp stores.
     * The physical container remains authoritative, just as it does for food.
     */
    static AmmunitionConsumption consumeMosinAmmunition(
            ServerLevel level,
            Collection<SettlementRadioStatus> radios,
            int requestedRounds
    ) {
        int remaining = Math.max(0, requestedRounds);
        Item ammunition = mosinAmmunition();
        List<StorageSource> sources = findSources(level, radios);
        if (ammunition == null || remaining == 0) {
            return new AmmunitionConsumption(summarize(sources), 0);
        }

        int removed = 0;
        for (StorageSource source : sources) {
            for (int slot = 0;
                    slot < source.slotCount() && remaining > 0;
                    slot++) {
                ItemStack current = source.stackInSlot(slot);
                if (!current.is(ammunition)) {
                    continue;
                }
                ItemStack extracted = source.extract(
                        slot,
                        Math.min(remaining, current.getCount())
                );
                removed += extracted.getCount();
                remaining -= extracted.getCount();
            }
            if (remaining <= 0) {
                break;
            }
        }
        return new AmmunitionConsumption(summarize(sources), removed);
    }

    private static List<StorageSource> findSources(
            ServerLevel level,
            Collection<SettlementRadioStatus> radios
    ) {
        List<SettlementRadioStatus> activeRadios = radios.stream()
                .filter(SettlementRadioStatus::contributesStockpile)
                .sorted(Comparator.comparing(
                        radio -> radio.campId().toString()
                ))
                .toList();
        List<StorageSource> sources = new ArrayList<>();
        Set<Long> visitedPositions = new HashSet<>();
        Set<IItemHandler> visitedHandlers = Collections.newSetFromMap(
                new IdentityHashMap<>()
        );

        for (SettlementRadioStatus radio : activeRadios) {
            collectCampSources(
                    level,
                    radio,
                    visitedPositions,
                    visitedHandlers,
                    sources
            );
        }
        sources.sort(Comparator.comparingLong(
                source -> source.position().asLong()
        ));
        return sources;
    }

    private static void collectCampSources(
            ServerLevel level,
            SettlementRadioStatus radio,
            Set<Long> visitedPositions,
            Set<IItemHandler> visitedHandlers,
            List<StorageSource> sources
    ) {
        BlockPos center = radio.campCenter();
        int radius = radio.campRadius();
        BlockPos minimum = center.offset(-radius, -radius, -radius);
        BlockPos maximum = center.offset(radius, radius, radius);
        double maximumDistanceSquared = (double) radius * radius;

        for (BlockPos position : BlockPos.betweenClosed(minimum, maximum)) {
            if (center.distSqr(position) > maximumDistanceSquared
                    || !visitedPositions.add(position.asLong())
                    || !level.hasChunk(
                            position.getX() >> 4,
                            position.getZ() >> 4
                    )) {
                continue;
            }
            BlockEntity blockEntity = level.getBlockEntity(position);
            if (blockEntity instanceof Container container) {
                sources.add(new ContainerSource(
                        position.immutable(),
                        blockEntity,
                        container
                ));
                continue;
            }

            IItemHandler handler = level.getCapability(
                    Capabilities.ItemHandler.BLOCK,
                    position,
                    null
            );
            if (handler != null && visitedHandlers.add(handler)) {
                sources.add(new ItemHandlerSource(
                        position.immutable(),
                        blockEntity,
                        handler
                ));
            }
        }
    }

    private static StockpileSnapshot summarize(
            Collection<StorageSource> sources
    ) {
        int rations = 0;
        int containers = 0;
        int mosinRounds = 0;
        Item ammunition = mosinAmmunition();
        for (StorageSource source : sources) {
            int sourceRations = 0;
            for (int slot = 0; slot < source.slotCount(); slot++) {
                ItemStack stack = source.stackInSlot(slot);
                if (ammunition != null && stack.is(ammunition)) {
                    mosinRounds = saturatedAdd(
                            mosinRounds,
                            stack.getCount()
                    );
                }
                FoodProperties properties = stack.getFoodProperties(null);
                if (properties != null && properties.nutrition() > 0) {
                    sourceRations = saturatedAdd(
                            sourceRations,
                            nutritionFor(properties, stack.getCount())
                    );
                }
            }
            if (sourceRations > 0) {
                containers++;
                rations = saturatedAdd(rations, sourceRations);
            }
        }
        return new StockpileSnapshot(rations, containers, mosinRounds);
    }

    private static Item mosinAmmunition() {
        Item item = BuiltInRegistries.ITEM.get(MOSIN_AMMUNITION_ID);
        return item == null || item == net.minecraft.world.item.Items.AIR
                ? null
                : item;
    }

    private static List<FoodSlot> foodSlots(
            Collection<StorageSource> sources
    ) {
        List<FoodSlot> foods = new ArrayList<>();
        for (StorageSource source : sources) {
            for (int slot = 0; slot < source.slotCount(); slot++) {
                ItemStack stack = source.stackInSlot(slot);
                FoodProperties properties = stack.getFoodProperties(null);
                if (properties != null && properties.nutrition() > 0) {
                    foods.add(new FoodSlot(
                            source,
                            slot,
                            properties.nutrition()
                    ));
                }
            }
        }
        foods.sort(Comparator.comparingInt(FoodSlot::nutrition)
                .thenComparingLong(food -> food.source().position().asLong())
                .thenComparingInt(FoodSlot::slot));
        return foods;
    }

    private static int requiredItems(int rations, int nutrition) {
        return (int) Math.min(
                Integer.MAX_VALUE,
                ((long) Math.max(0, rations) + nutrition - 1L) / nutrition
        );
    }

    private static int nutritionFor(
            FoodProperties properties,
            int itemCount
    ) {
        return saturatedMultiply(properties.nutrition(), itemCount);
    }

    private static void returnContainers(
            ServerLevel level,
            StorageSource source,
            ItemStack consumed
    ) {
        FoodProperties properties = consumed.getFoodProperties(null);
        if (properties == null) {
            return;
        }
        properties.usingConvertsTo().ifPresent(remainder -> {
            for (int count = 0; count < consumed.getCount(); count++) {
                ItemStack leftover = source.insert(remainder.copy());
                if (!leftover.isEmpty()) {
                    Block.popResource(
                            level,
                            source.position().above(),
                            leftover
                    );
                }
            }
        });
    }

    private static int saturatedAdd(int left, int right) {
        return (int) Math.min(
                Integer.MAX_VALUE,
                (long) Math.max(0, left) + Math.max(0, right)
        );
    }

    private static int saturatedMultiply(int left, int right) {
        return (int) Math.min(
                Integer.MAX_VALUE,
                (long) Math.max(0, left) * Math.max(0, right)
        );
    }

    private interface StorageSource {

        BlockPos position();

        int slotCount();

        ItemStack stackInSlot(int slot);

        ItemStack extract(int slot, int amount);

        ItemStack insert(ItemStack stack);
    }

    private record ContainerSource(
            BlockPos position,
            BlockEntity blockEntity,
            Container container
    ) implements StorageSource {

        @Override
        public int slotCount() {
            return container.getContainerSize();
        }

        @Override
        public ItemStack stackInSlot(int slot) {
            return container.getItem(slot);
        }

        @Override
        public ItemStack extract(int slot, int amount) {
            ItemStack extracted = container.removeItem(slot, amount);
            if (!extracted.isEmpty()) {
                container.setChanged();
                blockEntity.setChanged();
            }
            return extracted;
        }

        @Override
        public ItemStack insert(ItemStack stack) {
            ItemStack remaining = stack.copy();
            for (int slot = 0;
                    slot < container.getContainerSize() && !remaining.isEmpty();
                    slot++) {
                ItemStack current = container.getItem(slot);
                if (current.isEmpty()) {
                    if (!container.canPlaceItem(slot, remaining)) {
                        continue;
                    }
                    int placed = Math.min(
                            remaining.getCount(),
                            container.getMaxStackSize(remaining)
                    );
                    container.setItem(slot, remaining.copyWithCount(placed));
                    remaining.shrink(placed);
                } else if (ItemStack.isSameItemSameComponents(
                        current,
                        remaining
                ) && container.canPlaceItem(slot, remaining)) {
                    int space = Math.min(
                            container.getMaxStackSize(current),
                            current.getMaxStackSize()
                    ) - current.getCount();
                    if (space <= 0) {
                        continue;
                    }
                    current.grow(Math.min(space, remaining.getCount()));
                    remaining.shrink(Math.min(space, remaining.getCount()));
                    container.setItem(slot, current);
                }
            }
            if (remaining.getCount() != stack.getCount()) {
                container.setChanged();
                blockEntity.setChanged();
            }
            return remaining;
        }
    }

    private record ItemHandlerSource(
            BlockPos position,
            BlockEntity blockEntity,
            IItemHandler handler
    ) implements StorageSource {

        @Override
        public int slotCount() {
            return handler.getSlots();
        }

        @Override
        public ItemStack stackInSlot(int slot) {
            return handler.getStackInSlot(slot);
        }

        @Override
        public ItemStack extract(int slot, int amount) {
            ItemStack extracted = handler.extractItem(slot, amount, false);
            if (!extracted.isEmpty() && blockEntity != null) {
                blockEntity.setChanged();
            }
            return extracted;
        }

        @Override
        public ItemStack insert(ItemStack stack) {
            ItemStack remaining = ItemHandlerHelper.insertItemStacked(
                    handler,
                    stack,
                    false
            );
            if (remaining.getCount() != stack.getCount()
                    && blockEntity != null) {
                blockEntity.setChanged();
            }
            return remaining;
        }
    }

    record StockpileSnapshot(
            int rations,
            int containerCount,
            int mosinRounds
    ) {

        StockpileSnapshot(int rations, int containerCount) {
            this(rations, containerCount, 0);
        }
    }

    record StockpileConsumption(
            StockpileSnapshot remainingStockpile,
            int nutritionRemoved
    ) {
    }

    record AmmunitionConsumption(
            StockpileSnapshot remainingStockpile,
            int roundsRemoved
    ) {
    }

    private record FoodSlot(
            StorageSource source,
            int slot,
            int nutrition
    ) {
    }

    private SettlementStockpile() {
    }
}
