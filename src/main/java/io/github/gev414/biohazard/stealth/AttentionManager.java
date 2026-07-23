package io.github.gev414.biohazard.stealth;

import io.github.gev414.biohazard.config.SurvivalSystemsConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class AttentionManager {

    private static final ResourceLocation ZOMBIE_TACTICS_MARKER =
            ResourceLocation.fromNamespaceAndPath(
                    "zombie_tactics",
                    "marker"
            );
    private static final String APPROVED_MARKER_TAG =
            "biohazard_attention_marker";
    private static final boolean ZOMBIE_TACTICS_LOADED =
            ModList.get().isLoaded("zombie_tactics");
    private static final Map<UUID, Investigation> INVESTIGATIONS =
            new HashMap<>();

    private static int ticksUntilRefresh;

    public static void emit(
            ServerLevel level,
            Vec3 position,
            ServerPlayer source,
            double range,
            boolean createTacticsMarker
    ) {
        if (!SurvivalSystemsConfig.ENABLED.get() || range <= 0.0D) {
            return;
        }
        AwarenessManager.markNoisy(source);
        long expiry = level.getGameTime() + Math.max(
                60L,
                Math.round(range * 2.0D)
        );
        AABB area = new AABB(position, position).inflate(range);
        for (Mob mob : level.getEntitiesOfClass(
                Mob.class,
                area,
                candidate -> AwarenessManager.isAffected(candidate)
                        && candidate.isAlive()
        )) {
            INVESTIGATIONS.put(
                    mob.getUUID(),
                    new Investigation(mob, position, expiry)
            );
            if (mob.getTarget() == null) {
                mob.getNavigation().moveTo(
                        position.x,
                        position.y,
                        position.z,
                        1.1D
                );
            }
        }
        if (createTacticsMarker) {
            spawnTacticsMarker(level, position, range);
        }
    }

    public static void alertDirectlyAttacked(
            Mob mob,
            ServerPlayer player
    ) {
        AwarenessManager.markNoisy(player);
        AwarenessManager.alert(mob, player);
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        if (ticksUntilRefresh > 0) {
            ticksUntilRefresh--;
            return;
        }
        ticksUntilRefresh = 9;

        Iterator<Map.Entry<UUID, Investigation>> iterator =
                INVESTIGATIONS.entrySet().iterator();
        while (iterator.hasNext()) {
            Investigation investigation = iterator.next().getValue();
            Mob mob = investigation.mob;
            if (!mob.isAlive()
                    || mob.isRemoved()
                    || mob.level().getGameTime() > investigation.expiry) {
                iterator.remove();
                continue;
            }
            if (mob.getTarget() == null
                    && mob.getNavigation().isDone()
                    && mob.distanceToSqr(investigation.position)
                    > 2.25D) {
                mob.getNavigation().moveTo(
                        investigation.position.x,
                        investigation.position.y,
                        investigation.position.z,
                        1.1D
                );
            }
        }
    }

    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!ZOMBIE_TACTICS_LOADED
                || !SurvivalSystemsConfig.ENABLED.get()
                || !SurvivalSystemsConfig
                .REPLACE_ZOMBIE_TACTICS_MARKERS
                .get()) {
            return;
        }
        Entity entity = event.getEntity();
        if (BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType())
                .equals(ZOMBIE_TACTICS_MARKER)
                && !entity.getPersistentData()
                .getBoolean(APPROVED_MARKER_TAG)) {
            event.setCanceled(true);
        }
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        INVESTIGATIONS.clear();
        ticksUntilRefresh = 0;
    }

    private static void spawnTacticsMarker(
            ServerLevel level,
            Vec3 position,
            double maximumRange
    ) {
        if (!ZOMBIE_TACTICS_LOADED
                || zombieTacticsMarkerRange() > maximumRange) {
            return;
        }
        EntityType<?> markerType = BuiltInRegistries.ENTITY_TYPE
                .getOptional(ZOMBIE_TACTICS_MARKER)
                .orElse(null);
        if (markerType == null) {
            return;
        }
        Entity marker = markerType.create(level);
        if (marker == null) {
            return;
        }
        marker.setPos(position);
        marker.getPersistentData().putBoolean(
                APPROVED_MARKER_TAG,
                true
        );
        level.addFreshEntity(marker);
    }

    private static double zombieTacticsMarkerRange() {
        try {
            Class<?> config = Class.forName(
                    "n643064.zombie_tactics.Config"
            );
            return Math.max(
                    0.0D,
                    config.getField("markerRange").getDouble(null)
            );
        } catch (ReflectiveOperationException exception) {
            return Double.POSITIVE_INFINITY;
        }
    }

    private record Investigation(
            Mob mob,
            Vec3 position,
            long expiry
    ) {
    }

    private AttentionManager() {
    }
}
