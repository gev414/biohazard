package io.github.gev414.biohazard.event;

import io.github.gev414.biohazard.config.EncounterConfig;
import io.github.gev414.biohazard.encounter.BuildingDescriptor;
import io.github.gev414.biohazard.encounter.BuildingEncounter;
import io.github.gev414.biohazard.encounter.EncounterEntityData;
import io.github.gev414.biohazard.encounter.EncounterManager;
import io.github.gev414.biohazard.encounter.EncounterSavedData;
import io.github.gev414.biohazard.lostcities.LostCitiesBuildingResolver;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Optional;

public final class EncounterEvents {

    public static void onServerTick(ServerTickEvent.Post event) {
        EncounterManager.tick(event.getServer());
    }

    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.isCanceled()
                || !(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }

        EncounterEntityData.read(event.getEntity())
                .ifPresent(marker -> EncounterManager.recordDeath(
                        level.getServer(),
                        marker
                ));
    }

    public static void onRightClickBlock(
            PlayerInteractEvent.RightClickBlock event
    ) {
        if (!EncounterConfig.ENABLED.get()
                || !EncounterConfig.LOCK_RANDOMIZABLE_CONTAINERS.get()
                || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(event.getPos());
        if (!(blockEntity instanceof RandomizableContainerBlockEntity)) {
            return;
        }

        Optional<BuildingDescriptor> resolved =
                LostCitiesBuildingResolver.resolveCurrentChunk(
                        level,
                        event.getEntity().blockPosition()
                );
        if (resolved.isEmpty()) {
            return;
        }

        BuildingDescriptor building = resolved.get();
        if (!building.contains(event.getEntity().blockPosition())
                || !building.contains(event.getPos())) {
            return;
        }

        EncounterSavedData data = EncounterSavedData.get(
                level.getServer()
        );
        Optional<BuildingEncounter> existing = data.find(building.key());
        if (existing.isPresent()
                ? EncounterConfig.isExcluded(
                existing.get().buildingId()
        )
                : EncounterConfig.isExcluded(building.buildingId())) {
            return;
        }

        EncounterSavedData.MaterializedEncounter materialized =
                EncounterManager.materialize(level, building);
        if (!materialized.encounter().phase().locksContainers()) {
            return;
        }

        event.setCanceled(true);
        event.getEntity().sendSystemMessage(Component.translatable(
                "message.biohazard.encounter.container_locked"
        ));
    }

    private EncounterEvents() {
    }
}
