package io.github.gev414.biohazard.lostcities;

import mcjty.lostcities.api.ILostCities;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.InterModComms;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

import javax.annotation.Nullable;
import java.util.function.Function;

public final class LostCitiesIntegration {

    private static ILostCities api;

    public static void initialize(IEventBus modEventBus) {
        modEventBus.addListener(LostCitiesIntegration::onCommonSetup);
    }

    @Nullable
    public static ILostCities api() {
        return api;
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        InterModComms.sendTo(
                ILostCities.LOSTCITIES,
                ILostCities.GET_LOST_CITIES,
                ApiReceiver::new
        );
    }

    private static final class ApiReceiver
            implements Function<ILostCities, Void> {

        @Nullable
        @Override
        public Void apply(ILostCities lostCities) {
            api = lostCities;
            return null;
        }
    }

    private LostCitiesIntegration() {
    }
}
