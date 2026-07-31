package io.github.gev414.rotwire.sleep;

import com.tiviacz.travelersbackpack.blockentity.BackpackBlockEntity;
import com.tiviacz.travelersbackpack.blocks.SleepingBagBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Isolated so servers without Traveler's Backpack never load its classes.
 */
final class TravelersBackpackSleepIntegration {

    static boolean isSleepingBag(BlockState state) {
        return state.getBlock() instanceof SleepingBagBlock;
    }

    static boolean tryConsumeCampRations(
            ServerLevel level,
            ServerPlayer player,
            BlockPos origin,
            int radius,
            int nutritionThreshold
    ) {
        List<BackpackCandidate> candidates =
                findBackpacks(level, origin, radius);
        for (BackpackCandidate candidate : candidates) {
            ItemStackHandler storage =
                    candidate.backpack().getWrapper().getStorage();
            List<CampRationPlanner.FoodStack> foods =
                    inspectFood(storage, player);
            Optional<CampRationPlanner.Selection> selection =
                    CampRationPlanner.select(foods, nutritionThreshold);
            if (selection.isPresent()
                    && consume(
                            level,
                            player,
                            candidate,
                            storage,
                            foods,
                            selection.get()
                    )) {
                return true;
            }
        }
        return false;
    }

    static boolean hasCampRations(
            ServerLevel level,
            ServerPlayer player,
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
            ServerPlayer player,
            BlockPos origin,
            int radius,
            int nutritionThreshold
    ) {
        boolean backpackPresent = false;
        boolean rationReady = false;
        int availableNutrition = 0;

        for (BackpackCandidate candidate
                : findBackpacks(level, origin, radius)) {
            backpackPresent = true;
            ItemStackHandler storage =
                    candidate.backpack().getWrapper().getStorage();
            List<CampRationPlanner.FoodStack> foods =
                    inspectFood(storage, player);
            availableNutrition = Math.max(
                    availableNutrition,
                    totalNutrition(foods)
            );
            rationReady |= CampRationPlanner.select(
                    foods,
                    nutritionThreshold
            ).isPresent();
        }

        return new CampSupplyStatus(
                backpackPresent,
                rationReady,
                availableNutrition
        );
    }

    private static List<BackpackCandidate> findBackpacks(
            ServerLevel level,
            BlockPos origin,
            int radius
    ) {
        List<BackpackCandidate> candidates = new ArrayList<>();
        BlockPos minimum = origin.offset(-radius, -radius, -radius);
        BlockPos maximum = origin.offset(radius, radius, radius);
        double maximumDistanceSquared = (double) radius * radius;

        for (BlockPos mutable : BlockPos.betweenClosed(minimum, maximum)) {
            if (origin.distSqr(mutable) > maximumDistanceSquared) {
                continue;
            }
            if (level.getBlockEntity(mutable)
                    instanceof BackpackBlockEntity backpack) {
                BlockPos position = mutable.immutable();
                candidates.add(new BackpackCandidate(
                        position,
                        origin.distSqr(position),
                        backpack
                ));
            }
        }

        candidates.sort(Comparator
                .comparingDouble(BackpackCandidate::distanceSquared)
                .thenComparingLong(
                        candidate -> candidate.position().asLong()
                ));
        return candidates;
    }

    private static List<CampRationPlanner.FoodStack> inspectFood(
            ItemStackHandler storage,
            ServerPlayer player
    ) {
        List<CampRationPlanner.FoodStack> foods = new ArrayList<>();
        for (int slot = 0; slot < storage.getSlots(); slot++) {
            ItemStack stack = storage.getStackInSlot(slot);
            FoodProperties properties = stack.getFoodProperties(player);
            if (properties != null && properties.nutrition() > 0) {
                foods.add(new CampRationPlanner.FoodStack(
                        slot,
                        properties.nutrition(),
                        stack.getCount()
                ));
            }
        }
        return foods;
    }

    private static boolean consume(
            ServerLevel level,
            ServerPlayer player,
            BackpackCandidate candidate,
            ItemStackHandler storage,
            List<CampRationPlanner.FoodStack> foods,
            CampRationPlanner.Selection selection
    ) {
        for (CampRationPlanner.SlotAmount amount : selection.amounts()) {
            CampRationPlanner.FoodStack planned =
                    foodForSlot(foods, amount.slot());
            ItemStack current = storage.getStackInSlot(amount.slot());
            FoodProperties properties = current.getFoodProperties(player);
            if (planned == null
                    || current.getCount() < amount.amount()
                    || properties == null
                    || properties.nutrition() != planned.nutrition()) {
                return false;
            }
        }

        for (CampRationPlanner.SlotAmount amount : selection.amounts()) {
            ItemStack extracted = storage.extractItem(
                    amount.slot(),
                    amount.amount(),
                    false
            );
            FoodProperties properties =
                    extracted.getFoodProperties(player);
            if (properties == null) {
                continue;
            }
            properties.usingConvertsTo().ifPresent(remainder -> {
                for (int count = 0; count < extracted.getCount(); count++) {
                    ItemStack leftover = ItemHandlerHelper.insertItemStacked(
                            storage,
                            remainder.copy(),
                            false
                    );
                    if (!leftover.isEmpty()) {
                        net.minecraft.world.level.block.Block.popResource(
                                level,
                                candidate.position().above(),
                                leftover
                        );
                    }
                }
            });
        }
        candidate.backpack().setChanged();
        return true;
    }

    private static CampRationPlanner.FoodStack foodForSlot(
            List<CampRationPlanner.FoodStack> foods,
            int slot
    ) {
        for (CampRationPlanner.FoodStack food : foods) {
            if (food.slot() == slot) {
                return food;
            }
        }
        return null;
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
            boolean backpackPresent,
            boolean rationReady,
            int availableNutrition
    ) {
    }

    private record BackpackCandidate(
            BlockPos position,
            double distanceSquared,
            BackpackBlockEntity backpack
    ) {
    }

    private TravelersBackpackSleepIntegration() {
    }
}
