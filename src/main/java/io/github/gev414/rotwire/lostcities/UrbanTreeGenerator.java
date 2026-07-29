package io.github.gev414.rotwire.lostcities;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;

@FunctionalInterface
interface UrbanTreeGenerator {

    boolean generate(
            ServerLevel level,
            BlockPos rootPos,
            RandomSource random
    );
}
