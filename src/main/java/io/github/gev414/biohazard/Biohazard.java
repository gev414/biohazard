package io.github.gev414.biohazard;

import io.github.gev414.biohazard.entity.ModEntities;
import io.github.gev414.biohazard.event.ModEntityEvents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(Biohazard.MOD_ID)
public final class Biohazard {

    public static final String MOD_ID = "biohazard";

    public Biohazard(IEventBus modEventBus) {
        ModEntities.ENTITY_TYPES.register(modEventBus);
        modEventBus.addListener(ModEntityEvents::registerAttributes);
    }
}