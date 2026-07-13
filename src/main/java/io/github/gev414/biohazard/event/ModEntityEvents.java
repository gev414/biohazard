package io.github.gev414.biohazard.event;

import io.github.gev414.biohazard.entity.BruteEntity;
import io.github.gev414.biohazard.entity.ModEntities;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

public final class ModEntityEvents {

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(
                ModEntities.BRUTE.get(),
                BruteEntity.createAttributes().build()
        );
    }

    private ModEntityEvents() {
    }
}