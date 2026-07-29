package io.github.gev414.rotwire.network;

import io.github.gev414.rotwire.Rotwire;
import io.github.gev414.rotwire.client.WeatherExposureClient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record WeatherExposurePayload(
        boolean contaminated,
        boolean storm,
        boolean exposed,
        boolean harmful
) implements CustomPacketPayload {

    public static final Type<WeatherExposurePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    Rotwire.MOD_ID,
                    "weather_exposure"
            ));

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            WeatherExposurePayload
            > STREAM_CODEC = StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    WeatherExposurePayload::contaminated,
                    ByteBufCodecs.BOOL,
                    WeatherExposurePayload::storm,
                    ByteBufCodecs.BOOL,
                    WeatherExposurePayload::exposed,
                    ByteBufCodecs.BOOL,
                    WeatherExposurePayload::harmful,
                    WeatherExposurePayload::new
            );

    public static WeatherExposurePayload clear() {
        return new WeatherExposurePayload(
                false,
                false,
                false,
                false
        );
    }

    public static void handle(
            WeatherExposurePayload payload,
            IPayloadContext context
    ) {
        WeatherExposureClient.update(payload);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
