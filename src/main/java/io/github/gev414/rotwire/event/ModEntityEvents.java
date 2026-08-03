package io.github.gev414.rotwire.event;

import io.github.gev414.rotwire.entity.BruteEntity;
import io.github.gev414.rotwire.entity.ModEntities;
import io.github.gev414.rotwire.entity.SurvivorEntity;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

public final class ModEntityEvents {

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(
                ModEntities.BRUTE.get(),
                BruteEntity.createAttributes().build()
        );
        event.put(
                ModEntities.SURVIVOR.get(),
                SurvivorEntity.createAttributes().build()
        );
    }

    private ModEntityEvents() {
    }
}
