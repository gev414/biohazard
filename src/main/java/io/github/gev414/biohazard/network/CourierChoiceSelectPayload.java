package io.github.gev414.biohazard.network;

import io.github.gev414.biohazard.Biohazard;
import io.github.gev414.biohazard.quest.delivery.DeliveryManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CourierChoiceSelectPayload(
        String deliveryId,
        int optionIndex
) implements CustomPacketPayload {

    public static final Type<CourierChoiceSelectPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(
                    Biohazard.MOD_ID,
                    "courier_choice_select"
            )
    );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            CourierChoiceSelectPayload
            > STREAM_CODEC = new StreamCodec<>() {
                @Override
                public CourierChoiceSelectPayload decode(
                        RegistryFriendlyByteBuf buffer
                ) {
                    return new CourierChoiceSelectPayload(
                            buffer.readUtf(36),
                            buffer.readVarInt()
                    );
                }

                @Override
                public void encode(
                        RegistryFriendlyByteBuf buffer,
                        CourierChoiceSelectPayload payload
                ) {
                    buffer.writeUtf(payload.deliveryId, 36);
                    buffer.writeVarInt(payload.optionIndex);
                }
            };

    public static void handle(
            CourierChoiceSelectPayload payload,
            IPayloadContext context
    ) {
        if (context.player() instanceof ServerPlayer player) {
            DeliveryManager.selectChoice(
                    player,
                    payload.deliveryId,
                    payload.optionIndex
            );
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
