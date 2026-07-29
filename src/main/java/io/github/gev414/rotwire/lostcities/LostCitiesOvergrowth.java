package io.github.gev414.rotwire.lostcities;

import io.github.gev414.rotwire.Rotwire;
import io.github.gev414.rotwire.attachment.ModAttachments;
import mcjty.lostcities.api.ILostChunkInfo;
import mcjty.lostcities.api.ILostCityInformation;
import mcjty.lostcities.config.LostCityProfile;
import mcjty.lostcities.worldgen.lost.BuildingInfo;
import mcjty.lostcities.worldgen.lost.regassets.data.WorldSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class LostCitiesOvergrowth {

    private static final int MAX_CHUNKS_PER_TICK = 6;
    private static final int TARGET_TREES_PER_NEW_CHUNK = 3;
    private static final int EXTRA_TREES_PER_PROCESSED_CHUNK = 1;
    private static final int REQUIRED_CLEAR_HEIGHT = 7;
    private static final int[][] TREE_POSITIONS = {
            {2, 2},
            {13, 13},
            {13, 2},
            {2, 13},
            {2, 7},
            {13, 8},
            {7, 2},
            {8, 13},
            {4, 4},
            {11, 11},
            {11, 4},
            {4, 11}
    };
    private static final Set<Block> TREE_PIT_SURFACES = Set.of(
            Blocks.STONE,
            Blocks.SMOOTH_STONE,
            Blocks.SMOOTH_STONE_SLAB,
            Blocks.STONE_BRICKS,
            Blocks.MOSSY_STONE_BRICKS,
            Blocks.COBBLESTONE,
            Blocks.MOSSY_COBBLESTONE,
            Blocks.BRICKS,
            Blocks.GRANITE,
            Blocks.POLISHED_GRANITE,
            Blocks.ANDESITE,
            Blocks.POLISHED_ANDESITE,
            Blocks.DIORITE,
            Blocks.POLISHED_DIORITE
    );
    private static final Set<PendingChunk> PENDING =
            ConcurrentHashMap.newKeySet();

    private static volatile UrbanTreeGenerator treeGenerator;
    private static volatile boolean usingDynamicTrees;
    private static volatile boolean dynamicTreesFailed;

    public static void initialize() {
        usingDynamicTrees = ModList.get().isLoaded("dynamictrees");
        treeGenerator = usingDynamicTrees
                ? new DynamicTreesUrbanTreeGenerator()
                : LostCitiesOvergrowth::generateVanillaTree;
        NeoForge.EVENT_BUS.addListener(
                LostCitiesOvergrowth::onChunkLoad
        );
        NeoForge.EVENT_BUS.addListener(
                LostCitiesOvergrowth::onServerTick
        );
        NeoForge.EVENT_BUS.addListener(
                LostCitiesOvergrowth::onServerStopped
        );
    }

    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        ChunkPos center = event.getChunk().getPos();
        enqueue(level, center.x, center.z);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            enqueue(
                    level,
                    center.x + direction.getStepX(),
                    center.z + direction.getStepZ()
            );
        }
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        if (PENDING.isEmpty()) {
            return;
        }

        MinecraftServer server = event.getServer();
        Iterator<PendingChunk> iterator = PENDING.iterator();
        int processed = 0;
        while (iterator.hasNext() && processed < MAX_CHUNKS_PER_TICK) {
            PendingChunk pending = iterator.next();
            iterator.remove();
            ServerLevel level = server.getLevel(pending.dimension());
            if (level == null) {
                continue;
            }

            LevelChunk chunk = level.getChunkSource().getChunkNow(
                    pending.chunkX(),
                    pending.chunkZ()
            );
            if (chunk == null) {
                continue;
            }

            processLoadedChunk(level, chunk);
            processed++;
        }
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        PENDING.clear();
    }

    private static void processLoadedChunk(
            ServerLevel level,
            LevelChunk chunk
    ) {
        ILostCityInformation cityInformation = cityInformation(level);
        if (cityInformation == null) {
            return;
        }

        int chunkX = chunk.getPos().x;
        int chunkZ = chunk.getPos().z;
        ILostChunkInfo chunkInfo = cityInformation.getChunkInfo(
                chunkX,
                chunkZ
        );
        if (chunkInfo == null) {
            return;
        }

        completeVineFacades(
                level,
                cityInformation,
                chunkX,
                chunkZ,
                asBuildingInfo(chunkInfo)
        );
        generateUrbanTrees(level, chunk, chunkInfo);
    }

    private static ILostCityInformation cityInformation(
            ServerLevel level
    ) {
        if (LostCitiesIntegration.api() == null) {
            return null;
        }
        return LostCitiesIntegration.api().getLostInfo(level);
    }

    private static void completeVineFacades(
            ServerLevel level,
            ILostCityInformation cityInformation,
            int chunkX,
            int chunkZ,
            BuildingInfo currentInfo
    ) {
        for (Direction supportDirection : Direction.Plane.HORIZONTAL) {
            int neighborX = chunkX + supportDirection.getStepX();
            int neighborZ = chunkZ + supportDirection.getStepZ();
            if (level.getChunkSource().getChunkNow(
                    neighborX,
                    neighborZ
            ) == null) {
                continue;
            }

            BuildingInfo neighborInfo = asBuildingInfo(
                    cityInformation.getChunkInfo(neighborX, neighborZ)
            );
            if (neighborInfo == null || !neighborInfo.hasBuilding) {
                continue;
            }

            fillFacade(
                    level,
                    chunkX,
                    chunkZ,
                    supportDirection,
                    currentInfo,
                    neighborInfo
            );
        }
    }

    private static void fillFacade(
            ServerLevel level,
            int chunkX,
            int chunkZ,
            Direction supportDirection,
            BuildingInfo currentInfo,
            BuildingInfo building
    ) {
        LostCityProfile profile = building.profile;
        float vineChance = Mth.clamp(profile.VINE_CHANCE, 0.0F, 1.0F);

        int minimumY = building.getCityGroundLevel() + 2;
        if (currentInfo != null) {
            minimumY = currentInfo.hasBuilding
                    ? Math.max(minimumY, currentInfo.getMaxHeight())
                    : Math.max(
                            minimumY,
                            currentInfo.getCityGroundLevel() + 2
                    );
        }
        minimumY = Math.max(minimumY, level.getMinBuildHeight());
        int maximumY = Math.min(
                building.getMaxHeight(),
                level.getMaxBuildHeight() - 1
        );
        if (minimumY >= maximumY) {
            return;
        }

        WorldSettings settings = building.provider
                .getWorldStyle()
                .getWorldSettings();
        BlockState vine = vineState(settings, supportDirection);
        boolean[][] pattern = FacadeVinePattern.create(
                facadeSeed(
                        level.getSeed(),
                        chunkX,
                        chunkZ,
                        supportDirection
                ),
                vineChance,
                maximumY - minimumY
        );
        for (int along = 0; along < 16; along++) {
            for (int y = maximumY - 1; y >= minimumY; y--) {
                BlockPos position = edgePosition(
                        chunkX,
                        chunkZ,
                        supportDirection,
                        along,
                        y
                );
                reconcileVine(
                        level,
                        position,
                        supportDirection,
                        vine,
                        pattern[along][y - minimumY]
                );
            }
        }
    }

    private static void reconcileVine(
            ServerLevel level,
            BlockPos position,
            Direction supportDirection,
            BlockState vine,
            boolean shouldHaveVine
    ) {
        BlockState existing = level.getBlockState(position);
        if (!shouldHaveVine) {
            if (existing.getBlock() == vine.getBlock()) {
                level.setBlock(
                        position,
                        Blocks.AIR.defaultBlockState(),
                        Block.UPDATE_CLIENTS
                );
            }
            return;
        }

        if (!existing.isAir()) {
            return;
        }
        BlockState support = level.getBlockState(
                position.relative(supportDirection)
        );
        BlockState above = level.getBlockState(position.above());
        if (!support.isAir() || above.getBlock() == vine.getBlock()) {
            level.setBlock(position, vine, Block.UPDATE_CLIENTS);
        }
    }

    private static BlockState vineState(
            WorldSettings settings,
            Direction supportDirection
    ) {
        return switch (supportDirection) {
            case WEST -> settings.vineWest();
            case EAST -> settings.vineEast();
            case NORTH -> settings.vineNorth();
            case SOUTH -> settings.vineSouth();
            default -> throw new IllegalArgumentException(
                    "Vertical vine support direction"
            );
        };
    }

    private static BlockPos edgePosition(
            int chunkX,
            int chunkZ,
            Direction supportDirection,
            int along,
            int y
    ) {
        int minimumX = chunkX << 4;
        int minimumZ = chunkZ << 4;
        return switch (supportDirection) {
            case WEST -> new BlockPos(minimumX, y, minimumZ + along);
            case EAST -> new BlockPos(
                    minimumX + 15,
                    y,
                    minimumZ + along
            );
            case NORTH -> new BlockPos(minimumX + along, y, minimumZ);
            case SOUTH -> new BlockPos(
                    minimumX + along,
                    y,
                    minimumZ + 15
            );
            default -> throw new IllegalArgumentException(
                    "Vertical chunk edge direction"
            );
        };
    }

    private static void generateUrbanTrees(
            ServerLevel level,
            LevelChunk chunk,
            ILostChunkInfo chunkInfo
    ) {
        if (chunk.getData(ModAttachments.URBAN_TREES_DENSE_COMPLETE)
                || !isOpenCityChunk(chunkInfo)) {
            return;
        }

        boolean previousPassComplete = chunk.getData(
                ModAttachments.URBAN_TREES_COMPLETE
        );
        int targetTrees = previousPassComplete
                ? EXTRA_TREES_PER_PROCESSED_CHUNK
                : TARGET_TREES_PER_NEW_CHUNK;
        BuildingInfo buildingInfo = asBuildingInfo(chunkInfo);
        if (buildingInfo == null) {
            markTreePassComplete(chunk);
            return;
        }

        List<BlockPos> candidates = findTreeCandidates(
                level,
                chunk.getPos(),
                buildingInfo
        );
        long seed = treeSeed(
                level.getSeed(),
                chunk.getPos().x,
                chunk.getPos().z
        );
        Collections.shuffle(candidates, new java.util.Random(seed));

        RandomSource random = RandomSource.create(seed);
        List<BlockPos> generated = new ArrayList<>();
        for (BlockPos candidate : candidates) {
            if (generated.size() >= targetTrees) {
                break;
            }
            if (tooCloseToAnotherTree(candidate, generated)) {
                continue;
            }
            if (generateTreeAt(level, candidate, random)) {
                generated.add(candidate);
            }
        }

        markTreePassComplete(chunk);
    }

    private static boolean isOpenCityChunk(ILostChunkInfo chunkInfo) {
        if (!chunkInfo.isCity() || chunkInfo.getBuildingId() != null) {
            return false;
        }
        if (chunkInfo.getMaxHighwayLevel() >= 0) {
            return false;
        }
        return chunkInfo.getRailType() == null
                || !chunkInfo.getRailType().isSurface();
    }

    private static List<BlockPos> findTreeCandidates(
            ServerLevel level,
            ChunkPos chunk,
            BuildingInfo buildingInfo
    ) {
        List<BlockPos> candidates = new ArrayList<>();
        int minimumX = chunk.getMinBlockX();
        int minimumZ = chunk.getMinBlockZ();
        int expectedY = buildingInfo.getCityGroundLevel();
        for (int[] local : TREE_POSITIONS) {
            int x = minimumX + local[0];
            int z = minimumZ + local[1];
            int surfaceY = level.getHeight(
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    x,
                    z
            );
            BlockPos rootPos = new BlockPos(x, surfaceY - 1, z);
            if (Math.abs(rootPos.getY() - expectedY) <= 4
                    && isUsableTreeBed(level, rootPos)
                    && hasTreeClearance(level, rootPos.above())) {
                candidates.add(rootPos);
            }
        }
        return candidates;
    }

    private static boolean isUsableTreeBed(
            ServerLevel level,
            BlockPos rootPos
    ) {
        BlockState state = level.getBlockState(rootPos);
        if (!state.getFluidState().isEmpty()
                || state.hasBlockEntity()
                || state.getDestroySpeed(level, rootPos) < 0.0F) {
            return false;
        }
        return state.is(net.minecraft.tags.BlockTags.DIRT)
                || TREE_PIT_SURFACES.contains(state.getBlock());
    }

    private static boolean hasTreeClearance(
            ServerLevel level,
            BlockPos trunkPos
    ) {
        for (int y = 0; y < REQUIRED_CLEAR_HEIGHT; y++) {
            BlockState state = level.getBlockState(trunkPos.above(y));
            if (!state.isAir()
                    && !state.canBeReplaced()
                    && !(state.getBlock() instanceof SaplingBlock)) {
                return false;
            }
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockState state = level.getBlockState(
                    trunkPos.relative(direction)
            );
            if (!state.isAir() && !state.canBeReplaced()) {
                return false;
            }
        }
        return true;
    }

    private static boolean generateTreeAt(
            ServerLevel level,
            BlockPos rootPos,
            RandomSource random
    ) {
        BlockPos trunkPos = rootPos.above();
        BlockState oldGround = level.getBlockState(rootPos);
        BlockState oldTrunk = level.getBlockState(trunkPos);
        if (!oldGround.is(net.minecraft.tags.BlockTags.DIRT)) {
            level.setBlock(
                    rootPos,
                    Blocks.DIRT.defaultBlockState(),
                    Block.UPDATE_ALL
            );
        }
        if (!oldTrunk.isAir()) {
            level.setBlock(
                    trunkPos,
                    Blocks.AIR.defaultBlockState(),
                    Block.UPDATE_ALL
            );
        }

        boolean generated = tryGenerateTree(level, rootPos, random);
        if (generated) {
            return true;
        }

        level.setBlock(rootPos, oldGround, Block.UPDATE_ALL);
        level.setBlock(trunkPos, oldTrunk, Block.UPDATE_ALL);
        return false;
    }

    private static boolean tryGenerateTree(
            ServerLevel level,
            BlockPos rootPos,
            RandomSource random
    ) {
        boolean dynamicAttempt = usingDynamicTrees;
        try {
            if (treeGenerator.generate(level, rootPos, random)) {
                return true;
            }
        } catch (LinkageError | RuntimeException exception) {
            if (!dynamicTreesFailed) {
                dynamicTreesFailed = true;
                Rotwire.LOGGER.warn(
                        "Dynamic Trees city generation failed; "
                                + "using the sapling fallback",
                        exception
                );
                treeGenerator = LostCitiesOvergrowth::generateVanillaTree;
                usingDynamicTrees = false;
            }
            return generateVanillaTree(level, rootPos, random);
        }
        return dynamicAttempt
                && generateVanillaTree(level, rootPos, random);
    }

    private static boolean generateVanillaTree(
            ServerLevel level,
            BlockPos rootPos,
            RandomSource random
    ) {
        BlockPos saplingPos = rootPos.above();
        SaplingBlock sapling = chooseSapling(level.getBiome(rootPos));
        BlockState state = sapling.defaultBlockState()
                .setValue(SaplingBlock.STAGE, 1);
        level.setBlock(saplingPos, state, Block.UPDATE_ALL);
        sapling.advanceTree(level, saplingPos, state, random);
        return !(level.getBlockState(saplingPos).getBlock()
                instanceof SaplingBlock);
    }

    private static SaplingBlock chooseSapling(Holder<Biome> biome) {
        float temperature = biome.value().getBaseTemperature();
        if (temperature < 0.35F) {
            return (SaplingBlock) Blocks.SPRUCE_SAPLING;
        }
        if (temperature > 1.4F) {
            return (SaplingBlock) Blocks.ACACIA_SAPLING;
        }
        return (SaplingBlock) Blocks.OAK_SAPLING;
    }

    private static boolean tooCloseToAnotherTree(
            BlockPos candidate,
            List<BlockPos> generated
    ) {
        for (BlockPos other : generated) {
            long dx = candidate.getX() - other.getX();
            long dz = candidate.getZ() - other.getZ();
            if (dx * dx + dz * dz < 36L) {
                return true;
            }
        }
        return false;
    }

    private static void markTreePassComplete(LevelChunk chunk) {
        chunk.setData(ModAttachments.URBAN_TREES_COMPLETE, true);
        chunk.setData(ModAttachments.URBAN_TREES_DENSE_COMPLETE, true);
        chunk.setUnsaved(true);
    }

    private static BuildingInfo asBuildingInfo(
            ILostChunkInfo chunkInfo
    ) {
        return chunkInfo instanceof BuildingInfo buildingInfo
                ? buildingInfo
                : null;
    }

    private static void enqueue(
            ServerLevel level,
            int chunkX,
            int chunkZ
    ) {
        PENDING.add(
                new PendingChunk(level.dimension(), chunkX, chunkZ)
        );
    }

    private static long facadeSeed(
            long worldSeed,
            int chunkX,
            int chunkZ,
            Direction direction
    ) {
        long seed = worldSeed;
        seed ^= (long) chunkX * 341873128712L;
        seed ^= (long) chunkZ * 132897987541L;
        seed ^= (long) direction.get2DDataValue()
                * 42317861L;
        return seed ^ 0x56494E455F464143L;
    }

    private static long treeSeed(
            long worldSeed,
            int chunkX,
            int chunkZ
    ) {
        return worldSeed
                ^ (long) chunkX * 4987142L
                ^ (long) chunkZ * 5947611L
                ^ 0x524F54574952454CL;
    }

    private record PendingChunk(
            ResourceKey<Level> dimension,
            int chunkX,
            int chunkZ
    ) {
    }

    private LostCitiesOvergrowth() {
    }
}
