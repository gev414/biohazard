package io.github.gev414.rotwire.entity;

import io.github.gev414.rotwire.city.CityZoneKey;
import io.github.gev414.rotwire.config.SettlementConfig;
import io.github.gev414.rotwire.entity.ai.SurvivorCityRoamGoal;
import io.github.gev414.rotwire.entity.ai.SurvivorMosinAttackGoal;
import io.github.gev414.rotwire.entity.ai.SurvivorReturnToCampGoal;
import io.github.gev414.rotwire.entity.ai.SurvivorThreatAwarenessGoal;
import io.github.gev414.rotwire.settlement.SettlementManager;
import io.github.gev414.rotwire.settlement.SettlementSnapshot;
import io.github.gev414.rotwire.settlement.SettlementSiegeState;
import io.github.gev414.rotwire.settlement.SurvivorSafetyRules;
import mod.pbj.item.FireModeInstance;
import mod.pbj.item.GunItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistent non-trading settlement civilian. It extends
 * {@link AbstractVillager} so vanilla infected recognize it as a human target,
 * while all movement remains Rotwire's settlement-aware AI.
 */
public final class SurvivorEntity extends AbstractVillager {

    private static final String SETTLEMENT_ID_TAG = "settlementId";
    private static final String CITY_ZONE_TAG = "cityZone";
    private static final String OWNER_ID_TAG = "ownerId";
    private static final String HOME_POSITION_TAG = "homePosition";
    private static final String HOME_RADIUS_TAG = "homeRadius";
    private static final String ROLE_TAG = "role";
    private static final String RETURN_HOME_ORDERED_TAG = "returnHomeOrdered";

    private static final ResourceLocation MOSIN_ID =
            ResourceLocation.fromNamespaceAndPath("pointblank", "mosin");

    private static final double ROAM_SPEED = 0.80D;
    private static final double RETREAT_SPEED = 1.25D;
    private static final double MELEE_RANGE_SQR = 9.0D;

    @Nullable
    private UUID settlementId;
    @Nullable
    private CityZoneKey cityZone;
    @Nullable
    private UUID ownerId;
    @Nullable
    private BlockPos homePosition;
    private int homeRadius;
    private SurvivorRole role = SurvivorRole.CIVILIAN;
    private boolean returnHomeOrdered;

