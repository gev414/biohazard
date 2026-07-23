package io.github.gev414.biohazard.network;

import io.github.gev414.biohazard.Biohazard;
import io.github.gev414.biohazard.client.SurvivalStatusClient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SurvivalStatusPayload(
        int weightTenths,
        int tier,
        boolean enabled,
        boolean quiet,
        int suspicionPercent,
        int lightMaximumTenths,
        int burdenedMaximumTenths,
        int heavyMaximumTenths,
        int burdenedPenaltyPercent,
        int heavyPenaltyPercent,
        int overloadedPenaltyPercent
) implements CustomPacketPayload {

    public static final Type<SurvivalStatusPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(
                    Biohazard.MOD_ID,
                    "survival_status"
            )
    );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            SurvivalStatusPayload
            > STREAM_CODEC = StreamCodec.of(
                    (buffer, payload) -> payload.encode(buffer),
                    SurvivalStatusPayload::decode
            );

    public SurvivalStatusPayload {
        weightTenths = Math.max(0, weightTenths);
        tier = Math.max(0, Math.min(tier, 3));
        suspicionPercent = Math.max(
                0,
                Math.min(suspicionPercent, 100)
        );
        lightMaximumTenths = Math.max(0, lightMaximumTenths);
        burdenedMaximumTenths = Math.max(
                lightMaximumTenths,
                burdenedMaximumTenths
        );
        heavyMaximumTenths = Math.max(
                burdenedMaximumTenths,
                heavyMaximumTenths
        );
        burdenedPenaltyPercent = clampPercent(
                burdenedPenaltyPercent
        );
        heavyPenaltyPercent = clampPercent(heavyPenaltyPercent);
        overloadedPenaltyPercent = clampPercent(
                overloadedPenaltyPercent
        );
    }

    public static void handle(
            SurvivalStatusPayload payload,
            IPayloadContext context
    ) {
        SurvivalStatusClient.update(payload);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(weightTenths);
        buffer.writeVarInt(tier);
        buffer.writeBoolean(enabled);
        buffer.writeBoolean(quiet);
        buffer.writeVarInt(suspicionPercent);
        buffer.writeVarInt(lightMaximumTenths);
        buffer.writeVarInt(burdenedMaximumTenths);
        buffer.writeVarInt(heavyMaximumTenths);
        buffer.writeVarInt(burdenedPenaltyPercent);
        buffer.writeVarInt(heavyPenaltyPercent);
        buffer.writeVarInt(overloadedPenaltyPercent);
    }

    private static SurvivalStatusPayload decode(
            RegistryFriendlyByteBuf buffer
    ) {
        return new SurvivalStatusPayload(
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt()
        );
    }

    private static int clampPercent(int value) {
        return Math.max(0, Math.min(value, 95));
    }
}
