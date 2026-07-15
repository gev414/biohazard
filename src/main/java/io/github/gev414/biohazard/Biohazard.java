package io.github.gev414.biohazard;

import io.github.gev414.biohazard.config.EncounterConfig;
import io.github.gev414.biohazard.entity.ModEntities;
import io.github.gev414.biohazard.event.EncounterEvents;
import io.github.gev414.biohazard.event.ModEntityEvents;
import io.github.gev414.biohazard.lostcities.LostCitiesIntegration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(Biohazard.MOD_ID)
public final class Biohazard {

    public static final String MOD_ID = "biohazard";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public Biohazard(IEventBus modEventBus, ModContainer modContainer) {
        ModEntities.ENTITY_TYPES.register(modEventBus);
        modEventBus.addListener(ModEntityEvents::registerAttributes);

        EncounterConfig.initialize();
        modContainer.registerConfig(
                ModConfig.Type.SERVER,
                EncounterConfig.SPEC,
                "biohazard-encounters.toml"
        );

        LostCitiesIntegration.initialize(modEventBus);
        NeoForge.EVENT_BUS.addListener(EncounterEvents::onServerTick);
        NeoForge.EVENT_BUS.addListener(
                EventPriority.LOWEST,
                EncounterEvents::onLivingDeath
        );
        NeoForge.EVENT_BUS.addListener(EncounterEvents::onRightClickBlock);
    }
}
