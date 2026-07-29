package io.github.gev414.rotwire.weather;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.github.gev414.rotwire.config.WeatherConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.TimeArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class WeatherCommands {

    private static final int DEFAULT_DURATION_TICKS = 12_000;

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("rotwire")
                        .requires(source -> source.hasPermission(2))
                        .then(
                                Commands.literal("weather")
                                        .then(forceBranch())
                                        .then(
                                                Commands.literal("clear")
                                                        .executes(
                                                                WeatherCommands
                                                                ::clear
                                                        )
                                        )
                                        .then(
                                                Commands.literal("status")
                                                        .executes(
                                                                WeatherCommands
                                                                ::status
                                                        )
                                        )
                        )
        );
    }

    private static LiteralArgumentBuilder<CommandSourceStack>
    forceBranch() {
        LiteralArgumentBuilder<CommandSourceStack> branch =
                Commands.literal("force");
        for (ScheduledWeather weather : ScheduledWeather.values()) {
            branch.then(forceCondition(weather));
        }
        return branch;
    }

    private static LiteralArgumentBuilder<CommandSourceStack>
    forceCondition(ScheduledWeather weather) {
        return Commands.literal(weather.serializedName())
                .executes(
                        context -> force(
                                context,
                                weather,
                                DEFAULT_DURATION_TICKS
                        )
                )
                .then(
                        Commands.argument(
                                        "duration",
                                        TimeArgument.time(1)
                                )
                                .executes(
                                        context -> force(
                                                context,
                                                weather,
                                                IntegerArgumentType
                                                        .getInteger(
                                                                context,
                                                                "duration"
                                                        )
                                        )
                                )
                );
    }

    private static int force(
            CommandContext<CommandSourceStack> context,
            ScheduledWeather weather,
            int durationTicks
    ) {
        CommandSourceStack source = context.getSource();
        if (!WeatherConfig.ENABLED.get()) {
            source.sendFailure(Component.translatable(
                    "commands.rotwire.weather.disabled"
            ));
            return 0;
        }

        WeatherManager.force(
                source.getServer().overworld(),
                weather,
                durationTicks
        );
        source.sendSuccess(
                () -> Component.translatable(
                        "commands.rotwire.weather.force",
                        weatherName(weather),
                        durationTicks
                ),
                true
        );
        return 1;
    }

    private static int clear(
            CommandContext<CommandSourceStack> context
    ) {
        CommandSourceStack source = context.getSource();
        boolean cleared = WeatherManager.clearForced(
                source.getServer().overworld()
        );
        if (!cleared) {
            source.sendFailure(Component.translatable(
                    "commands.rotwire.weather.none"
            ));
            return 0;
        }

        source.sendSuccess(
                () -> Component.translatable(
                        "commands.rotwire.weather.clear"
                ),
                true
        );
        return 1;
    }

    private static int status(
            CommandContext<CommandSourceStack> context
    ) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getServer().overworld();
        WeatherOverride override = WeatherManager.forced(level);
        if (override == null) {
            source.sendSuccess(
                    () -> Component.translatable(
                            "commands.rotwire.weather.status.none"
                    ),
                    false
            );
            return 0;
        }

        int remaining = override.remainingTicks(level.getGameTime());
        source.sendSuccess(
                () -> Component.translatable(
                        "commands.rotwire.weather.status.active",
                        weatherName(override.weather()),
                        remaining
                ),
                false
        );
        return 1;
    }

    private static Component weatherName(ScheduledWeather weather) {
        return Component.translatable(
                "screen.rotwire.weather.type."
                        + weather.serializedName()
        );
    }

    private WeatherCommands() {
    }
}
