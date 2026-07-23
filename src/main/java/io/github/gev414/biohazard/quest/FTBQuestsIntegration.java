package io.github.gev414.biohazard.quest;

import dev.architectury.event.EventResult;
import dev.ftb.mods.ftbquests.events.CustomRewardEvent;
import dev.ftb.mods.ftbquests.events.CustomTaskEvent;
import dev.ftb.mods.ftbquests.quest.reward.CustomReward;
import dev.ftb.mods.ftbquests.quest.task.CustomTask;
import io.github.gev414.biohazard.city.CityZoneKey;
import io.github.gev414.biohazard.city.CityZoneManager;
import io.github.gev414.biohazard.quest.delivery.DeliveryCategory;
import io.github.gev414.biohazard.quest.delivery.DeliveryManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;
import java.util.Set;

public final class FTBQuestsIntegration {

    public static final String ACCEPT_TAG = "biohazard_radio_accept";
    public static final String COMPLETE_TAG = "biohazard_radio_complete";
    public static final String CITY_OPERATION_TAG =
            "biohazard_city_operation";
    public static final String CITY_BUILDING_COUNT_PREFIX =
            "biohazard_city_buildings_";
    public static final String DELIVERY_TAG = "biohazard_radio_delivery";
    public static final String CHOICE_DELIVERY_TAG =
            "biohazard_radio_choice_delivery";
    public static final String MANIFEST_PREFIX = "biohazard_manifest_";
    public static final String CHOICE_COUNT_PREFIX = "biohazard_choice_count_";

