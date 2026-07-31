package io.github.gev414.rotwire.quest;

import io.github.gev414.rotwire.block.ModBlocks;
import io.github.gev414.rotwire.block.entity.RadioTransmitterBlockEntity;
import io.github.gev414.rotwire.camp.CampModuleType;
import io.github.gev414.rotwire.city.CityZoneKey;
import io.github.gev414.rotwire.config.RadioQuestConfig;
import io.github.gev414.rotwire.config.SurvivalSystemsConfig;
import io.github.gev414.rotwire.sleep.CampInspector;
import io.github.gev414.rotwire.sleep.CampStatus;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.Optional;

public final class RadioNetwork {

    public static Optional<BlockPos> findConnectedTransmitter(
            ServerPlayer player
    ) {
        Level level = player.level();
        BlockPos center = player.blockPosition();
        int range = RadioQuestConfig.TRANSMITTER_RANGE.get();
        int searchRange = Math.max(
                range,
                SurvivalSystemsConfig.SLEEP_CAMPSITE_RADIUS.get()
        );
        double maximumDistanceSquared = (range + 0.5D) * (range + 0.5D);

        for (BlockPos position : BlockPos.betweenClosed(
                center.offset(-searchRange, -searchRange, -searchRange),
                center.offset(searchRange, searchRange, searchRange)
        )) {
            if (!level.isLoaded(position)
                    || !level.getBlockState(position).is(
                    ModBlocks.RADIO_TRANSMITTER.get()
            )) {
                continue;
            }
            if (!isConnected(level, position)) {
                continue;
            }
            if (center.distSqr(position) <= maximumDistanceSquared) {
                return Optional.of(position.immutable());
            }
            if (level.getBlockEntity(position)
                    instanceof RadioTransmitterBlockEntity transmitter
                    && transmitter.hasModule(CampModuleType.OPERATIONS)) {
                CampStatus status = CampInspector.inspectRadio(
                        player.serverLevel(),
                        player,
                        position
                );
                if (status.active()
                        && status.center().distSqr(center)
                        <= (double) status.radius() * status.radius()) {
                    return Optional.of(position.immutable());
                }
            }
        }

        return Optional.empty();
    }

    public static boolean isConnected(Level level, BlockPos position) {
        return level.getBlockEntity(position)
                instanceof RadioTransmitterBlockEntity transmitter
                && transmitter.isConnected(level.getGameTime());
    }

    public static long calibrationSecondsRemaining(
            Level level,
            BlockPos position
    ) {
        if (level.getBlockEntity(position)
                instanceof RadioTransmitterBlockEntity transmitter) {
            return (transmitter.ticksUntilConnected(level.getGameTime()) + 19L)
                    / 20L;
        }
        return (RadioQuestConfig.CALIBRATION_TICKS.get() + 19L) / 20L;
    }

    public static boolean isSurveying(Level level, BlockPos position) {
        return level.getBlockEntity(position)
                instanceof RadioTransmitterBlockEntity transmitter
                && transmitter.isSurveying();
    }

    public static int surveyedChunks(Level level, BlockPos position) {
        return level.getBlockEntity(position)
                instanceof RadioTransmitterBlockEntity transmitter
                ? transmitter.surveyedChunks()
                : 0;
    }

    public static int maximumSurveyChunks(Level level, BlockPos position) {
        return level.getBlockEntity(position)
                instanceof RadioTransmitterBlockEntity transmitter
                ? transmitter.maximumSurveyChunks()
                : 0;
    }

    public static Optional<CityZoneKey> cityZone(
            Level level,
            BlockPos position
    ) {
        if (level.getBlockEntity(position)
                instanceof RadioTransmitterBlockEntity transmitter) {
            return Optional.ofNullable(transmitter.cityZone());
        }
        return Optional.empty();
    }

    private RadioNetwork() {
    }
}
