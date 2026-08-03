package io.github.gev414.rotwire.settlement;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.github.gev414.rotwire.city.CityZoneKey;
import io.github.gev414.rotwire.quest.RadioNetwork;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/** Administrative siege controls scoped to the caller's connected radio. */
public final class SettlementSiegeCommands {

    public static LiteralArgumentBuilder<CommandSourceStack> branch() {
        return Commands.literal("siege")
                .then(Commands.literal("start").executes(
                        SettlementSiegeCommands::start
                ))
                .then(Commands.literal("cancel").executes(
                        SettlementSiegeCommands::cancel
                ))
                .then(Commands.literal("status").executes(
                        SettlementSiegeCommands::status
                ));
    }

    private static int start(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        Optional<CityZoneKey> zone = callerZone(source);
        if (zone.isEmpty()) {
            source.sendFailure(Component.translatable(
                    "commands.rotwire.siege.radio_required"
            ));
            return 0;
        }
        if (!SettlementManager.status(source.getLevel(), zone.get())
                .map(SettlementManager::isOperational).orElse(false)) {
            source.sendFailure(Component.translatable(
                    "commands.rotwire.siege.not_operational"
            ));
            return 0;
        }
        if (!SettlementManager.startTestSiege(source.getLevel(), zone.get())) {
            source.sendFailure(Component.translatable(
                    "commands.rotwire.siege.already_active"
            ));
            return 0;
        }
        source.sendSuccess(
                () -> Component.translatable("commands.rotwire.siege.started"),
                true
        );
        return 1;
    }

    private static int cancel(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        Optional<CityZoneKey> zone = callerZone(source);
        if (zone.isEmpty()) {
            source.sendFailure(Component.translatable(
                    "commands.rotwire.siege.radio_required"
            ));
            return 0;
        }
        if (!SettlementManager.cancelTestSiege(source.getLevel(), zone.get())) {
            source.sendFailure(Component.translatable(
                    "commands.rotwire.siege.none"
            ));
            return 0;
        }
        source.sendSuccess(
                () -> Component.translatable("commands.rotwire.siege.cancelled"),
                true
        );
        return 1;
    }

    private static int status(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        Optional<CityZoneKey> zone = callerZone(source);
        if (zone.isEmpty()) {
            source.sendFailure(Component.translatable(
                    "commands.rotwire.siege.radio_required"
            ));
            return 0;
        }
        SettlementSnapshot settlement = SettlementManager.status(
                source.getLevel(),
                zone.get()
        ).orElse(null);
        if (settlement == null) {
            source.sendFailure(Component.translatable(
                    "commands.rotwire.siege.none"
            ));
            return 0;
        }
        long remaining = Math.max(
                0L,
                settlement.nextSiegeAt() - source.getLevel().getGameTime()
        );
        source.sendSuccess(
                () -> Component.translatable(
                        "commands.rotwire.siege.status",
                        Component.translatable(
                                "screen.rotwire.city_status.siege."
                                        + siegeName(settlement.siegeState())
                        ),
                        (remaining + 19L) / 20L
                ),
                false
        );
        return 1;
    }

    private static Optional<CityZoneKey> callerZone(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            return Optional.empty();
        }
        return RadioNetwork.findConnectedTransmitter(player)
                .flatMap(position -> RadioNetwork.cityZone(
                        player.level(),
                        position
                ));
    }

    private static String siegeName(SettlementSiegeState state) {
        return switch (state) {
            case WARNING -> "warning";
            case ACTIVE -> "active";
            case RECOVERY -> "recovery";
            case CALM -> "calm";
        };
    }

    private SettlementSiegeCommands() {
    }
}
