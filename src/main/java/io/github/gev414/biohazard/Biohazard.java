package io.github.gev414.biohazard;

import io.github.gev414.biohazard.block.ModBlocks;
import io.github.gev414.biohazard.block.entity.ModBlockEntities;
import io.github.gev414.biohazard.city.InfectedCityScaling;
import io.github.gev414.biohazard.config.CityOperationsConfig;
import io.github.gev414.biohazard.config.EncounterConfig;
import io.github.gev414.biohazard.config.HordeAtmosphereConfig;
import io.github.gev414.biohazard.config.RadioQuestConfig;
import io.github.gev414.biohazard.config.SurvivalSystemsConfig;
import io.github.gev414.biohazard.entity.ModEntities;
import io.github.gev414.biohazard.event.EncounterEvents;
import io.github.gev414.biohazard.event.HordeAtmosphereSyncEvents;
import io.github.gev414.biohazard.event.ModEntityEvents;
import io.github.gev414.biohazard.event.ModCreativeTabEvents;
import io.github.gev414.biohazard.event.SurvivalSystemsEvents;
import io.github.gev414.biohazard.item.ModItems;
import io.github.gev414.biohazard.lostcities.LostCitiesIntegration;
import io.github.gev414.biohazard.loot.HandcraftedStorageLoot;
import io.github.gev414.biohazard.network.ModPayloads;
import io.github.gev414.biohazard.quest.FTBQuestsIntegration;
import io.github.gev414.biohazard.quest.QuestDefaultsInstaller;
import io.github.gev414.biohazard.quest.delivery.DeliveryManager;
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
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        modEventBus.addListener(ModCreativeTabEvents::buildContents);
        modEventBus.addListener(ModEntityEvents::registerAttributes);
        modEventBus.addListener(ModPayloads::register);

        EncounterConfig.initialize();
        modContainer.registerConfig(
                ModConfig.Type.SERVER,
                EncounterConfig.SPEC,
                "biohazard-encounters.toml"
        );
        CityOperationsConfig.initialize();
        modContainer.registerConfig(
                ModConfig.Type.SERVER,
                CityOperationsConfig.SPEC,
                "biohazard-city-operations.toml"
        );
        HordeAtmosphereConfig.initialize();
        modContainer.registerConfig(
                ModConfig.Type.CLIENT,
                HordeAtmosphereConfig.SPEC,
                "biohazard-client.toml"
        );
        RadioQuestConfig.initialize();
        modContainer.registerConfig(
                ModConfig.Type.SERVER,
                RadioQuestConfig.SPEC,
                "biohazard-radio-quests.toml"
        );
        SurvivalSystemsConfig.initialize();
        modContainer.registerConfig(
                ModConfig.Type.SERVER,
                SurvivalSystemsConfig.SPEC,
                "biohazard-survival.toml"
        );

        QuestDefaultsInstaller.installIfMissing();
        FTBQuestsIntegration.initialize();

        LostCitiesIntegration.initialize(modEventBus);
        NeoForge.EVENT_BUS.addListener(EncounterEvents::onServerTick);
        NeoForge.EVENT_BUS.addListener(
                EventPriority.LOWEST,
                EncounterEvents::onLivingDeath
        );
        NeoForge.EVENT_BUS.addListener(EncounterEvents::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(
                InfectedCityScaling::onEntityJoinLevel
        );
        NeoForge.EVENT_BUS.addListener(
                HandcraftedStorageLoot::onBlockPlaced
        );
        NeoForge.EVENT_BUS.addListener(
                HordeAtmosphereSyncEvents::onServerTick
        );
        NeoForge.EVENT_BUS.addListener(
                HordeAtmosphereSyncEvents::onPlayerLoggedIn
        );
        NeoForge.EVENT_BUS.addListener(
                HordeAtmosphereSyncEvents::onPlayerLoggedOut
        );
        NeoForge.EVENT_BUS.addListener(
                HordeAtmosphereSyncEvents::onServerStopped
        );
        NeoForge.EVENT_BUS.addListener(DeliveryManager::onServerTick);
        NeoForge.EVENT_BUS.addListener(DeliveryManager::onServerStopped);
        NeoForge.EVENT_BUS.addListener(
                SurvivalSystemsEvents::onServerTick
        );
        NeoForge.EVENT_BUS.addListener(
                EventPriority.HIGHEST,
                SurvivalSystemsEvents::onLivingChangeTarget
        );
        NeoForge.EVENT_BUS.addListener(
                SurvivalSystemsEvents::onIncomingDamage
        );
        NeoForge.EVENT_BUS.addListener(
                SurvivalSystemsEvents::onBlockBreak
        );
        NeoForge.EVENT_BUS.addListener(
                SurvivalSystemsEvents::onSoundAtPosition
        );
        NeoForge.EVENT_BUS.addListener(
                EventPriority.HIGHEST,
                SurvivalSystemsEvents::onEntityJoinLevel
        );
        NeoForge.EVENT_BUS.addListener(
                SurvivalSystemsEvents::onPlayerLoggedIn
        );
        NeoForge.EVENT_BUS.addListener(
                SurvivalSystemsEvents::onPlayerLoggedOut
        );
        NeoForge.EVENT_BUS.addListener(
                SurvivalSystemsEvents::onServerStopped
        );
    }
}
