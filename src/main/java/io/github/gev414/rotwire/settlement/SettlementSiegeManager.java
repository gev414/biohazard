package io.github.gev414.rotwire.settlement;

import io.github.gev414.rotwire.config.SettlementConfig;
import io.github.gev414.rotwire.mob.ai.CoordinatedHostileAi;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.EventHooks;

import java.util.Optional;

/**
 * Drives the small, loaded-area-only siege lifecycle for operational primary
 * camps. Every transition is persisted by the surrounding settlement data.
 */
final class SettlementSiegeManager {

    private static final String SETTLEMENT_TAG = "rotwireSiegeSettlement";

    static boolean tick(MinecraftServer server, Settlement settlement) {
        if (!SettlementConfig.SIEGES_ENABLED.get()) {
            return cancel(settlement);
        }
        Optional<SettlementRadioStatus> radio = settlement.primaryRadio();
        if (radio.isEmpty() || !settlement.canRunSieges(
                SettlementConfig.SIEGE_MINIMUM_POPULATION.get()
        )) {
            return cancel(settlement);
        }
        ServerLevel level = levelFor(server, settlement);
        if (level == null) {
            return false;
        }
        long now = level.getGameTime();
        return switch (settlement.snapshot().siegeState()) {
            case CALM -> beginSchedule(server, level, settlement, radio.get(), now);
            case WARNING -> beginAssault(
                    server,
                    level,
                    settlement,
                    radio.get(),
                    now
            );
            case ACTIVE -> runAssault(
                    server,
                    level,
                    settlement,
                    radio.get(),
                    now
            );
            case RECOVERY -> finishRecovery(
                    server,
                    level,
                    settlement,
                    radio.get(),
                    now
            );
        };
    }

    static boolean startTestSiege(
            MinecraftServer server,
            ServerLevel level,
            Settlement settlement
    ) {
        Optional<SettlementRadioStatus> radio = settlement.primaryRadio();
        if (radio.isEmpty() || !settlement.canRunSieges(
                SettlementConfig.SIEGE_MINIMUM_POPULATION.get()
        ) || settlement.snapshot().siegeState() == SettlementSiegeState.ACTIVE) {
            return false;
        }
        long now = level.getGameTime();
        if (!settlement.beginSiege(
                now + SettlementConfig.SIEGE_DURATION_TICKS.get(),
                now + ticksUntilNightfall(level)
        )) {
            return false;
        }
        broadcast(server, level, radio.get().campCenter(),
                "message.rotwire.siege.active");
        return true;
    }

    static boolean cancelTestSiege(
            ServerLevel level,
            Settlement settlement
    ) {
        Optional<SettlementRadioStatus> radio = settlement.primaryRadio();
        if (radio.isPresent()) {
            clearSiegeZombies(level, settlement, radio.get().campCenter());
        }
        return settlement.setSiegeSchedule(SettlementSiegeState.CALM, -1L);
    }

    private static boolean beginSchedule(
            MinecraftServer server,
            ServerLevel level,
            Settlement settlement,
            SettlementRadioStatus radio,
            long now
    ) {
        long next = settlement.snapshot().nextSiegeAt();
        if (next < 0L) {
            return settlement.setSiegeSchedule(
                    SettlementSiegeState.CALM,
                    now + SettlementConfig.SIEGE_INTERVAL_TICKS.get()
            );
        }
        if (next > now) {
            return false;
        }
        broadcast(server, level, radio.campCenter(),
                "message.rotwire.siege.warning");
        return settlement.setSiegeSchedule(
                SettlementSiegeState.WARNING,
                now + SettlementConfig.SIEGE_WARNING_TICKS.get()
        );
    }

    private static boolean beginAssault(
            MinecraftServer server,
            ServerLevel level,
            Settlement settlement,
            SettlementRadioStatus radio,
            long now
    ) {
        if (now < settlement.snapshot().nextSiegeAt()) {
            return false;
        }
        broadcast(server, level, radio.campCenter(),
                "message.rotwire.siege.active");
        return settlement.beginSiege(
                now + SettlementConfig.SIEGE_DURATION_TICKS.get(),
                now + ticksUntilNightfall(level)
        );
    }

