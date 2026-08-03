package io.github.gev414.rotwire.sleep;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Finds food in inventory-capable blocks inside a campsite. Both vanilla
 * {@link Container} block entities and NeoForge block item handlers count.
 */
final class CampContainerSupplies {

    static boolean tryConsumeCampRations(
            ServerLevel level,
            Player player,
            BlockPos origin,
            int radius,
            int nutritionThreshold
    ) {
        List<StorageSource> sources = findSources(level, origin, radius);
        List<FoodCandidate> candidates = inspectFood(sources, player);
        Optional<CampRationPlanner.Selection> selection =
                CampRationPlanner.select(
                        plannerFoods(candidates),
                        nutritionThreshold
                );
        return selection.isPresent()
                && consume(
                        level,
                        player,
                        candidates,
                        selection.get()
                );
    }

    static boolean hasCampRations(
            ServerLevel level,
            Player player,
            BlockPos origin,
            int radius,
            int nutritionThreshold
    ) {
        return inspectCampSupplies(
                level,
                player,
                origin,
                radius,
                nutritionThreshold
        ).rationReady();
    }

    static CampSupplyStatus inspectCampSupplies(
            ServerLevel level,
            @Nullable Player player,
            BlockPos origin,
            int radius,
            int nutritionThreshold
    ) {
        List<StorageSource> sources = findSources(level, origin, radius);
        List<FoodCandidate> candidates = inspectFood(sources, player);
        List<CampRationPlanner.FoodStack> foods = plannerFoods(candidates);
        return new CampSupplyStatus(
                !sources.isEmpty(),
                CampRationPlanner.select(
                        foods,
                        nutritionThreshold
                ).isPresent(),
                totalNutrition(foods)
        );
    }

