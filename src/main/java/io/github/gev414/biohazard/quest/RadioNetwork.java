package io.github.gev414.biohazard.quest;

import io.github.gev414.biohazard.block.ModBlocks;
import io.github.gev414.biohazard.block.entity.RadioTransmitterBlockEntity;
import io.github.gev414.biohazard.city.CityZoneKey;
import io.github.gev414.biohazard.config.RadioQuestConfig;
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
        double maximumDistanceSquared = (range + 0.5D) * (range + 0.5D);

        for (BlockPos position : BlockPos.betweenClosed(
                center.offset(-range, -range, -range),
                center.offset(range, range, range)
        )) {
            if (center.distSqr(position) > maximumDistanceSquared
                    || !level.isLoaded(position)
                    || !level.getBlockState(position).is(
                    ModBlocks.RADIO_TRANSMITTER.get()
            )) {
                continue;
            }
            if (isConnected(level, position)) {
                return Optional.of(position.immutable());
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
