package io.github.gev414.biohazard.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;
import io.github.gev414.biohazard.entity.ai.RockThrowAttackGoal;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import io.github.gev414.biohazard.entity.projectile.BruteRockProjectile;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class BruteEntity extends Zombie implements RangedAttackMob {

    private static final int ROCK_ATTACK_GOAL_PRIORITY = 0;

    private static final double ROCK_MINIMUM_RANGE = 6.0D;
    private static final double ROCK_MAXIMUM_RANGE = 18.0D;

    private static final int ROCK_WINDUP_TICKS = 20;
    private static final int ROCK_COOLDOWN_TICKS = 120;

    public static final float SCALE = 1.5F;

    public static final float WIDTH = 0.6F * SCALE;
    public static final float HEIGHT = 1.95F * SCALE;
    public static final float EYE_HEIGHT = 1.74F * SCALE;

    private static final int PLAYER_TARGET_ACQUISITION_GRACE_TICKS = 40;

    // 100 is twenty times the normal monster base reward of 5.
    private static final int BRUTE_XP_REWARD = 100;

    private final ServerBossEvent bossEvent;

    private final Set<ServerPlayer> trackingPlayers = new HashSet<>();
    private final Set<UUID> combatParticipantIds = new HashSet<>();

    private boolean playerEngagementActive;
    private long lastPlayerDamageGameTime = Long.MIN_VALUE;

    public BruteEntity(
            EntityType<? extends BruteEntity> entityType,
            Level level
    ) {
        super(entityType, level);

        this.bossEvent = new ServerBossEvent(
                this.getDisplayName(),
                BossEvent.BossBarColor.RED,
                BossEvent.BossBarOverlay.PROGRESS
        );

        this.bossEvent.setDarkenScreen(false);
        this.bossEvent.setPlayBossMusic(false);
        this.bossEvent.setCreateWorldFog(false);
        this.bossEvent.setVisible(false);

        this.xpReward = BRUTE_XP_REWARD;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, 250.0D)
                .add(Attributes.ATTACK_DAMAGE, 8.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 1.0D)
                .add(Attributes.ARMOR, 6.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5D)
                .add(Attributes.MOVEMENT_SPEED, 0.23D)
                .add(Attributes.FOLLOW_RANGE, 40.0D);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();

        this.goalSelector.addGoal(
                ROCK_ATTACK_GOAL_PRIORITY,
                new RockThrowAttackGoal<>(
                        this,
                        ROCK_MINIMUM_RANGE,
                        ROCK_MAXIMUM_RANGE,
                        ROCK_WINDUP_TICKS,
                        ROCK_COOLDOWN_TICKS
                )
        );
    }

    // start BOSS BAR implementation

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.trackingPlayers.add(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);

        this.trackingPlayers.remove(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void setCustomName(Component customName) {
        super.setCustomName(customName);
        this.bossEvent.setName(this.getDisplayName());
    }

    @Override
    public boolean hurt(DamageSource damageSource, float amount) {
        boolean wasHurt = super.hurt(damageSource, amount);

        if (wasHurt
                && this.isAlive()
                && damageSource.getEntity()
                instanceof ServerPlayer attacker) {
            this.combatParticipantIds.add(attacker.getUUID());
            this.lastPlayerDamageGameTime =
                    this.level().getGameTime();
        }

        return wasHurt;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();

        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        float progress = Mth.clamp(
                this.getHealth() / this.getMaxHealth(),
                0.0F,
                1.0F
        );

        this.bossEvent.setProgress(progress);
        this.updateBossBar(serverLevel);
    }

    private void updateBossBar(ServerLevel serverLevel) {
        // Defensive cleanup for logout or dimension-change edge cases.
        this.trackingPlayers.removeIf(
                player -> !serverLevel.players().contains(player)
        );

        ServerPlayer target =
                this.getEngagedPlayerTarget(serverLevel);

        if (target == null) {
            this.hideBossBarOutsideCombat(
                    serverLevel.getGameTime()
            );
            return;
        }

        this.playerEngagementActive = true;
        this.combatParticipantIds.add(target.getUUID());
        this.bossEvent.setVisible(true);

        // Remove viewers who are no longer eligible.
        for (ServerPlayer viewer
                : Set.copyOf(this.bossEvent.getPlayers())) {
            if (!this.shouldSeeBossBar(viewer, serverLevel)) {
                this.bossEvent.removePlayer(viewer);
            }
        }

        // Add eligible tracked participants.
        for (ServerPlayer player : this.trackingPlayers) {
            if (this.shouldSeeBossBar(player, serverLevel)) {
                this.bossEvent.addPlayer(player);
            }
        }
    }

    private ServerPlayer getEngagedPlayerTarget(
            ServerLevel serverLevel
    ) {
        if (!this.isAlive()
                || !(this.getTarget()
                instanceof ServerPlayer target)) {
            return null;
        }

        if (!serverLevel.players().contains(target)
                || target.level() != serverLevel
                || !target.isAlive()
                || target.isSpectator()
                || target.isCreative()) {
            return null;
        }

        return target;
    }

    private boolean shouldSeeBossBar(
            ServerPlayer player,
            ServerLevel serverLevel
    ) {
        return this.playerEngagementActive
                && this.trackingPlayers.contains(player)
                && serverLevel.players().contains(player)
                && player.isAlive()
                && !player.isSpectator()
                && this.combatParticipantIds.contains(
                player.getUUID()
        );
    }

    private void hideBossBarOutsideCombat(long gameTime) {
        this.bossEvent.setVisible(false);
        this.bossEvent.removeAllPlayers();

        boolean pendingDamageExpired =
                this.lastPlayerDamageGameTime == Long.MIN_VALUE
                        || gameTime
                        - this.lastPlayerDamageGameTime
                        > PLAYER_TARGET_ACQUISITION_GRACE_TICKS;

        /*
         * Preserve initial attackers briefly while the AI acquires
         * its first target. Once an established fight ends, clear
         * everyone immediately.
         */
        if (this.playerEngagementActive
                || pendingDamageExpired) {
            this.combatParticipantIds.clear();
            this.lastPlayerDamageGameTime = Long.MIN_VALUE;
        }

        this.playerEngagementActive = false;
    }

    private void clearBossBarState() {
        this.bossEvent.setVisible(false);
        this.bossEvent.removeAllPlayers();

        this.trackingPlayers.clear();
        this.combatParticipantIds.clear();

        this.playerEngagementActive = false;
        this.lastPlayerDamageGameTime = Long.MIN_VALUE;
    }

    @Override
    public void die(DamageSource damageSource) {
        super.die(damageSource);
        this.clearBossBarState();
    }

    @Override
    public void remove(Entity.RemovalReason removalReason) {
        this.clearBossBarState();
        super.remove(removalReason);
    }

    // end BOSS BAR implementation

    @Override
    public void performRangedAttack(
            LivingEntity target,
            float distanceFactor
    ) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        BruteRockProjectile projectile =
                new BruteRockProjectile(serverLevel, this);

        double directionX = target.getX() - this.getX();
        double directionY =
                target.getY(0.5D) - projectile.getY();
        double directionZ = target.getZ() - this.getZ();

        double horizontalDistance = Math.sqrt(
                directionX * directionX
                        + directionZ * directionZ
        );

        projectile.shoot(
                directionX,
                directionY + horizontalDistance * 0.2D,
                directionZ,
                1.4F,
                2.0F + distanceFactor * 2.0F
        );

        this.playSound(
                SoundEvents.IRON_GOLEM_ATTACK,
                1.0F,
                0.8F + this.getRandom().nextFloat() * 0.2F
        );

        serverLevel.addFreshEntity(projectile);
    }
}