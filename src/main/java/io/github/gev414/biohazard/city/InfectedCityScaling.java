package io.github.gev414.biohazard.city;

import io.github.gev414.biohazard.Biohazard;
import io.github.gev414.biohazard.config.CityOperationsConfig;
import io.github.gev414.biohazard.entity.BruteEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

public final class InfectedCityScaling {

    private static final String DANGER_LEVEL_TAG =
            "biohazard_city_danger_level";
    private static final ResourceLocation MAX_HEALTH_MODIFIER =
            ResourceLocation.fromNamespaceAndPath(
                    Biohazard.MOD_ID,
                    "city_danger_max_health"
            );

    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(event.getEntity() instanceof LivingEntity living)) {
            return;
        }
        applyCurrentDanger(level, living);
    }

    public static void updateLoadedInfected(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof LivingEntity living) {
                    applyCurrentDanger(level, living);
                }
            }
        }
    }

    private static void applyCurrentDanger(
            ServerLevel level,
            LivingEntity entity
    ) {
        if (!CityOperationsConfig.ENABLED.get()
                || !entity.getType().is(
                ModEntityTypeTags.CITY_SCALED_INFECTED
        )) {
            return;
        }

        int rememberedDanger = Math.max(
                0,
                entity.getPersistentData().getInt(DANGER_LEVEL_TAG)
        );
        int localDanger = CityZoneManager.dangerLevelAt(
                level,
                entity.chunkPosition().x,
                entity.chunkPosition().z
        );
        int targetDanger = Math.max(rememberedDanger, localDanger);
        if (targetDanger <= 0) {
            return;
        }

        AttributeInstance maximumHealth = entity.getAttribute(
                Attributes.MAX_HEALTH
        );
        if (maximumHealth == null) {
            return;
        }

        double healthPerLevel = entity instanceof BruteEntity
                ? CityOperationsConfig
                .BRUTE_HEALTH_PER_DANGER_LEVEL
                .get()
                : CityOperationsConfig.HEALTH_PER_DANGER_LEVEL.get();
        double targetAmount = targetDanger * healthPerLevel;
        AttributeModifier existing = maximumHealth.getModifier(
                MAX_HEALTH_MODIFIER
        );

        entity.getPersistentData().putInt(
                DANGER_LEVEL_TAG,
                targetDanger
        );
        if (existing != null
                && Double.compare(existing.amount(), targetAmount) == 0
                && existing.operation()
                == AttributeModifier.Operation.ADD_MULTIPLIED_BASE) {
            return;
        }

        float previousMaximum = entity.getMaxHealth();
        float healthFraction = previousMaximum <= 0.0F
                ? 1.0F
                : entity.getHealth() / previousMaximum;
        maximumHealth.addOrReplacePermanentModifier(
                new AttributeModifier(
                        MAX_HEALTH_MODIFIER,
                        targetAmount,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                )
        );
        entity.setHealth(Mth.clamp(
                entity.getMaxHealth() * healthFraction,
                0.0F,
                entity.getMaxHealth()
        ));
    }

    private InfectedCityScaling() {
    }
}
