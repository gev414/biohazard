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

    public BruteEntity(EntityType<? extends BruteEntity> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, 60.0D)
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