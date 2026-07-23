package io.github.gev414.biohazard.network;

import io.github.gev414.biohazard.Biohazard;
import io.github.gev414.biohazard.client.CityStatusClient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CityStatusPayload(
        boolean mapped,
        int clearedBuildings,
        int dangerLevel,
        int maximumDangerLevel,
        int healthPercent,
        int remainingUntilNextLevel
) implements CustomPacketPayload {

    public static final Type<CityStatusPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(
                    Biohazard.MOD_ID,
                    "city_status"
            )
    );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            CityStatusPayload
            > STREAM_CODEC = StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    CityStatusPayload::mapped,
                    ByteBufCodecs.VAR_INT,
                    CityStatusPayload::clearedBuildings,
                    ByteBufCodecs.VAR_INT,
                    CityStatusPayload::dangerLevel,
                    ByteBufCodecs.VAR_INT,
                    CityStatusPayload::maximumDangerLevel,
                    ByteBufCodecs.VAR_INT,
                    CityStatusPayload::healthPercent,
                    ByteBufCodecs.VAR_INT,
                    CityStatusPayload::remainingUntilNextLevel,
                    CityStatusPayload::new
            );

    public static CityStatusPayload noCity() {
        return new CityStatusPayload(false, 0, 0, 0, 0, 0);
    }

    public static void handle(
            CityStatusPayload payload,
            IPayloadContext context
    ) {
        CityStatusClient.update(payload);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
