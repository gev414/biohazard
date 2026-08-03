package io.github.gev414.rotwire.network;

import io.github.gev414.rotwire.Rotwire;
import io.github.gev414.rotwire.client.CityStatusClient;
import net.minecraft.network.RegistryFriendlyByteBuf;
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
        int remainingUntilNextLevel,
        boolean operationalSettlement,
        int siegeState,
        long siegeTransitionAt
) implements CustomPacketPayload {

    public static final Type<CityStatusPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(
                    Rotwire.MOD_ID,
                    "city_status"
            )
    );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            CityStatusPayload
            > STREAM_CODEC = StreamCodec.of(
                    (buffer, payload) -> {
                        buffer.writeBoolean(payload.mapped());
                        buffer.writeVarInt(payload.clearedBuildings());
                        buffer.writeVarInt(payload.dangerLevel());
                        buffer.writeVarInt(payload.maximumDangerLevel());
                        buffer.writeVarInt(payload.healthPercent());
                        buffer.writeVarInt(payload.remainingUntilNextLevel());
                        buffer.writeBoolean(payload.operationalSettlement());
                        buffer.writeVarInt(payload.siegeState());
                        buffer.writeVarLong(payload.siegeTransitionAt());
                    },
                    buffer -> new CityStatusPayload(
                            buffer.readBoolean(),
                            buffer.readVarInt(),
                            buffer.readVarInt(),
                            buffer.readVarInt(),
                            buffer.readVarInt(),
                            buffer.readVarInt(),
                            buffer.readBoolean(),
                            buffer.readVarInt(),
                            buffer.readVarLong()
                    )
            );

    public static CityStatusPayload noCity() {
        return new CityStatusPayload(false, 0, 0, 0, 0, 0, false, 0, -1L);
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
