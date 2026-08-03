package io.github.gev414.rotwire.settlement;

import io.github.gev414.rotwire.config.SettlementConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/** Resolves data-driven siege resistance without mutating another mod's blocks. */
public final class SiegeBreachRules {

    public static boolean canBreach(ServerLevel level, BlockPos position) {
        BlockState state = level.getBlockState(position);
        return state.is(SettlementBlockTags.SIEGE_BREAKABLE)
                && state.getDestroySpeed(level, position) >= 0.0F;
    }

    public static int durationTicks(BlockState state) {
        if (state.is(SettlementBlockTags.SIEGE_BREACH_FRAGILE)) {
            return SettlementConfig.SIEGE_FRAGILE_BREACH_TICKS.get();
        }
        if (state.is(SettlementBlockTags.SIEGE_BREACH_REINFORCED)) {
            return SettlementConfig.SIEGE_REINFORCED_BREACH_TICKS.get();
        }
        return SettlementConfig.SIEGE_STANDARD_BREACH_TICKS.get();
    }

    /**
     * Structural fallback accepts ordinary solid blocks regardless of their
     * material tag. Technical blocks, inventories/machines, fluids, blocks
     * with negative hardness, and data-pack protected blocks remain immune.
     */
    public static boolean canStructurallyBreach(
            ServerLevel level,
            BlockPos position
    ) {
        BlockState state = level.getBlockState(position);
        return !state.isAir()
                && state.getFluidState().isEmpty()
                && !state.getCollisionShape(level, position).isEmpty()
                && !state.is(SettlementBlockTags.SIEGE_UNBREAKABLE)
                && state.getDestroySpeed(level, position) >= 0.0F
                && level.getBlockEntity(position) == null;
    }

    public static int structuralDurationTicks(
            ServerLevel level,
            BlockPos position
    ) {
        float hardness = Math.max(
                0.0F,
                level.getBlockState(position).getDestroySpeed(level, position)
        );
        long calculated = SettlementConfig.SIEGE_STRUCTURAL_BASE_TICKS.get()
                + Math.round(
                        hardness
                                * SettlementConfig
                                .SIEGE_STRUCTURAL_HARDNESS_TICKS.get()
                );
        return (int) Math.min(
                SettlementConfig.SIEGE_STRUCTURAL_MAX_TICKS.get(),
                Math.max(1L, calculated)
        );
    }

    private SiegeBreachRules() {
    }
}
