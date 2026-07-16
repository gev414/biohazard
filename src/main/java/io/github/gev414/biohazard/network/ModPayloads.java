package io.github.gev414.biohazard.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModPayloads {

    private static final String PROTOCOL_VERSION = "1";

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToClient(
                HordeAtmospherePayload.TYPE,
                HordeAtmospherePayload.STREAM_CODEC,
                HordeAtmospherePayload::handle
        );
    }

    private ModPayloads() {
    }
}
