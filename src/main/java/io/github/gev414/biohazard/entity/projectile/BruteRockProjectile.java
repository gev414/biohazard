package io.github.gev414.biohazard.entity.projectile;

import io.github.gev414.biohazard.damage.ModDamageTypes;
import io.github.gev414.biohazard.entity.ModEntities;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class BruteRockProjectile extends ThrowableItemProjectile {

    private static final float DIRECT_DAMAGE = 6.0F;
    private static final double DIRECT_EXTRA_KNOCKBACK = 0.6D;

    private static final float SPLASH_DAMAGE = 4.0F;
    private static final double SPLASH_RADIUS = 2.0D;
    private static final double SPLASH_RADIUS_SQUARED =
            SPLASH_RADIUS * SPLASH_RADIUS;

    private static final double IMPACT_POSITION_OFFSET = 0.01D;

    public BruteRockProjectile(
            EntityType<? extends BruteRockProjectile> entityType,
            Level level
    ) {
        super(entityType, level);
    }

    public BruteRockProjectile(Level level, LivingEntity owner) {
        super(ModEntities.BRUTE_ROCK.get(), owner, level);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.COBBLESTONE;
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);

        if (this.level().isClientSide) {
            return;
        }

        Vec3 impactPosition = getImpactPosition(result);

        Entity directTarget =
                result instanceof EntityHitResult entityHitResult
                        ? entityHitResult.getEntity()
                        : null;

        if (directTarget != null
                && directTarget != this.getOwner()) {
            hurtDirectTarget(directTarget);
        }

        hurtSplashTargets(
                impactPosition,
                directTarget
        );

        this.discard();
    }

    private void hurtDirectTarget(Entity target) {
        DamageSource directDamageSource =
                this.damageSources().thrown(
                        this,
                        this.getOwner()
                );

        boolean wasDamaged = target.hurt(
                directDamageSource,
                DIRECT_DAMAGE
        );

        if (wasDamaged
                && target instanceof LivingEntity livingTarget) {
            Vec3 flightDirection = this.getDeltaMovement();

            livingTarget.knockback(
                    DIRECT_EXTRA_KNOCKBACK,
                    -flightDirection.x,
                    -flightDirection.z
            );
        }
    }

    private void hurtSplashTargets(
            Vec3 impactPosition,
            Entity directTarget
    ) {
        double splashDiameter = SPLASH_RADIUS * 2.0D;

        AABB splashBounds = AABB.ofSize(
                impactPosition,
                splashDiameter,
                splashDiameter,
                splashDiameter
        );

        Entity owner = this.getOwner();

        DamageSource splashDamageSource =
                new DamageSource(
                        this.level()
                                .registryAccess()
                                .registryOrThrow(
                                        Registries.DAMAGE_TYPE
                                )
                                .getHolderOrThrow(
                                        ModDamageTypes.BRUTE_ROCK_SPLASH
                                ),
                        impactPosition
                );

        for (LivingEntity target :
                this.level().getEntitiesOfClass(
                        LivingEntity.class,
                        splashBounds,
                        LivingEntity::isAlive
                )) {
            if (target == owner
                    || target == directTarget) {
                continue;
            }

            if (target.getBoundingBox()
                    .distanceToSqr(impactPosition)
                    > SPLASH_RADIUS_SQUARED) {
                continue;
            }

            if (Explosion.getSeenPercent(
                    impactPosition,
                    target
            ) <= 0.0F) {
                continue;
            }

            target.hurt(
                    splashDamageSource,
                    SPLASH_DAMAGE
            );
        }
    }

    private static Vec3 getImpactPosition(HitResult result) {
        if (result instanceof BlockHitResult blockHitResult) {
            Vec3 outwardNormal = Vec3.atLowerCornerOf(
                    blockHitResult
                            .getDirection()
                            .getNormal()
            );

            return blockHitResult
                    .getLocation()
                    .add(
                            outwardNormal.scale(
                                    IMPACT_POSITION_OFFSET
                            )
                    );
        }

        return result.getLocation();
    }
}