    private static boolean runAssault(
            MinecraftServer server,
            ServerLevel level,
            Settlement settlement,
            SettlementRadioStatus radio,
            long now
    ) {
        boolean attended = hasNearbyPlayer(level, radio.campCenter());
        boolean changed = false;
        if (attended) {
            changed = settlement.markSiegeAttended();
        }
        if (settlement.isUnattendedSiegeDue(now)) {
            int lossPercent = 60 + level.getRandom().nextInt(21);
            settlement.resolveVirtualSiege(
                    lossPercent,
                    now + SettlementConfig.SIEGE_RECOVERY_TICKS.get()
            );
            settlement.applyPendingRaids(level);
            broadcastFall(server, settlement);
            return true;
        }
        if (now >= settlement.snapshot().nextSiegeAt()) {
            clearSiegeZombies(level, settlement, radio.campCenter());
            broadcast(server, level, radio.campCenter(),
                    "message.rotwire.siege.recovery");
            return settlement.setSiegeSchedule(
                    SettlementSiegeState.RECOVERY,
                    now + SettlementConfig.SIEGE_RECOVERY_TICKS.get()
            );
        }
        if (!attended) {
            return false;
        }
        if (isInterval(now, SettlementConfig.SIEGE_SPAWN_INTERVAL_TICKS.get())) {
            changed |= spawnWave(level, settlement, radio);
        }
        if (isInterval(
                now,
                SettlementConfig.SIEGE_RESOURCE_DRAIN_INTERVAL_TICKS.get()
        )) {
            changed |= settlement.raidRations(
                    level,
                    SettlementConfig.SIEGE_RESOURCE_DRAIN_RATIONS.get()
            ) > 0;
        }
        return changed;
    }

    private static boolean finishRecovery(
            MinecraftServer server,
            ServerLevel level,
            Settlement settlement,
            SettlementRadioStatus radio,
            long now
    ) {
        if (now < settlement.snapshot().nextSiegeAt()) {
            return false;
        }
        broadcast(server, level, radio.campCenter(),
                "message.rotwire.siege.calm");
        return settlement.setSiegeSchedule(
                SettlementSiegeState.CALM,
                now + SettlementConfig.SIEGE_INTERVAL_TICKS.get()
        );
    }

    private static boolean cancel(Settlement settlement) {
        SettlementSnapshot snapshot = settlement.snapshot();
        if (snapshot.siegeState() == SettlementSiegeState.CALM
                && snapshot.nextSiegeAt() < 0L) {
            return false;
        }
        return settlement.setSiegeSchedule(SettlementSiegeState.CALM, -1L);
    }

    private static boolean spawnWave(
            ServerLevel level,
            Settlement settlement,
            SettlementRadioStatus radio
    ) {
        int remainingCapacity = SettlementConfig.SIEGE_NEARBY_CAP.get()
                - activeZombieCount(level, settlement, radio.campCenter());
        int attempts = Math.min(
                Math.max(0, remainingCapacity),
                SettlementConfig.SIEGE_ZOMBIES_PER_WAVE.get()
        );
        boolean spawned = false;
        for (int zombie = 0; zombie < attempts; zombie++) {
            spawned |= spawnZombie(level, settlement, radio);
        }
        return spawned;
    }

    private static boolean spawnZombie(
            ServerLevel level,
            Settlement settlement,
            SettlementRadioStatus radio
    ) {
        if (level.getDifficulty() == Difficulty.PEACEFUL
                || !level.getGameRules().getBoolean(
                GameRules.RULE_DOMOBSPAWNING
        )) {
            return false;
        }
        Zombie zombie = EntityType.ZOMBIE.create(level);
        if (zombie == null) {
            return false;
        }
        BlockPos position = findSpawnPosition(level, radio.campCenter(), zombie);
        if (position == null) {
            zombie.discard();
            return false;
        }
        EventHooks.finalizeMobSpawn(
                zombie,
                level,
                level.getCurrentDifficultyAt(position),
                MobSpawnType.EVENT,
                null
        );
        zombie.getPersistentData().putUUID(SETTLEMENT_TAG, settlement.id());
        zombie.getPersistentData().putLong(
                CoordinatedHostileAi.ASSAULT_TARGET_TAG,
                radio.campCenter().asLong()
        );
        zombie.setPersistenceRequired();
        return level.tryAddFreshEntityWithPassengers(zombie);
    }

