package io.github.gev414.biohazard.quest.delivery;

import io.github.gev414.biohazard.Biohazard;
import io.github.gev414.biohazard.network.CourierChoiceOpenPayload;
import io.github.gev414.biohazard.quest.RadioNetwork;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public final class DeliveryManager {

    private static final int UPDATE_INTERVAL_TICKS = 20;
    private static int ticksUntilUpdate;

    public static void schedule(
            ServerPlayer player,
            long rewardId,
            String manifest,
            DeliveryCategory category
    ) {
        List<ItemStack> generated = generateManifest(player, manifest);
        if (generated.isEmpty()) {
            Biohazard.LOGGER.error(
                    "Radio delivery manifest '{}' generated no items for {}",
                    manifest,
                    player.getGameProfile().getName()
            );
            player.sendSystemMessage(Component.translatable(
                    "message.biohazard.delivery.invalid_manifest"
            ));
            return;
        }

        MinecraftServer server = player.getServer();
        long orderedAt = server.overworld().getGameTime();
        long readyAt = orderedAt + category.delayTicks();
        DeliverySavedData.get(server).add(new RadioDelivery(
                UUID.randomUUID(),
                player.getUUID(),
                rewardId,
                manifest,
                category,
                orderedAt,
                readyAt,
                DeliveryKind.ITEMS,
                generated,
                false
        ));

        notifyScheduled(player, category, orderedAt, readyAt);
    }

    public static void scheduleChoice(
            ServerPlayer player,
            long rewardId,
            String manifest,
            DeliveryCategory category,
            int optionCount
    ) {
        List<ItemStack> options = generateChoiceOptions(
                player,
                manifest,
                optionCount
        );
        if (options.isEmpty()) {
            Biohazard.LOGGER.error(
                    "Radio choice manifest '{}' generated no options for {}",
                    manifest,
                    player.getGameProfile().getName()
            );
            player.sendSystemMessage(Component.translatable(
                    "message.biohazard.delivery.invalid_manifest"
            ));
            return;
        }

        MinecraftServer server = player.getServer();
        long orderedAt = server.overworld().getGameTime();
        long readyAt = orderedAt + category.delayTicks();
        DeliverySavedData.get(server).add(new RadioDelivery(
                UUID.randomUUID(),
                player.getUUID(),
                rewardId,
                manifest,
                category,
                orderedAt,
                readyAt,
                DeliveryKind.CHOICE,
                options,
                false
        ));

        notifyScheduled(player, category, orderedAt, readyAt);
    }

    private static void notifyScheduled(
            ServerPlayer player,
            DeliveryCategory category,
            long orderedAt,
            long readyAt
    ) {
        long seconds = Math.max(0L, (readyAt - orderedAt + 19L) / 20L);
        player.sendSystemMessage(Component.translatable(
                "message.biohazard.delivery.scheduled",
                category.serializedName(),
                seconds
        ));
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        if (ticksUntilUpdate > 0) {
            ticksUntilUpdate--;
            return;
        }
        ticksUntilUpdate = UPDATE_INTERVAL_TICKS - 1;

        MinecraftServer server = event.getServer();
        long gameTime = server.overworld().getGameTime();
        DeliverySavedData data = DeliverySavedData.get(server);
        boolean changed = false;

        for (RadioDelivery delivery : data.deliveries()) {
            if (!delivery.isReady(gameTime) || delivery.notified()) {
                continue;
            }
            ServerPlayer owner = server.getPlayerList()
                    .getPlayer(delivery.owner());
            if (owner == null) {
                continue;
            }
            delivery.markNotified();
            changed = true;
            owner.sendSystemMessage(Component.translatable(
                    "message.biohazard.delivery.ready",
                    delivery.category().serializedName()
            ));
        }

        if (changed) {
            data.setDirty();
        }
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        ticksUntilUpdate = 0;
    }

    public static void collectReady(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        long gameTime = server.overworld().getGameTime();
        DeliverySavedData data = DeliverySavedData.get(server);
        Iterator<RadioDelivery> iterator = data.deliveries().iterator();
        int collected = 0;
        boolean changed = false;

        while (iterator.hasNext()) {
            RadioDelivery delivery = iterator.next();
            if (!delivery.owner().equals(player.getUUID())
                    || !delivery.isReady(gameTime)
                    || delivery.isChoice()) {
                continue;
            }

            List<ItemStack> remaining = new ArrayList<>();
            for (ItemStack savedStack : delivery.items()) {
                ItemStack toInsert = savedStack.copy();
                player.getInventory().add(toInsert);
                if (!toInsert.isEmpty()) {
                    remaining.add(toInsert);
                }
            }

            changed = true;
            if (remaining.isEmpty()) {
                iterator.remove();
                collected++;
            } else {
                delivery.replaceItems(remaining);
            }
        }

        if (changed) {
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
            data.setDirty();
        }
        if (collected > 0) {
            player.sendSystemMessage(Component.translatable(
                    "message.biohazard.delivery.collected",
                    collected
            ));
        }
    }

    public static boolean openReadyChoice(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        long gameTime = server.overworld().getGameTime();

        for (RadioDelivery delivery
                : DeliverySavedData.get(server).deliveries()) {
            if (!delivery.owner().equals(player.getUUID())
                    || !delivery.isReady(gameTime)
                    || !delivery.isChoice()) {
                continue;
            }

            PacketDistributor.sendToPlayer(
                    player,
                    new CourierChoiceOpenPayload(
                            delivery.id().toString(),
                            delivery.items().stream()
                                    .map(stack -> stack.getItemHolder()
                                            .unwrapKey()
                                            .orElseThrow()
                                            .location()
                                            .toString())
                                    .toList()
                    )
            );
            return true;
        }
        return false;
    }

    public static void selectChoice(
            ServerPlayer player,
            String deliveryId,
            int optionIndex
    ) {
        if (RadioNetwork.findConnectedTransmitter(player).isEmpty()) {
            player.sendSystemMessage(Component.translatable(
                    "message.biohazard.radio.out_of_range"
            ));
            return;
        }

        UUID id;
        try {
            id = UUID.fromString(deliveryId);
        } catch (IllegalArgumentException exception) {
            return;
        }

        MinecraftServer server = player.getServer();
        long gameTime = server.overworld().getGameTime();
        DeliverySavedData data = DeliverySavedData.get(server);
        for (RadioDelivery delivery : data.deliveries()) {
            if (!delivery.id().equals(id)
                    || !delivery.owner().equals(player.getUUID())
                    || !delivery.isChoice()
                    || !delivery.isReady(gameTime)
                    || optionIndex < 0
                    || optionIndex >= delivery.items().size()) {
                continue;
            }

            delivery.select(optionIndex);
            data.setDirty();
            player.sendSystemMessage(Component.translatable(
                    "message.biohazard.delivery.choice_confirmed"
            ));
            collectReady(player);
            return;
        }
    }

    public static void sendStatus(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        long gameTime = server.overworld().getGameTime();
        int ready = 0;
        int pending = 0;
        long nextReadyAt = Long.MAX_VALUE;

        for (RadioDelivery delivery
                : DeliverySavedData.get(server).deliveries()) {
            if (!delivery.owner().equals(player.getUUID())) {
                continue;
            }
            if (delivery.isReady(gameTime)) {
                ready++;
            } else {
                pending++;
                nextReadyAt = Math.min(nextReadyAt, delivery.readyAt());
            }
        }

        if (ready > 0) {
            player.sendSystemMessage(Component.translatable(
                    "message.biohazard.delivery.inventory_full",
                    ready
            ));
        } else if (pending > 0) {
            long seconds = (nextReadyAt - gameTime + 19L) / 20L;
            player.sendSystemMessage(Component.translatable(
                    "message.biohazard.delivery.pending",
                    pending,
                    seconds
            ));
        } else {
            player.sendSystemMessage(Component.translatable(
                    "message.biohazard.delivery.none"
            ));
        }
    }

    private static List<ItemStack> generateManifest(
            ServerPlayer player,
            String manifest
    ) {
        ServerLevel level = player.serverLevel();
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(
                Biohazard.MOD_ID,
                "quest_delivery/" + manifest
        );
        ResourceKey<LootTable> key = ResourceKey.create(
                Registries.LOOT_TABLE,
                location
        );
        LootTable table = player.getServer()
                .reloadableRegistries()
                .getLootTable(key);
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, player.position())
                .withOptionalParameter(LootContextParams.THIS_ENTITY, player)
                .withLuck(player.getLuck())
                .create(LootContextParamSets.CHEST);
        return table.getRandomItems(params).stream()
                .filter(stack -> !stack.isEmpty())
                .map(ItemStack::copy)
                .toList();
    }

    private static List<ItemStack> generateChoiceOptions(
            ServerPlayer player,
            String manifest,
            int optionCount
    ) {
        List<ItemStack> options = new ArrayList<>();
        int targetCount = Math.max(1, optionCount);
        int attempts = targetCount * 8;
        for (int attempt = 0; attempt < attempts
                && options.size() < targetCount; attempt++) {
            for (ItemStack candidate : generateManifest(player, manifest)) {
                boolean duplicate = options.stream().anyMatch(existing ->
                        ItemStack.isSameItemSameComponents(existing, candidate)
                );
                if (!duplicate) {
                    options.add(candidate.copy());
                }
                if (options.size() >= targetCount) {
                    break;
                }
            }
        }
        return options;
    }

    private DeliveryManager() {
    }
}
