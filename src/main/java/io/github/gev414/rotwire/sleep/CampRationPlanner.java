package io.github.gev414.rotwire.sleep;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class CampRationPlanner {

    static Optional<Selection> select(
            List<FoodStack> foods,
            int nutritionThreshold
    ) {
        int target = nutritionThreshold + 1;
        if (target <= 0) {
            return Optional.of(new Selection(0, List.of()));
        }

        Plan[] belowTarget = new Plan[target];
        belowTarget[0] = new Plan(0, 0, new int[foods.size()]);
        Plan best = null;

        for (int foodIndex = 0; foodIndex < foods.size(); foodIndex++) {
            FoodStack food = foods.get(foodIndex);
            if (food.nutrition() <= 0 || food.count() <= 0) {
                continue;
            }

            int usefulCount = Math.min(food.count(), target);
            for (int unit = 0; unit < usefulCount; unit++) {
                for (int nutrition = target - 1;
                        nutrition >= 0;
                        nutrition--) {
                    Plan existing = belowTarget[nutrition];
                    if (existing == null) {
                        continue;
                    }

                    int newNutrition =
                            existing.nutrition + food.nutrition();
                    int[] amounts = existing.amounts.clone();
                    amounts[foodIndex]++;
                    Plan candidate = new Plan(
                            newNutrition,
                            existing.itemCount + 1,
                            amounts
                    );

                    if (newNutrition >= target) {
                        if (betterSatisfied(candidate, best)) {
                            best = candidate;
                        }
                    } else {
                        Plan current = belowTarget[newNutrition];
                        if (current == null
                                || candidate.itemCount
                                < current.itemCount) {
                            belowTarget[newNutrition] = candidate;
                        }
                    }
                }
            }
        }

        if (best == null) {
            return Optional.empty();
        }

        List<SlotAmount> amounts = new ArrayList<>();
        for (int index = 0; index < best.amounts.length; index++) {
            if (best.amounts[index] > 0) {
                amounts.add(new SlotAmount(
                        foods.get(index).slot(),
                        best.amounts[index]
                ));
            }
        }
        return Optional.of(new Selection(best.nutrition, amounts));
    }

    private static boolean betterSatisfied(Plan candidate, Plan current) {
        return current == null
                || candidate.nutrition < current.nutrition
                || candidate.nutrition == current.nutrition
                && candidate.itemCount < current.itemCount;
    }

    record FoodStack(int slot, int nutrition, int count) {
    }

    record SlotAmount(int slot, int amount) {
    }

    record Selection(int nutrition, List<SlotAmount> amounts) {

        Selection {
            amounts = List.copyOf(amounts);
        }
    }

    private record Plan(int nutrition, int itemCount, int[] amounts) {
    }

    private CampRationPlanner() {
    }
}
