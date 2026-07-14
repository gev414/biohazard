package io.github.gev414.biohazard.entity.projectile;

import io.github.gev414.biohazard.entity.ModEntities;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public final class BruteRockProjectile extends ThrowableItemProjectile {

    private static final float DAMAGE = 6.0F;

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
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);

        Entity target = result.getEntity();

        target.hurt(
                this.damageSources().thrown(this, this.getOwner()),
                DAMAGE
        );
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);

        if (!this.level().isClientSide) {
            this.discard();
        }
    }
}