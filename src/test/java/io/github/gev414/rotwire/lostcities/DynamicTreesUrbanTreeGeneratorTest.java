package io.github.gev414.rotwire.lostcities;

import com.dtteam.dynamictrees.worldgen.DynamicTreeGenerationContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class DynamicTreesUrbanTreeGeneratorTest {

    @Test
    void generatesAsALiveWorldUpdateSoLeavesReachClients() {
        DynamicTreeGenerationContext context =
                DynamicTreesUrbanTreeGenerator.createGenerationContext(
                        null,
                        null,
                        BlockPos.ZERO,
                        null,
                        Direction.NORTH
                );

        assertFalse(context.isWorldGen());
    }
}