    private static boolean initialized;

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        CustomTaskEvent.EVENT.register(FTBQuestsIntegration::configureTask);
        CustomRewardEvent.EVENT.register(FTBQuestsIntegration::claimReward);
    }

    private static EventResult configureTask(CustomTaskEvent event) {
        CustomTask task = event.getTask();
        if (!task.getQuestFile().isServerSide()) {
            return EventResult.pass();
        }

        if (task.hasTag(ACCEPT_TAG)) {
            configureButton(task, FTBQuestsIntegration::acceptContract);
        } else if (task.hasTag(COMPLETE_TAG)) {
            configureButton(task, FTBQuestsIntegration::completeContract);
        } else if (task.hasTag(CITY_OPERATION_TAG)) {
            configureCityOperation(task);
        }
        return EventResult.pass();
    }

    private static void configureButton(
            CustomTask task,
            CustomTask.Check check
    ) {
        task.setMaxProgress(1L);
        task.setCheckTimer(0);
        task.setEnableButton(true);
        task.setCheck(check);
    }

    private static void acceptContract(
            CustomTask.Data task,
            ServerPlayer player
    ) {
        Optional<BlockPos> transmitter = requireConnectedRadio(player);
        if (transmitter.isEmpty()) {
            return;
        }

        var cityTasks = task.task()
                .getQuest()
                .getTasksAsList()
                .stream()
                .filter(CustomTask.class::isInstance)
                .map(CustomTask.class::cast)
                .filter(candidate -> candidate.hasTag(
                        CITY_OPERATION_TAG
                ))
                .toList();
        if (!cityTasks.isEmpty()) {
            Optional<CityZoneKey> zone = RadioNetwork.cityZone(
                    player.level(),
                    transmitter.get()
            );
            if (zone.isEmpty()) {
                player.sendSystemMessage(Component.translatable(
                        "message.biohazard.city.operation_requires_zone"
                ));
                return;
            }

            for (CustomTask cityTask : cityTasks) {
                if (!CityZoneManager.bindOperation(
                        player.serverLevel().getServer(),
                        task.teamData().getTeamId(),
                        cityTask.getId(),
                        zone.get()
                )) {
                    player.sendSystemMessage(Component.translatable(
                            "message.biohazard.city.operation_requires_zone"
                    ));
                    return;
                }
            }
        }

        task.setProgress(task.task().getMaxProgress());
        player.sendSystemMessage(Component.translatable(
                "message.biohazard.radio.contract_accepted"
        ));
    }

    private static void completeContract(
            CustomTask.Data task,
            ServerPlayer player
    ) {
        if (requireConnectedRadio(player).isEmpty()) {
            return;
        }
        if (RadioSubmission.completeTurnIn(task, player)) {
            player.sendSystemMessage(Component.translatable(
                    "message.biohazard.radio.contract_transmitted"
            ));
        }
    }

    private static void configureCityOperation(CustomTask task) {
        long requiredBuildings = suffix(
                task.getTags(),
                CITY_BUILDING_COUNT_PREFIX
        ).flatMap(value -> {
            try {
                return Optional.of(Long.parseLong(value));
            } catch (NumberFormatException exception) {
                return Optional.empty();
            }
        }).filter(value -> value > 0L && value <= 1_000L).orElse(5L);

        task.setMaxProgress(requiredBuildings);
        task.setCheckTimer(20);
        task.setEnableButton(false);
        task.setCheck(FTBQuestsIntegration::checkCityOperation);
    }

    private static void checkCityOperation(
            CustomTask.Data task,
            ServerPlayer player
    ) {
        boolean accepted = task.task()
                .getQuest()
                .getTasksAsList()
                .stream()
                .filter(CustomTask.class::isInstance)
                .map(CustomTask.class::cast)
                .filter(candidate -> candidate.hasTag(ACCEPT_TAG))
                .anyMatch(candidate -> task.teamData().getProgress(
                        candidate
                ) >= candidate.getMaxProgress());
        if (!accepted) {
            task.setProgress(0L);
            return;
        }

        long progress = CityZoneManager.operationProgress(
                player.serverLevel().getServer(),
                task.teamData().getTeamId(),
                task.task().getId()
        );
        task.setProgress(Math.min(
                task.task().getMaxProgress(),
                progress
        ));
    }

    private static Optional<BlockPos> requireConnectedRadio(
            ServerPlayer player
    ) {
        Optional<BlockPos> transmitter =
                RadioNetwork.findConnectedTransmitter(player);
        if (transmitter.isPresent()) {
            return transmitter;
        }
        player.sendSystemMessage(Component.translatable(
                "message.biohazard.radio.out_of_range"
        ));
        return Optional.empty();
    }

    private static EventResult claimReward(CustomRewardEvent event) {
        CustomReward reward = event.getReward();
        boolean choiceDelivery = reward.hasTag(CHOICE_DELIVERY_TAG);
        if (!choiceDelivery && !reward.hasTag(DELIVERY_TAG)) {
            return EventResult.pass();
        }

        Optional<String> manifest = suffix(
                reward.getTags(),
                MANIFEST_PREFIX
        );
        if (manifest.isEmpty()) {
            event.getPlayer().sendSystemMessage(Component.translatable(
                    "message.biohazard.delivery.invalid_manifest"
            ));
            return EventResult.pass();
        }

        if (choiceDelivery) {
            DeliveryManager.scheduleChoice(
                    event.getPlayer(),
                    reward.getId(),
                    manifest.get(),
                    DeliveryCategory.fromTags(reward.getTags()),
                    choiceCount(reward.getTags())
            );
        } else {
            DeliveryManager.schedule(
                    event.getPlayer(),
                    reward.getId(),
                    manifest.get(),
                    DeliveryCategory.fromTags(reward.getTags())
            );
        }
        return EventResult.pass();
    }

    private static int choiceCount(Set<String> tags) {
        return suffix(tags, CHOICE_COUNT_PREFIX)
                .flatMap(value -> {
                    try {
                        return Optional.of(Integer.parseInt(value));
                    } catch (NumberFormatException exception) {
                        return Optional.empty();
                    }
                })
                .filter(count -> count > 0 && count <= 9)
                .orElse(3);
    }

    private static Optional<String> suffix(
            Set<String> tags,
            String prefix
    ) {
        return tags.stream()
                .filter(tag -> tag.startsWith(prefix))
                .map(tag -> tag.substring(prefix.length()))
                .filter(value -> !value.isBlank())
                .findFirst();
    }

    private FTBQuestsIntegration() {
    }
}
