package io.github.gev414.biohazard.block.entity;

import io.github.gev414.biohazard.config.RadioQuestConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class RadioTransmitterBlockEntity extends BlockEntity {

    private static final String READY_AT_TAG = "ready_at";

    private long readyAt = -1L;

    public RadioTransmitterBlockEntity(
            BlockPos position,
            BlockState state
    ) {
        super(ModBlockEntities.RADIO_TRANSMITTER.get(), position, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide() && readyAt < 0L) {
            beginCalibration(level.getGameTime());
        }
    }

    public void beginCalibration(long gameTime) {
        readyAt = gameTime + RadioQuestConfig.CALIBRATION_TICKS.get();
        setChanged();
    }

    public boolean isConnected(long gameTime) {
        return readyAt >= 0L && gameTime >= readyAt;
    }

    public long ticksUntilConnected(long gameTime) {
        return readyAt < 0L ? RadioQuestConfig.CALIBRATION_TICKS.get()
                : Math.max(0L, readyAt - gameTime);
    }

    @Override
    protected void saveAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        super.saveAdditional(tag, registries);
        tag.putLong(READY_AT_TAG, readyAt);
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        super.loadAdditional(tag, registries);
        readyAt = tag.contains(READY_AT_TAG)
                ? tag.getLong(READY_AT_TAG)
                : -1L;
    }
}
