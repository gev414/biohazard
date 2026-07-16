package io.github.gev414.biohazard.network;

import io.github.gev414.biohazard.Biohazard;
import io.github.gev414.biohazard.client.HordeAtmosphereState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record HordeAtmospherePayload(
        boolean hordeDay,
        boolean active,
        int dayLength,
        int hordeStartTime
) implements CustomPacketPayload {

    public static final Type<HordeAtmospherePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(
                    Biohazard.MOD_ID,
                    "horde_atmosphere"
            )
    );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            HordeAtmospherePayload
            > STREAM_CODEC = StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    HordeAtmospherePayload::hordeDay,
                    ByteBufCodecs.BOOL,
                    HordeAtmospherePayload::active,
                    ByteBufCodecs.VAR_INT,
                    HordeAtmospherePayload::dayLength,
                    ByteBufCodecs.VAR_INT,
                    HordeAtmospherePayload::hordeStartTime,
                    HordeAtmospherePayload::new
            );

    public static void handle(
            HordeAtmospherePayload payload,
            IPayloadContext context
    ) {
        HordeAtmosphereState.update(
                payload.hordeDay,
                payload.active,
                payload.dayLength,
                payload.hordeStartTime
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
