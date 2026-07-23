package io.github.gev414.biohazard.encumbrance;

import io.github.gev414.biohazard.Biohazard;
import io.github.gev414.biohazard.config.SurvivalSystemsConfig;
import mod.pbj.item.GunItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class EncumbranceManager {

    private static final ResourceLocation SPEED_MODIFIER =
            ResourceLocation.fromNamespaceAndPath(
                    Biohazard.MOD_ID,
                    "encumbrance_movement_speed"
            );
    private static final Map<UUID, EncumbranceSnapshot> SNAPSHOTS =
            new HashMap<>();
    private static final boolean TRAVELERS_BACKPACK_LOADED =
            ModList.get().isLoaded("travelersbackpack");

    private static int ticksUntilUpdate;

    public static void onServerTick(ServerTickEvent.Post event) {
        if (!SurvivalSystemsConfig.ENABLED.get()) {
            clearAll(event);
            return;
        }
        if (ticksUntilUpdate > 0) {
            ticksUntilUpdate--;
            return;
        }
        ticksUntilUpdate = SurvivalSystemsConfig
                .UPDATE_INTERVAL_TICKS
                .get() - 1;

        for (ServerPlayer player
                : event.getServer().getPlayerList().getPlayers()) {
            update(player);
        }
    }

    public static void onPlayerLoggedIn(
            PlayerEvent.PlayerLoggedInEvent event
    ) {
        if (event.getEntity() instanceof ServerPlayer player) {
            update(player);
        }
    }

    public static void onPlayerLoggedOut(
            PlayerEvent.PlayerLoggedOutEvent event
    ) {
        SNAPSHOTS.remove(event.getEntity().getUUID());
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        SNAPSHOTS.clear();
        ticksUntilUpdate = 0;
    }

    public static EncumbranceSnapshot snapshot(ServerPlayer player) {
        return SNAPSHOTS.getOrDefault(
                player.getUUID(),
                EncumbranceSnapshot.EMPTY
        );
    }

    public static boolean isLight(ServerPlayer player) {
        return snapshot(player).tier().permitsQuietMovement();
    }

    private static void update(ServerPlayer player) {
        if (!SurvivalSystemsConfig.ENABLED.get()) {
            removeSpeedModifier(player);
            SNAPSHOTS.remove(player.getUUID());
            return;
        }

        double weight = inventoryWeight(player);
        EncumbranceTier tier = EncumbranceTier.forWeight(
                weight,
                SurvivalSystemsConfig.LIGHT_MAX_WEIGHT.get(),
                SurvivalSystemsConfig.BURDENED_MAX_WEIGHT.get(),
                SurvivalSystemsConfig.HEAVY_MAX_WEIGHT.get()
        );
        SNAPSHOTS.put(
                player.getUUID(),
                new EncumbranceSnapshot(weight, tier)
        );
        applySpeedModifier(player, speedPenalty(tier));
    }

    private static double inventoryWeight(ServerPlayer player) {
        double weight = 0.0D;
        for (ItemStack stack : player.getInventory().items) {
            weight += stackWeight(stack);
        }
        for (ItemStack stack : player.getInventory().armor) {
            weight += stackWeight(stack);
        }
        for (ItemStack stack : player.getInventory().offhand) {
            weight += stackWeight(stack);
        }
        if (TRAVELERS_BACKPACK_LOADED) {
            weight += TravelersBackpackIntegration.equippedWeight(
                    player,
                    stack -> stackWeight(stack, 1)
            );
        }
        return weight;
    }

    private static double stackWeight(ItemStack stack) {
        return stackWeight(stack, 0);
    }

    private static double stackWeight(ItemStack stack, int nestingDepth) {
        if (stack.isEmpty() || stack.is(ModItemTags.WEIGHTLESS)) {
            return 0.0D;
        }
        if (TRAVELERS_BACKPACK_LOADED
                && TravelersBackpackIntegration.isBackpack(stack)) {
            if (nestingDepth > 0) {
                return SurvivalSystemsConfig.BACKPACK_BASE_WEIGHT.get();
            }
            return TravelersBackpackIntegration.backpackWeight(
                    stack,
                    nested -> stackWeight(nested, nestingDepth + 1)
            );
        }

        double categoryWeight;
        if (stack.is(ModItemTags.VERY_HEAVY)) {
            categoryWeight =
                    SurvivalSystemsConfig.VERY_HEAVY_STACK_WEIGHT.get();
        } else if (stack.is(ModItemTags.HEAVY)) {
            categoryWeight =
                    SurvivalSystemsConfig.HEAVY_STACK_WEIGHT.get();
        } else if (stack.is(ModItemTags.LIGHT)) {
            categoryWeight =
                    SurvivalSystemsConfig.LIGHT_STACK_WEIGHT.get();
        } else if (stack.getItem() instanceof GunItem) {
            categoryWeight =
                    SurvivalSystemsConfig.FIREARM_STACK_WEIGHT.get();
        } else if (stack.getItem() instanceof ArmorItem) {
            categoryWeight =
                    SurvivalSystemsConfig.ARMOR_STACK_WEIGHT.get();
        } else if (stack.getItem() instanceof BlockItem) {
            categoryWeight =
                    SurvivalSystemsConfig.BLOCK_STACK_WEIGHT.get();
        } else {
            categoryWeight =
                    SurvivalSystemsConfig.DEFAULT_STACK_WEIGHT.get();
        }
        return EncumbranceMath.scaledStackWeight(
                categoryWeight,
                stack.getCount(),
                stack.getMaxStackSize()
        );
    }

    private static double speedPenalty(EncumbranceTier tier) {
        return switch (tier) {
            case LIGHT -> 0.0D;
            case BURDENED ->
                    SurvivalSystemsConfig.BURDENED_SPEED_PENALTY.get();
            case HEAVY ->
                    SurvivalSystemsConfig.HEAVY_SPEED_PENALTY.get();
            case OVERLOADED ->
                    SurvivalSystemsConfig.OVERLOADED_SPEED_PENALTY.get();
        };
    }

    private static void applySpeedModifier(
            ServerPlayer player,
            double penalty
    ) {
        AttributeInstance speed = player.getAttribute(
                Attributes.MOVEMENT_SPEED
        );
        if (speed == null) {
            return;
        }
        if (penalty <= 0.0D) {
            speed.removeModifier(SPEED_MODIFIER);
            return;
        }
        speed.addOrUpdateTransientModifier(new AttributeModifier(
                SPEED_MODIFIER,
                -Math.min(0.95D, penalty),
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        ));
    }

    private static void removeSpeedModifier(ServerPlayer player) {
        AttributeInstance speed = player.getAttribute(
                Attributes.MOVEMENT_SPEED
        );
        if (speed != null) {
            speed.removeModifier(SPEED_MODIFIER);
        }
    }

    private static void clearAll(ServerTickEvent.Post event) {
        if (SNAPSHOTS.isEmpty()) {
            return;
        }
        for (ServerPlayer player
                : event.getServer().getPlayerList().getPlayers()) {
            removeSpeedModifier(player);
        }
        SNAPSHOTS.clear();
    }

    private EncumbranceManager() {
    }
}
