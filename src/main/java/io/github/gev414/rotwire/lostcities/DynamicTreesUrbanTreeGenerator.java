package io.github.gev414.rotwire.lostcities;

import com.dtteam.dynamictrees.api.worldgen.BiomePropertySelectors;
import com.dtteam.dynamictrees.api.worldgen.LevelContext;
import com.dtteam.dynamictrees.tree.species.Species;
import com.dtteam.dynamictrees.worldgen.BiomeDatabase;
import com.dtteam.dynamictrees.worldgen.BiomeDatabases;
import com.dtteam.dynamictrees.worldgen.DynamicTreeGenerationContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;

final class DynamicTreesUrbanTreeGenerator implements UrbanTreeGenerator {

    private static final int URBAN_TREE_RADIUS = 4;

    @Override
    public boolean generate(
            ServerLevel level,
            BlockPos rootPos,
            RandomSource random
    ) {
        LevelContext levelContext = LevelContext.create(level);
        if (BiomeDatabases.isBlacklisted(levelContext.dimensionName())) {
            return false;
        }

        Holder<Biome> biome = level.getBiome(rootPos);
        BiomeDatabase database =
                BiomeDatabases.getDimensionalOrDefault(
                        levelContext.dimensionName()
                );
        BiomeDatabase.EntryReader entry = database.getEntry(biome);
        if (entry == null || entry.isBlacklisted()) {
            return false;
        }

        BlockState soil = level.getBlockState(rootPos);
        BiomePropertySelectors.SpeciesSelection selection =
                entry.getSpeciesSelector().getSpecies(
                        rootPos,
                        soil,
                        random
                );
        if (!selection.isHandled()) {
            return false;
        }

        Species species = selection.getSpecies();
        if (!species.isValid()
                || !species.isAcceptableSoilForWorldgen(
                        level,
                        rootPos,
                        soil
                )) {
            return false;
        }

        Direction facing = Direction.from2DDataValue(random.nextInt(4));
        DynamicTreeGenerationContext context =
                new DynamicTreeGenerationContext(
                        levelContext,
                        species,
                        rootPos,
                        rootPos.mutable(),
                        biome,
                        facing,
                        URBAN_TREE_RADIUS,
                        true
                );
        return species.generate(context);
    }
}