    private static BlockPos findSpawnPosition(
            ServerLevel level,
            BlockPos target,
            Zombie zombie
    ) {
        int minimum = Math.min(
                SettlementConfig.SIEGE_MINIMUM_SPAWN_DISTANCE.get(),
                SettlementConfig.SIEGE_MAXIMUM_SPAWN_DISTANCE.get()
        );
        int maximum = Math.max(
                SettlementConfig.SIEGE_MINIMUM_SPAWN_DISTANCE.get(),
                SettlementConfig.SIEGE_MAXIMUM_SPAWN_DISTANCE.get()
        );
        RandomSource random = level.getRandom();
        for (int attempt = 0;
                attempt < SettlementConfig.SIEGE_SPAWN_POSITION_ATTEMPTS.get();
                attempt++) {
            double angle = random.nextDouble() * Math.TAU;
            double distance = minimum + random.nextDouble() * (maximum - minimum);
            int x = (int) Math.floor(target.getX() + Math.cos(angle) * distance);
            int z = (int) Math.floor(target.getZ() + Math.sin(angle) * distance);
            if (!level.hasChunk(x >> 4, z >> 4)) {
                continue;
            }
            BlockPos candidate = new BlockPos(
                    x,
                    level.getHeight(
                            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                            x,
                            z
                    ),
                    z
            );
            if (!level.isAreaLoaded(candidate, 1)
                    || !level.isPositionEntityTicking(candidate)
                    || !level.getWorldBorder().isWithinBounds(candidate)
                    || !level.canSeeSky(candidate)
                    || !level.getFluidState(candidate).isEmpty()
                    || !level.getBlockState(candidate.below()).isFaceSturdy(
                            level,
                            candidate.below(),
                            net.minecraft.core.Direction.UP
                    )) {
                continue;
            }
            zombie.moveTo(
                    candidate.getX() + 0.5D,
                    candidate.getY(),
                    candidate.getZ() + 0.5D,
                    random.nextFloat() * 360.0F,
                    0.0F
            );
            if (level.noCollision(zombie) && zombie.checkSpawnObstruction(level)) {
                return candidate;
            }
        }
        return null;
    }

    private static int activeZombieCount(
            ServerLevel level,
            Settlement settlement,
            BlockPos target
    ) {
        int radius = SettlementConfig.SIEGE_MAXIMUM_SPAWN_DISTANCE.get() + 32;
        return level.getEntitiesOfClass(
                Zombie.class,
                new AABB(target).inflate(radius, 32.0D, radius),
                zombie -> zombie.isAlive()
                        && zombie.getPersistentData().hasUUID(SETTLEMENT_TAG)
                        && settlement.id().equals(
                        zombie.getPersistentData().getUUID(SETTLEMENT_TAG)
                )
        ).size();
    }

    private static void clearSiegeZombies(
            ServerLevel level,
            Settlement settlement,
            BlockPos target
    ) {
        int radius = SettlementConfig.SIEGE_MAXIMUM_SPAWN_DISTANCE.get() + 48;
        for (Zombie zombie : level.getEntitiesOfClass(
                Zombie.class,
                new AABB(target).inflate(radius, 32.0D, radius),
                candidate -> candidate.getPersistentData().hasUUID(SETTLEMENT_TAG)
                        && settlement.id().equals(candidate.getPersistentData()
                        .getUUID(SETTLEMENT_TAG))
        )) {
            zombie.discard();
        }
    }

    private static boolean hasNearbyPlayer(ServerLevel level, BlockPos target) {
        double maximum = SettlementConfig.SIEGE_PLAYER_ACTIVATION_DISTANCE.get();
        double maximumSqr = maximum * maximum;
        return level.players().stream().anyMatch(player -> !player.isSpectator()
                && player.isAlive()
                && player.distanceToSqr(
                target.getX() + 0.5D,
                target.getY(),
                target.getZ() + 0.5D
        ) <= maximumSqr);
    }

    private static void broadcast(
            MinecraftServer server,
            ServerLevel level,
            BlockPos target,
            String message
    ) {
        double maximum = SettlementConfig.SIEGE_PLAYER_ACTIVATION_DISTANCE.get();
        double maximumSqr = maximum * maximum;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.serverLevel() == level
                    && player.distanceToSqr(
                    target.getX() + 0.5D,
                    target.getY(),
                    target.getZ() + 0.5D
            ) <= maximumSqr) {
                player.sendSystemMessage(net.minecraft.network.chat.Component
                        .translatable(message));
            }
        }
    }

    private static void broadcastFall(
            MinecraftServer server,
            Settlement settlement
    ) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(net.minecraft.network.chat.Component
                    .translatable(
                            "message.rotwire.siege.fallen",
                            settlement.snapshot().name()
                    ));
        }
    }

    private static long ticksUntilNightfall(ServerLevel level) {
        int timeOfDay = (int) Math.floorMod(level.getDayTime(), 24_000L);
        return timeOfDay < 13_000
                ? 13_000L - timeOfDay
                : 37_000L - timeOfDay;
    }

    private static boolean isInterval(long time, int interval) {
        return interval > 0 && time % interval == 0L;
    }

    private static ServerLevel levelFor(
            MinecraftServer server,
            Settlement settlement
    ) {
        ResourceKey<Level> key = ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION,
                settlement.cityZone().dimension()
        );
        return server.getLevel(key);
    }

    private SettlementSiegeManager() {
    }
}
