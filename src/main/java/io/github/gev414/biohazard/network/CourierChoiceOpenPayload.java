package io.github.gev414.biohazard.network;

import io.github.gev414.biohazard.Biohazard;
import io.github.gev414.biohazard.client.CourierChoiceClient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public record CourierChoiceOpenPayload(
        String deliveryId,
        List<String> options
) implements CustomPacketPayload {

    public static final Type<CourierChoiceOpenPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(
                    Biohazard.MOD_ID,
                    "courier_choice_open"
            )
    );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            CourierChoiceOpenPayload
            > STREAM_CODEC = new StreamCodec<>() {
                @Override
                public CourierChoiceOpenPayload decode(
                        RegistryFriendlyByteBuf buffer
                ) {
                    String deliveryId = buffer.readUtf(36);
                    int count = buffer.readVarInt();
                    List<String> options = new ArrayList<>(count);
                    for (int index = 0; index < count; index++) {
                        options.add(buffer.readUtf(256));
                    }
                    return new CourierChoiceOpenPayload(deliveryId, options);
                }

                @Override
                public void encode(
                        RegistryFriendlyByteBuf buffer,
                        CourierChoiceOpenPayload payload
                ) {
                    buffer.writeUtf(payload.deliveryId, 36);
                    buffer.writeVarInt(payload.options.size());
                    for (String option : payload.options) {
                        buffer.writeUtf(option, 256);
                    }
                }
            };

    public static void handle(
            CourierChoiceOpenPayload payload,
            IPayloadContext context
    ) {
        CourierChoiceClient.open(payload);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