    public SurvivorEntity(
            EntityType<? extends SurvivorEntity> entityType,
            Level level
    ) {
        super(entityType, level);
        setCanPickUpLoot(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.10D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new SurvivorReturnToCampGoal(
                this,
                RETREAT_SPEED
        ));
        goalSelector.addGoal(2, new SurvivorMosinAttackGoal(this));
        goalSelector.addGoal(3, new SurvivorThreatAwarenessGoal(this));
        goalSelector.addGoal(5, new SurvivorCityRoamGoal(this, ROAM_SPEED));
        goalSelector.addGoal(7, new LookAtPlayerGoal(
                this,
                Player.class,
                8.0F
        ));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(
                this,
                Monster.class,
                10,
                true,
                false,
                monster -> isRifleman() && !shouldReturnToCamp()
        ));
    }

    /**
     * Attaches this physical survivor to the durable city settlement at the
     * moment it is called from the primary Camp Hub.
     */
    public void bindToSettlement(
            UUID settlementId,
            CityZoneKey cityZone,
            UUID ownerId,
            BlockPos homePosition,
            int homeRadius
    ) {
        this.settlementId = settlementId;
        this.cityZone = cityZone;
        this.ownerId = ownerId;
        this.homePosition = homePosition.immutable();
        this.homeRadius = Math.max(2, homeRadius);
        setPersistenceRequired();
    }

    /**
     * Gives this survivor a genuine PointBlank Mosin, with its magazine stored
     * on the firearm item itself so reloads and persistence use the same ammo
     * capacity as a player-held Mosin.
     */
    public boolean equipMosinRifle(int loadedRounds) {
        ItemStack rifle = createMosinStack();
        if (rifle.isEmpty() || !(rifle.getItem() instanceof GunItem gun)) {
            return false;
        }
        FireModeInstance fireMode = GunItem.getFireModeInstance(rifle);
        int capacity = Math.max(
                0,
                gun.getMaxAmmoCapacity(rifle, fireMode)
        );
        if (capacity <= 0) {
            return false;
        }
        GunItem.setAmmo(
                rifle,
                fireMode,
                Math.min(Math.max(0, loadedRounds), capacity)
        );
        role = SurvivorRole.RIFLEMAN;
        setItemSlot(EquipmentSlot.MAINHAND, rifle);
        setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        setCustomName(Component.translatable("entity.rotwire.rifleman"));
        return true;
    }

    public static int mosinMagazineCapacity() {
        ItemStack rifle = createMosinStack();
        if (rifle.isEmpty() || !(rifle.getItem() instanceof GunItem gun)) {
            return 0;
        }
        return Math.max(
                0,
                gun.getMaxAmmoCapacity(
                        rifle,
                        GunItem.getFireModeInstance(rifle)
                )
        );
    }

    public boolean isRifleman() {
        return role == SurvivorRole.RIFLEMAN;
    }

    public boolean canFightWithMosin() {
        return isRifleman()
                && mosinProfile().isPresent()
                && availableAmmunition() > 0
                && !shouldReturnToCamp();
    }

    public int mosinMaximumShootingDistance() {
        return mosinProfile()
                .map(MosinProfile::maximumShootingDistance)
                .orElse(0);
    }

    public int mosinMagazineRounds() {
        return mosinProfile().map(profile -> Math.max(
                0,
                GunItem.getAmmo(profile.stack(), profile.fireMode())
        )).orElse(0);
    }

    /**
     * Refills the PointBlank magazine from physical 7.62x51 stacks in this
     * survivor's settlement, returning true when at least one round loaded.
     */
    public boolean reloadMosinFromSettlement() {
        if (!(level() instanceof ServerLevel serverLevel)
                || cityZone == null
                || settlementId == null) {
            return false;
        }
        MosinProfile profile = mosinProfile().orElse(null);
        if (profile == null) {
            return false;
        }
        int currentRounds = GunItem.getAmmo(
                profile.stack(),
                profile.fireMode()
        );
        int requiredRounds = profile.capacity() - currentRounds;
        if (requiredRounds <= 0) {
            return false;
        }
        int withdrawn = SettlementManager.withdrawMosinAmmunition(
                serverLevel,
                cityZone,
                requiredRounds
        );
        if (withdrawn <= 0) {
            return false;
        }
        GunItem.setAmmo(
                profile.stack(),
                profile.fireMode(),
                currentRounds + withdrawn
        );
        return true;
    }

    /**
     * Fires one custom hitscan shot using the live Mosin fire-mode data. This
     * preserves the weapon's damage, fire sound, magazine, RPM, and PointBlank
     * headshot multiplier without relying on a fake player firing packet.
     */
    public boolean fireMosinAt(LivingEntity target) {
        MosinProfile profile = mosinProfile().orElse(null);
        if (profile == null || target == null || !target.isAlive()
                || isAlliedTo(target) || shouldReturnToCamp()) {
            return false;
        }
        int currentRounds = GunItem.getAmmo(
                profile.stack(),
                profile.fireMode()
        );
        if (currentRounds <= 0 || !getSensing().hasLineOfSight(target)) {
            return false;
        }

        Vec3 origin = getEyePosition();
        double distance = origin.distanceTo(target.getEyePosition());
        if (distance > profile.maximumShootingDistance()) {
            return false;
        }
        boolean headshot = random.nextDouble()
                < SettlementConfig.RIFLEMAN_HEADSHOT_CHANCE.get();
        Vec3 aimPoint = headshot
                ? target.getEyePosition()
                : target.getBoundingBox().getCenter();
        Vec3 direction = aimPoint.subtract(origin).normalize();
        if (distance > SettlementConfig.RIFLEMAN_PRECISE_RANGE.get()
                && random.nextDouble()
                < SettlementConfig.RIFLEMAN_LONG_RANGE_MISS_CHANCE.get()) {
            direction = intentionalMissDirection(direction);
        }

        Vec3 maximumEnd = origin.add(
                direction.scale(profile.maximumShootingDistance())
        );
        BlockHitResult blockHit = level().clip(new ClipContext(
                origin,
                maximumEnd,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                this
        ));
        Vec3 rayEnd = blockHit.getType() == HitResult.Type.MISS
                ? maximumEnd
                : blockHit.getLocation();
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                level(),
                this,
                origin,
                rayEnd,
                getBoundingBox().expandTowards(
                        direction.scale(profile.maximumShootingDistance())
                ).inflate(1.0D),
                entity -> entity instanceof LivingEntity living
                        && living.isAlive()
                        && !(entity instanceof Player)
                        && !isAlliedTo(entity)
        );

        GunItem.setAmmo(
                profile.stack(),
                profile.fireMode(),
                currentRounds - 1
        );
        swing(InteractionHand.MAIN_HAND, true);
        level().playSound(
                null,
                getX(),
                getY(),
                getZ(),
                profile.gun().getFireSound(),
                SoundSource.HOSTILE,
                profile.gun().getFireSoundVolume(),
                1.0F
        );
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ParticleTypes.SMOKE,
                    origin.x,
                    origin.y,
                    origin.z,
                    3,
                    direction.x * 0.05D,
                    direction.y * 0.05D,
                    direction.z * 0.05D,
                    0.01D
            );
        }
        if (entityHit != null
                && entityHit.getEntity() instanceof LivingEntity living) {
            float damage = profile.fireMode().getDamage();
            if (headshot) {
                damage *= profile.fireMode().getHeadshotMultiplier();
            }
            living.hurt(level().damageSources().mobAttack(this), damage);
        }
        return true;
    }

    public int mosinShotCooldownTicks() {
        return mosinProfile().map(profile -> Math.max(
                1,
                (int) Math.ceil(1_200.0D / Math.max(
                        1,
                        profile.fireMode().getRpm()
                ))
        )).orElse(20);
    }

    public boolean isBoundToSettlement() {
        return settlementId != null
                && cityZone != null
                && homePosition != null;
    }

    public @Nullable BlockPos homePosition() {
        return homePosition;
    }

    public boolean isWithinHomeRadius() {
        return homePosition != null
                && homePosition.distSqr(blockPosition())
                <= (double) homeRadius * homeRadius;
    }

    /**
     * Retreats and rally orders finish a few blocks from the central shelter
     * rather than at the outer campsite boundary. Treating the whole camp
     * radius as "home" previously left an unarmed survivor near its edge.
     */
    public boolean isAtCampRetreatPoint() {
        double distance = SettlementConfig
                .SURVIVOR_CAMP_RETREAT_DISTANCE.get();
        return homePosition != null
                && homePosition.distSqr(blockPosition())
                <= distance * distance;
    }

    public void orderReturnToCamp() {
        returnHomeOrdered = !isAtCampRetreatPoint();
        setTarget(null);
        getNavigation().stop();
    }

    public void completeReturnToCampOrder() {
        if (isAtCampRetreatPoint()) {
            returnHomeOrdered = false;
        }
    }

    public boolean canRoamCity() {
        return isBoundToSettlement() && !shouldReturnToCamp();
    }

    /**
     * Includes the shared guard policy even though this first civilian has no
     * combat loadout. This keeps future hitscan guard equipment from changing
     * the settlement's survival rules.
     */
    public boolean shouldReturnToCamp() {
        if (!isBoundToSettlement()) {
            return false;
        }
        if (!(level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        if (returnHomeOrdered) {
            return true;
        }

        SettlementSnapshot settlement = settlement(serverLevel).orElse(null);
        if (settlement == null) {
            return true;
        }

        int hostileCount = nearbyHostileCount();
        SurvivorSafetyRules.SafetyContext safetyContext =
                new SurvivorSafetyRules.SafetyContext(
                        settlement.siegeState(),
                        hasRangedLoadout(),
                        availableAmmunition(),
                        isInMeleeFight(),
                        hostileCount,
                        nearbyMeleeAllyCount()
                );
        return SurvivorSafetyRules.mustReturnToCamp(safetyContext)
                || (!isRifleman() && hostileCount > 0)
                || !isWithinCityRoamRadius();
    }

    public Optional<SettlementBinding> settlementBinding() {
        if (settlementId == null || cityZone == null) {
            return Optional.empty();
        }
        return Optional.of(new SettlementBinding(settlementId, cityZone));
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean isAlliedTo(Entity entity) {
        if (super.isAlliedTo(entity)) {
            return true;
        }
        if (entity instanceof Player player
                && ownerId != null
                && ownerId.equals(player.getUUID())) {
            return true;
        }
        return entity instanceof SurvivorEntity other
                && settlementId != null
                && settlementId.equals(other.settlementId);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        return InteractionResult.sidedSuccess(level().isClientSide());
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(
            ServerLevel level,
            AgeableMob otherParent
    ) {
        return null;
    }

    @Override
    public boolean showProgressBar() {
        return false;
    }

    @Override
    protected void rewardTradeXp(MerchantOffer offer) {
        // Civilians do not expose the villager trading system.
    }

    @Override
    protected void updateTrades() {
        // Civilians do not expose the villager trading system.
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.VILLAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.VILLAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.VILLAGER_DEATH;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (settlementId != null) {
            tag.putUUID(SETTLEMENT_ID_TAG, settlementId);
        }
        if (cityZone != null) {
            tag.put(CITY_ZONE_TAG, cityZone.save());
        }
        if (ownerId != null) {
            tag.putUUID(OWNER_ID_TAG, ownerId);
        }
        if (homePosition != null) {
            tag.putLong(HOME_POSITION_TAG, homePosition.asLong());
        }
        tag.putInt(HOME_RADIUS_TAG, homeRadius);
        tag.putString(ROLE_TAG, role.name());
        tag.putBoolean(RETURN_HOME_ORDERED_TAG, returnHomeOrdered);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        settlementId = tag.hasUUID(SETTLEMENT_ID_TAG)
                ? tag.getUUID(SETTLEMENT_ID_TAG)
                : null;
        cityZone = tag.contains(CITY_ZONE_TAG, Tag.TAG_COMPOUND)
                ? CityZoneKey.load(tag.getCompound(CITY_ZONE_TAG))
                : null;
        ownerId = tag.hasUUID(OWNER_ID_TAG)
                ? tag.getUUID(OWNER_ID_TAG)
                : null;
        homePosition = tag.contains(HOME_POSITION_TAG)
                ? BlockPos.of(tag.getLong(HOME_POSITION_TAG))
                : null;
        homeRadius = Math.max(2, tag.getInt(HOME_RADIUS_TAG));
        role = SurvivorRole.fromSavedName(tag.getString(ROLE_TAG));
        returnHomeOrdered = tag.getBoolean(RETURN_HOME_ORDERED_TAG);
        if (role == SurvivorRole.RIFLEMAN) {
            setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        }
        setPersistenceRequired();
    }

    private Optional<SettlementSnapshot> settlement(ServerLevel level) {
        return cityZone == null || settlementId == null
                ? Optional.empty()
                : SettlementManager.status(level, cityZone)
                .filter(snapshot -> settlementId.equals(snapshot.id()));
    }

    private boolean hasRangedLoadout() {
        return isRifleman() && mosinProfile().isPresent();
    }

    private int availableAmmunition() {
        MosinProfile profile = mosinProfile().orElse(null);
        if (profile == null) {
            return 0;
        }
        int magazine = Math.max(
                0,
                GunItem.getAmmo(profile.stack(), profile.fireMode())
        );
        if (!(level() instanceof ServerLevel serverLevel)) {
            return magazine;
        }
        int settlementRounds = settlement(serverLevel)
                .map(SettlementSnapshot::mosinAmmunition)
                .orElse(0);
        return (int) Math.min(
                Integer.MAX_VALUE,
                (long) magazine + settlementRounds
        );
    }

    private boolean isInMeleeFight() {
        LivingEntity target = getTarget();
        return target != null
                && target.isAlive()
                && distanceToSqr(target) <= MELEE_RANGE_SQR;
    }

    private int nearbyHostileCount() {
        int radius = SettlementConfig.CIVILIAN_HOSTILE_RETREAT_RADIUS.get();
        return level().getEntitiesOfClass(
                Monster.class,
                getBoundingBox().inflate(radius, 4.0D, radius),
                LivingEntity::isAlive
        ).size();
    }

    private int nearbyMeleeAllyCount() {
        int radius = SettlementConfig.CIVILIAN_HOSTILE_RETREAT_RADIUS.get();
        return level().getEntitiesOfClass(
                SurvivorEntity.class,
                getBoundingBox().inflate(radius, 4.0D, radius),
                survivor -> survivor.isAlliedTo(this)
                        && survivor.isInMeleeFight()
        ).size();
    }

    private boolean isWithinCityRoamRadius() {
        if (homePosition == null) {
            return true;
        }
        int radius = SettlementConfig.CIVILIAN_CITY_ROAM_RADIUS.get();
        return homePosition.distSqr(blockPosition())
                <= (double) radius * radius;
    }

    private Optional<MosinProfile> mosinProfile() {
        ItemStack stack = getMainHandItem();
        if (!isRifleman() || !(stack.getItem() instanceof GunItem gun)) {
            return Optional.empty();
        }
        FireModeInstance fireMode = GunItem.getFireModeInstance(stack);
        int capacity = gun.getMaxAmmoCapacity(stack, fireMode);
        if (capacity <= 0 || fireMode.getRpm() <= 0) {
            return Optional.empty();
        }
        return Optional.of(new MosinProfile(
                stack,
                gun,
                fireMode,
                capacity,
                Math.max(64, fireMode.getMaxShootingDistance())
        ));
    }

    private static ItemStack createMosinStack() {
        Item item = BuiltInRegistries.ITEM.get(MOSIN_ID);
        if (item == null || item == net.minecraft.world.item.Items.AIR) {
            return ItemStack.EMPTY;
        }
        ItemStack rifle = new ItemStack(item);
        // A bare PointBlank stack has no selected fire mode or ammo tag.
        // Initialize it exactly as PointBlank does for crafted firearms before
        // its magazine and fire-mode APIs are queried.
        GunItem.initStackForCrafting(rifle);
        return rifle;
    }

    private static Vec3 intentionalMissDirection(Vec3 direction) {
        Vec3 side = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (side.lengthSqr() < 1.0E-6D) {
            side = direction.cross(new Vec3(1.0D, 0.0D, 0.0D));
        }
        return direction.add(side.normalize().scale(0.15D)).normalize();
    }

    private enum SurvivorRole {
        CIVILIAN,
        RIFLEMAN;

        private static SurvivorRole fromSavedName(String savedName) {
            try {
                return SurvivorRole.valueOf(savedName);
            } catch (IllegalArgumentException ignored) {
                return CIVILIAN;
            }
        }
    }

    private record MosinProfile(
            ItemStack stack,
            GunItem gun,
            FireModeInstance fireMode,
            int capacity,
            int maximumShootingDistance
    ) {
    }

    public record SettlementBinding(
            UUID settlementId,
            CityZoneKey cityZone
    ) {
    }
}
