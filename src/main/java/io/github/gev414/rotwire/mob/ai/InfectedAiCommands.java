package io.github.gev414.rotwire.mob.ai;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.github.gev414.rotwire.entity.ai.SurvivorNavigationBudget;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/** Permission-gated diagnostics for hostile and survivor AI path workloads. */
public final class InfectedAiCommands {

    private static final int DEFAULT_RADIUS = 64;

    public static LiteralArgumentBuilder<CommandSourceStack> branch() {
        return Commands.literal("ai")
                .then(
                        Commands.literal("status")
                                .executes(context -> status(
                                        context,
                                        DEFAULT_RADIUS
                                ))
                                .then(
                                        Commands.argument(
                                                        "radius",
                                                        IntegerArgumentType
                                                                .integer(
                                                                        8,
                                                                        256
                                                                )
                                                )
                                                .executes(context -> status(
                                                        context,
                                                        IntegerArgumentType
                                                                .getInteger(
                                                                        context,
                                                                        "radius"
                                                                )
                                                ))
                                )
                );
    }

    private static int status(
            CommandContext<CommandSourceStack> context,
            int radius
    ) {
        CommandSourceStack source = context.getSource();
        CoordinatedHostileAi.Report report = CoordinatedHostileAi.report(
                source.getLevel(),
                source.getPosition(),
                radius
        );
        source.sendSuccess(
                () -> Component.translatable(
                        "commands.rotwire.ai.summary",
                        radius,
                        report.total(),
                        count(report, HostileIntent.IDLE),
                        count(report, HostileIntent.INVESTIGATE),
                        count(report, HostileIntent.HUNT),
                        count(report, HostileIntent.ASSAULT)
                ),
                false
        );
        CoordinatedHostileAi.PathBudgetSnapshot pathBudget = report
                .pathBudget();
        source.sendSuccess(
                () -> Component.translatable(
                        "commands.rotwire.ai.path_load",
                        pathBudget.active(),
                        pathBudget.effectiveLimit(),
                        pathBudget.configuredLimit(),
                        pathBudget.calculated(),
                        pathBudget.deferred(),
                        String.format(
                                Locale.ROOT,
                                "%.2f",
                                pathBudget.averageMilliseconds()
                        ),
                        String.format(
                                Locale.ROOT,
                                "%.2f",
                                pathBudget.maximumMilliseconds()
                        ),
                        pathBudget.queued()
                ),
                false
        );
        SurvivorNavigationBudget.Snapshot survivorPaths =
                SurvivorNavigationBudget.snapshot(source.getLevel());
        source.sendSuccess(
                () -> Component.translatable(
                        "commands.rotwire.ai.survivor_path_load",
                        survivorPaths.limit(),
                        survivorPaths.calculated(),
                        survivorPaths.deferred(),
                        String.format(
                                Locale.ROOT,
                                "%.2f",
                                survivorPaths.averageMilliseconds()
                        ),
                        String.format(
                                Locale.ROOT,
                                "%.2f",
                                survivorPaths.maximumMilliseconds()
                        )
                ),
                false
        );
        CoordinatedHostileAi.Snapshot nearest = report.nearest();
        if (nearest == null) {
            source.sendSuccess(
                    () -> Component.translatable(
                            "commands.rotwire.ai.nearest.none"
                    ),
                    false
            );
            return 0;
        }
        source.sendSuccess(
                () -> Component.translatable(
                        "commands.rotwire.ai.nearest",
                        nearest.name(),
                        nearest.entityId(),
                        nearest.intent().name(),
                        nearest.action().name(),
                        nearest.target(),
                        nearest.navigationDone(),
                        nearest.pathCooldown(),
                        nearest.pathAttempts(),
                        nearest.lastPathSucceeded(),
                        nearest.breachTarget() == null
                                ? "none"
                                : nearest.breachTarget().toShortString(),
                        nearest.activePathPartial()
                ),
                false
        );
        source.sendSuccess(
                () -> Component.translatable(
                        "commands.rotwire.ai.nearest.perception",
                        nearest.targetDistance() < 0.0D
                                ? "none"
                                : String.format(
                                        Locale.ROOT,
                                        "%.1f",
                                        nearest.targetDistance()
                                ),
                        nearest.targetVisible(),
                        nearest.lastObstacle(),
                        nearest.lastObstacleBreachable(),
                        nearest.breachAllowed(),
                        nearest.consecutivePathFailures()
                ),
                false
        );
        source.sendSuccess(
                () -> Component.translatable(
                        "commands.rotwire.ai.siege_group",
                        nearest.sharedRouteFailures(),
                        nearest.sharedBreachPlan(),
                        nearest.sharedOpening()
                ),
                false
        );
        return report.total();
    }

    private static int count(
            CoordinatedHostileAi.Report report,
            HostileIntent intent
    ) {
        return report.intents().getOrDefault(intent, 0);
    }

    private InfectedAiCommands() {
    }
}
