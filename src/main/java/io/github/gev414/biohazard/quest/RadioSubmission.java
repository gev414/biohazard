package io.github.gev414.biohazard.quest;

import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.CustomTask;
import dev.ftb.mods.ftbquests.quest.task.ItemTask;
import dev.ftb.mods.ftbquests.quest.task.Task;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class RadioSubmission {

    static final String SUBMIT_ITEM_TAG = "biohazard_radio_submit";

    static boolean completeTurnIn(
            CustomTask.Data customTask,
            ServerPlayer player
    ) {
        TeamData teamData = customTask.teamData();
        Task completionTask = customTask.task();
        List<Task> questTasks = completionTask.getQuest().getTasksAsList();

        for (Task task : questTasks) {
            if (task == completionTask
                    || isRadioSubmission(task)
                    || task.isOptionalForProgression(teamData)) {
                continue;
            }
            if (!teamData.isCompleted(task)) {
                player.sendSystemMessage(Component.translatable(
                        "message.biohazard.radio.objectives_incomplete"
                ));
                return false;
            }
        }

        if (!consumeTaggedItemTasksAtomically(questTasks, teamData, player)) {
            return false;
        }

        customTask.setProgress(customTask.task().getMaxProgress());
        return true;
    }

    private static boolean isRadioSubmission(Task task) {
        return task instanceof ItemTask itemTask
                && itemTask.hasTag(SUBMIT_ITEM_TAG);
    }

    private static boolean consumeTaggedItemTasksAtomically(
            List<Task> questTasks,
            TeamData teamData,
            ServerPlayer player
    ) {
        List<ItemTask> submissions = questTasks.stream()
                .filter(ItemTask.class::isInstance)
                .map(ItemTask.class::cast)
                .filter(task -> task.hasTag(SUBMIT_ITEM_TAG))
                .toList();
        if (submissions.isEmpty()) {
            return true;
        }

        List<ItemStack> inventory = player.getInventory().items;
        int[] remainingBySlot = new int[inventory.size()];
        for (int slot = 0; slot < inventory.size(); slot++) {
            remainingBySlot[slot] = inventory.get(slot).getCount();
        }

        List<Allocation> allocations = new ArrayList<>();
        Map<ItemTask, Long> totals = new LinkedHashMap<>();

        for (ItemTask task : submissions) {
            // Radio submissions remain visible in FTB Quests, but the radio
            // is the sole authority that consumes their required items.
            long required = task.getMaxProgress();
            long allocated = 0L;

            for (int slot = 0;
                 slot < inventory.size() && allocated < required;
                 slot++) {
                ItemStack stack = inventory.get(slot);
                if (remainingBySlot[slot] <= 0 || !task.test(stack)) {
                    continue;
                }

                int amount = (int) Math.min(
                        remainingBySlot[slot],
                        required - allocated
                );
                remainingBySlot[slot] -= amount;
                allocated += amount;
                allocations.add(new Allocation(task, slot, amount));
            }

            if (allocated < required) {
                player.sendSystemMessage(Component.translatable(
                        "message.biohazard.radio.missing_submission",
                        task.getTitle(),
                        required - allocated
                ));
                return false;
            }
            long missingProgress = Math.max(
                    0L,
                    task.getMaxProgress() - teamData.getProgress(task)
            );
            if (missingProgress > 0L) {
                totals.put(task, missingProgress);
            }
        }

        for (Allocation allocation : allocations) {
            inventory.get(allocation.slot()).shrink(allocation.amount());
        }
        totals.forEach(teamData::addProgress);
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        return true;
    }

    private record Allocation(ItemTask task, int slot, int amount) {
    }

    private RadioSubmission() {
    }
}
