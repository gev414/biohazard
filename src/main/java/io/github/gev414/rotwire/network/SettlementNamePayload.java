package io.github.gev414.rotwire.network;

import io.github.gev414.rotwire.Rotwire;
import io.github.gev414.rotwire.block.entity.RadioTransmitterBlockEntity;
import io.github.gev414.rotwire.menu.CampRadioMenu;
import io.github.gev414.rotwire.settlement.SettlementManager;
import io.github.gev414.rotwire.settlement.SettlementNameRules;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * The initial primary-hub name is client-entered, but every ownership and
 * position check remains server-authoritative.
 */
public record SettlementNamePayload(
        BlockPos radioPosition,
        String name
) implements CustomPacketPayload {

    public static final Type<SettlementNamePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(
                    Rotwire.MOD_ID,
                    "settlement_name"
            )
    );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            SettlementNamePayload
            > STREAM_CODEC = new StreamCodec<>() {
                @Override
                public SettlementNamePayload decode(
                        RegistryFriendlyByteBuf buffer
                ) {
                    return new SettlementNamePayload(
                            buffer.readBlockPos(),
                            buffer.readUtf(SettlementNameRules.MAX_LENGTH)
                    );
                }

                @Override
                public void encode(
                        RegistryFriendlyByteBuf buffer,
                        SettlementNamePayload payload
                ) {
                    buffer.writeBlockPos(payload.radioPosition);
                    buffer.writeUtf(
                            payload.name,
                            SettlementNameRules.MAX_LENGTH
                    );
                }
            };

    public static void handle(
            SettlementNamePayload payload,
            IPayloadContext context
    ) {
        if (!(context.player() instanceof ServerPlayer player)
                || !(player.containerMenu instanceof CampRadioMenu menu)
                || !menu.radioPosition().equals(payload.radioPosition)
                || !menu.stillValid(player)
                || !(player.level().getBlockEntity(payload.radioPosition)
                instanceof RadioTransmitterBlockEntity radio)
                || radio.cityZone() == null
                || radio.campId().isEmpty()) {
            return;
        }

        radio.refreshSettlement(player.serverLevel());
        boolean renamed = SettlementManager.renamePrimary(
                player.serverLevel(),
                radio.cityZone(),
                radio.campId().get(),
                player.getUUID(),
                payload.name
        );
        if (!renamed) {
            player.sendSystemMessage(Component.translatable(
                    "message.rotwire.settlement.name_rejected"
            ));
            return;
        }

        player.sendSystemMessage(Component.translatable(
                "message.rotwire.settlement.named",
                SettlementNameRules.normalize(payload.name).orElse("")
        ));
        player.closeContainer();
        radio.openCampHub(player);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
