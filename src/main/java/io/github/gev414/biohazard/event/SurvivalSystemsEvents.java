package io.github.gev414.biohazard.event;

import io.github.gev414.biohazard.config.SurvivalSystemsConfig;
import io.github.gev414.biohazard.encumbrance.EncumbranceManager;
import io.github.gev414.biohazard.stealth.AttentionManager;
import io.github.gev414.biohazard.stealth.AwarenessManager;
import io.github.gev414.biohazard.stealth.PointBlankAttention;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class SurvivalSystemsEvents {

    public static void onServerTick(ServerTickEvent.Post event) {
        EncumbranceManager.onServerTick(event);
        AwarenessManager.onServerTick(event);
        AttentionManager.onServerTick(event);
        SurvivalStatusSync.onServerTick(event);
    }

    public static void onLivingChangeTarget(
            LivingChangeTargetEvent event
    ) {
        AwarenessManager.onLivingChangeTarget(event);
    }

    public static void onIncomingDamage(
            LivingIncomingDamageEvent event
    ) {
        if (!SurvivalSystemsConfig.ENABLED.get()
                || !(event.getEntity() instanceof Mob mob)
                || !AwarenessManager.isAffected(mob)) {
            return;
        }
        Entity attacker = event.getSource().getEntity();
        if (!(attacker instanceof ServerPlayer player)) {
            return;
        }

        AttentionManager.alertDirectlyAttacked(mob, player);
        Entity direct = event.getSource().getDirectEntity();
        if (direct == player
                && event.getSource().is(DamageTypes.PLAYER_ATTACK)
                && player.level() instanceof ServerLevel level) {
            AttentionManager.emit(
                    level,
                    player.position(),
                    player,
                    SurvivalSystemsConfig.MELEE_ATTENTION_RANGE.get(),
                    false
            );
        }
    }

    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!SurvivalSystemsConfig.ENABLED.get()
                || !(event.getPlayer() instanceof ServerPlayer player)
                || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        BlockState state = event.getState();
        float progress = state.getDestroyProgress(
                player,
                event.getLevel(),
                event.getPos()
        );
        if (progress <= 0.0F || progress >= 1.0F) {
            return;
        }
        AttentionManager.emit(
                level,
                event.getPos().getCenter(),
                player,
                SurvivalSystemsConfig.BLOCK_BREAK_ATTENTION_RANGE.get(),
                false
        );
    }

    public static void onSoundAtPosition(
            PlayLevelSoundEvent.AtPosition event
    ) {
        PointBlankAttention.onSoundAtPosition(event);
    }

    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        AttentionManager.onEntityJoinLevel(event);
    }

    public static void onPlayerLoggedIn(
            PlayerEvent.PlayerLoggedInEvent event
    ) {
        EncumbranceManager.onPlayerLoggedIn(event);
        SurvivalStatusSync.onPlayerLoggedIn(event);
    }

    public static void onPlayerLoggedOut(
            PlayerEvent.PlayerLoggedOutEvent event
    ) {
        EncumbranceManager.onPlayerLoggedOut(event);
        AwarenessManager.onPlayerLoggedOut(event);
        SurvivalStatusSync.onPlayerLoggedOut(event);
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        EncumbranceManager.onServerStopped(event);
        AwarenessManager.onServerStopped(event);
        AttentionManager.onServerStopped(event);
        SurvivalStatusSync.onServerStopped(event);
        PointBlankAttention.clear();
    }

    private SurvivalSystemsEvents() {
    }
}
