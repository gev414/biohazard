package io.github.gev414.rotwire.network;

import io.github.gev414.rotwire.Rotwire;
import io.github.gev414.rotwire.client.WeatherForecastClient;
import io.github.gev414.rotwire.weather.ScheduledWeather;
import io.github.gev414.rotwire.weather.WeatherDayPlan;
import io.github.gev414.rotwire.weather.WeatherSeason;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record WeatherForecastPayload(
        boolean available,
        int todayWeather,
        int todayStart,
        int todayEnd,
        int tomorrowWeather,
        int tomorrowStart,
        int tomorrowEnd,
        int season,
        boolean forced,
        int forcedWeather,
        long forcedExpiresAt
) implements CustomPacketPayload {

    public static final Type<WeatherForecastPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    Rotwire.MOD_ID,
                    "weather_forecast"
            ));

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            WeatherForecastPayload
            > STREAM_CODEC = StreamCodec.of(
                    WeatherForecastPayload::encode,
                    WeatherForecastPayload::decode
            );

    public static WeatherForecastPayload from(
            WeatherDayPlan today,
            WeatherDayPlan tomorrow,
            WeatherSeason season,
            ScheduledWeather forcedWeather,
            long forcedExpiresAt
    ) {
        return new WeatherForecastPayload(
                true,
                today.weather().ordinal(),
                today.startTick(),
                today.endTick(),
                tomorrow.weather().ordinal(),
                tomorrow.startTick(),
                tomorrow.endTick(),
                season.ordinal(),
                forcedWeather != null,
                forcedWeather == null ? 0 : forcedWeather.ordinal(),
                forcedWeather == null ? 0L : forcedExpiresAt
        );
    }

    public static WeatherForecastPayload unavailable() {
        return new WeatherForecastPayload(
                false,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                false,
                0,
                0L
        );
    }

    public static void handle(
            WeatherForecastPayload payload,
            IPayloadContext context
    ) {
        WeatherForecastClient.update(payload);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(
            RegistryFriendlyByteBuf buffer,
            WeatherForecastPayload payload
    ) {
        buffer.writeBoolean(payload.available);
        buffer.writeVarInt(payload.todayWeather);
        buffer.writeVarInt(payload.todayStart);
        buffer.writeVarInt(payload.todayEnd);
        buffer.writeVarInt(payload.tomorrowWeather);
        buffer.writeVarInt(payload.tomorrowStart);
        buffer.writeVarInt(payload.tomorrowEnd);
        buffer.writeVarInt(payload.season);
        buffer.writeBoolean(payload.forced);
        buffer.writeVarInt(payload.forcedWeather);
        buffer.writeVarLong(payload.forcedExpiresAt);
    }

    private static WeatherForecastPayload decode(
            RegistryFriendlyByteBuf buffer
    ) {
        return new WeatherForecastPayload(
                buffer.readBoolean(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readBoolean(),
                buffer.readVarInt(),
                buffer.readVarLong()
        );
    }
}