    private static List<StorageSource> findSources(
            ServerLevel level,
            BlockPos origin,
            int radius
    ) {
        List<StorageSource> sources = new ArrayList<>();
        BlockPos minimum = origin.offset(-radius, -radius, -radius);
        BlockPos maximum = origin.offset(radius, radius, radius);
        double maximumDistanceSquared = (double) radius * radius;

        for (BlockPos mutable : BlockPos.betweenClosed(minimum, maximum)) {
            if (origin.distSqr(mutable) > maximumDistanceSquared
                    || !level.hasChunk(
                            mutable.getX() >> 4,
                            mutable.getZ() >> 4
                    )) {
                continue;
            }

            BlockPos position = mutable.immutable();
            BlockEntity blockEntity = level.getBlockEntity(position);
            if (blockEntity == null) {
                continue;
            }
            if (blockEntity instanceof Container container) {
                sources.add(new ContainerSource(
                        position,
                        origin.distSqr(position),
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
            if (handler != null) {
                sources.add(new ItemHandlerSource(
                        position,
                        origin.distSqr(position),
                        blockEntity,
                        handler
                ));
            }
        }

        sources.sort(Comparator
                .comparingDouble(StorageSource::distanceSquared)
                .thenComparingLong(source -> source.position().asLong()));
        return sources;
    }

    private static List<FoodCandidate> inspectFood(
            List<StorageSource> sources,
            @Nullable Player player
    ) {
        List<FoodCandidate> foods = new ArrayList<>();
        for (StorageSource source : sources) {
            for (int slot = 0; slot < source.slotCount(); slot++) {
                ItemStack stack = source.stackInSlot(slot);
                FoodProperties properties = stack.getFoodProperties(player);
                if (properties == null || properties.nutrition() <= 0) {
                    continue;
                }
                foods.add(new FoodCandidate(
                        foods.size(),
                        source,
                        slot,
                        properties.nutrition(),
                        stack.getCount()
                ));
            }
        }
        return foods;
    }

    private static List<CampRationPlanner.FoodStack> plannerFoods(
            List<FoodCandidate> candidates
    ) {
        return candidates.stream()
                .map(candidate -> new CampRationPlanner.FoodStack(
                        candidate.id(),
                        candidate.nutrition(),
                        candidate.count()
                ))
                .toList();
    }

    private static boolean consume(
            ServerLevel level,
            Player player,
            List<FoodCandidate> candidates,
            CampRationPlanner.Selection selection
    ) {
        for (CampRationPlanner.SlotAmount amount : selection.amounts()) {
            FoodCandidate candidate = candidateFor(
                    candidates,
                    amount.slot()
            );
            if (candidate == null
                    || !candidate.matches(player, amount.amount())
                    || !candidate.source().canExtract(
                            candidate.slot(),
                            amount.amount()
                    )) {
                return false;
            }
        }

        List<ConsumedStack> consumed = new ArrayList<>();
        for (CampRationPlanner.SlotAmount amount : selection.amounts()) {
            FoodCandidate candidate = candidateFor(
                    candidates,
                    amount.slot()
            );
            if (candidate == null) {
                restore(level, consumed);
                return false;
            }
            ItemStack extracted = candidate.source().extract(
                    candidate.slot(),
                    amount.amount()
            );
            if (extracted.getCount() != amount.amount()) {
                if (!extracted.isEmpty()) {
                    consumed.add(new ConsumedStack(
                            candidate.source(),
                            extracted
                    ));
                }
                restore(level, consumed);
                return false;
            }
            consumed.add(new ConsumedStack(
                    candidate.source(),
                    extracted
            ));
        }

        for (ConsumedStack entry : consumed) {
            returnFoodContainer(level, player, entry);
        }
        return true;
    }

    private static FoodCandidate candidateFor(
            List<FoodCandidate> candidates,
            int id
    ) {
        return id >= 0 && id < candidates.size()
                ? candidates.get(id)
                : null;
    }

    private static void restore(
            ServerLevel level,
            List<ConsumedStack> consumed
    ) {
        for (ConsumedStack entry : consumed) {
            insertOrDrop(level, entry.source(), entry.stack());
        }
    }

    private static void returnFoodContainer(
            ServerLevel level,
            Player player,
            ConsumedStack consumed
    ) {
        FoodProperties properties = consumed.stack().getFoodProperties(player);
        if (properties == null) {
            return;
        }
        properties.usingConvertsTo().ifPresent(remainder -> {
            for (int count = 0;
                    count < consumed.stack().getCount();
                    count++) {
                insertOrDrop(
                        level,
                        consumed.source(),
                        remainder.copy()
                );
            }
        });
    }

    private static void insertOrDrop(
            ServerLevel level,
            StorageSource source,
            ItemStack stack
    ) {
        ItemStack leftover = source.insert(stack);
        if (!leftover.isEmpty()) {
            Block.popResource(
                    level,
                    source.position().above(),
                    leftover
            );
        }
    }

    private static int totalNutrition(
            List<CampRationPlanner.FoodStack> foods
    ) {
        long total = 0L;
        for (CampRationPlanner.FoodStack food : foods) {
            total += (long) food.nutrition() * food.count();
            if (total >= Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            }
        }
        return (int) total;
    }

    record CampSupplyStatus(
            boolean containerPresent,
            boolean rationReady,
            int availableNutrition
    ) {
    }

    private record FoodCandidate(
            int id,
            StorageSource source,
            int slot,
            int nutrition,
            int count
    ) {

        boolean matches(@Nullable Player player, int requestedCount) {
            ItemStack current = source.stackInSlot(slot);
            FoodProperties properties = current.getFoodProperties(player);
            return requestedCount > 0
                    && current.getCount() >= requestedCount
                    && properties != null
                    && properties.nutrition() == nutrition;
        }
    }

    private record ConsumedStack(
            StorageSource source,
            ItemStack stack
    ) {
    }

    private interface StorageSource {

        BlockPos position();

        double distanceSquared();

        int slotCount();

        ItemStack stackInSlot(int slot);

        boolean canExtract(int slot, int amount);

        ItemStack extract(int slot, int amount);

        ItemStack insert(ItemStack stack);
    }

    private record ContainerSource(
            BlockPos position,
            double distanceSquared,
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
        public boolean canExtract(int slot, int amount) {
            return container.getItem(slot).getCount() >= amount;
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
                    slot < container.getContainerSize()
                            && !remaining.isEmpty();
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
                    container.setItem(
                            slot,
                            remaining.copyWithCount(placed)
                    );
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
                    int placed = Math.min(space, remaining.getCount());
                    current.grow(placed);
                    remaining.shrink(placed);
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
            double distanceSquared,
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
        public boolean canExtract(int slot, int amount) {
            return handler.extractItem(slot, amount, true).getCount()
                    >= amount;
        }

        @Override
        public ItemStack extract(int slot, int amount) {
            ItemStack extracted = handler.extractItem(slot, amount, false);
            if (!extracted.isEmpty()) {
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
            if (remaining.getCount() != stack.getCount()) {
                blockEntity.setChanged();
            }
            return remaining;
        }
    }

    private CampContainerSupplies() {
    }
}
