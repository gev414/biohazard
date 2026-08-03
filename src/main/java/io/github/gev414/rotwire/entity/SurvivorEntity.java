package io.github.gev414.rotwire.entity;

import io.github.gev414.rotwire.city.CityZoneKey;
import io.github.gev414.rotwire.config.SettlementConfig;
import io.github.gev414.rotwire.entity.ai.SurvivorCityRoamGoal;
import io.github.gev414.rotwire.entity.ai.SurvivorMosinAttackGoal;
import io.github.gev414.rotwire.entity.ai.SurvivorReturnToCampGoal;
import io.github.gev414.rotwire.entity.ai.SurvivorThreatAwarenessGoal;
import io.github.gev414.rotwire.settlement.SettlementManager;
import io.github.gev414.rotwire.settlement.SettlementAmmunition;
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
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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
    private static final String FIREARM_ROUNDS_TAG = "firearmRounds";
    private static final String FIREARM_RESERVE_SHOTS_TAG = "firearmReserveShots";
    private static final String RETURN_HOME_ORDERED_TAG = "returnHomeOrdered";

    /** Each physical camp-stockpile round provides this many survivor shots. */
    private static final int SHOTS_PER_STORED_AMMUNITION = 10;

    private static final EntityDataAccessor<Integer> ROLE_DATA =
            SynchedEntityData.defineId(
                    SurvivorEntity.class,
                    EntityDataSerializers.INT
            );

    private static final ResourceLocation MOSIN_ID =
            ResourceLocation.fromNamespaceAndPath("pointblank", "mosin");
    private static final ResourceLocation M1911_ID =
            ResourceLocation.fromNamespaceAndPath("pointblank", "m1911a1");
    private static final ResourceLocation M870_ID =
            ResourceLocation.fromNamespaceAndPath("pointblank", "m870");

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
    private int firearmRounds;
    private int firearmReserveShots;
    @Nullable
    private FirearmProfile firearmProfileCache;
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
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ROLE_DATA, SurvivorRole.CIVILIAN.ordinal());
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
                monster -> {
                    int range = firearmMaximumShootingDistance();
                    return canFightWithFirearm()
                            && range > 0
                            && distanceToSqr(monster)
                            <= (double) range * range;
                }
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

    public boolean equipMosinRifle(int loadedRounds) {
        return equipFirearm(SurvivorRole.RIFLEMAN, loadedRounds);
    }

    public boolean equipPistol(int loadedRounds) {
        return equipFirearm(SurvivorRole.PISTOLMAN, loadedRounds);
    }

    public boolean equipShotgun(int loadedRounds) {
        return equipFirearm(SurvivorRole.SHOTGUNNER, loadedRounds);
    }

    public static int mosinMagazineCapacity() {
        return magazineCapacity(SurvivorRole.RIFLEMAN);
    }

    public static int pistolMagazineCapacity() {
        return magazineCapacity(SurvivorRole.PISTOLMAN);
    }

    public static int shotgunMagazineCapacity() {
        return magazineCapacity(SurvivorRole.SHOTGUNNER);
    }

    /**
     * Returns the number of physical stockpile items required to provide the
     * requested number of survivor combat shots.
     */
    public static int storedAmmunitionItemsForShots(int combatShots) {
        int shots = Math.max(0, combatShots);
        return shots == 0
                ? 0
                : ((shots - 1) / SHOTS_PER_STORED_AMMUNITION) + 1;
    }

    public boolean isRifleman() {
        return role() == SurvivorRole.RIFLEMAN;
    }

    public boolean isPistolman() {
        return role() == SurvivorRole.PISTOLMAN;
    }

    public boolean isShotgunner() {
        return role() == SurvivorRole.SHOTGUNNER;
    }

    public boolean hasFirearm() {
        return role().isArmed();
    }

    public @Nullable ResourceLocation firearmItemId() {
        SurvivorRole currentRole = role();
        return currentRole.isArmed() ? currentRole.gunId() : null;
    }

    public boolean canFightWithMosin() {
        return canFightWithFirearm();
    }

    public boolean canFightWithFirearm() {
        return role().isArmed()
                && firearmProfile().isPresent()
                && availableAmmunition() > 0
                && !mustActivelyRetreat();
    }

    public int mosinMaximumShootingDistance() {
        return firearmMaximumShootingDistance();
    }

    public int firearmMaximumShootingDistance() {
        return firearmProfile()
                .map(FirearmProfile::maximumShootingDistance)
                .orElse(0);
    }

    public double firearmMinimumEngagementDistance() {
        double configured = switch (role()) {
            case RIFLEMAN -> SettlementConfig
                    .RIFLEMAN_MINIMUM_ENGAGEMENT_DISTANCE.get();
            case PISTOLMAN -> SettlementConfig
                    .PISTOLMAN_MINIMUM_ENGAGEMENT_DISTANCE.get();
            case SHOTGUNNER -> SettlementConfig
                    .SHOTGUNNER_MINIMUM_ENGAGEMENT_DISTANCE.get();
            case CIVILIAN -> 0.0D;
        };
        int maximum = firearmMaximumShootingDistance();
        return maximum <= 1
                ? 0.0D
                : Math.min(configured, maximum - 1.0D);
    }

    public int mosinMagazineRounds() {
        return firearmMagazineRounds();
    }

    public int firearmMagazineRounds() {
        return firearmProfile().map(profile -> Math.max(
                0,
                Math.min(firearmRounds, profile.capacity())
        )).orElse(0);
    }

    /**
     * Refills the PointBlank magazine from physical 7.62x51 stacks in this
     * survivor's settlement, returning true when at least one round loaded.
     */
    public boolean reloadMosinFromSettlement() {
        return reloadFirearmFromSettlement();
    }

    public boolean reloadFirearmFromSettlement() {
        if (!(level() instanceof ServerLevel serverLevel)
                || cityZone == null
                || settlementId == null) {
            return false;
        }
        FirearmProfile profile = firearmProfile().orElse(null);
        if (profile == null) {
            return false;
        }
        int currentRounds = firearmMagazineRounds();
        int requiredRounds = profile.capacity() - currentRounds;
        if (requiredRounds <= 0) {
            return false;
        }
        int reserveRoundsLoaded = Math.min(requiredRounds, firearmReserveShots);
        int roundsStillRequired = requiredRounds - reserveRoundsLoaded;
        int requestedAmmunitionItems = storedAmmunitionItemsForShots(
                roundsStillRequired
        );
        int withdrawn = SettlementManager.withdrawAmmunition(
                serverLevel,
                cityZone,
                role().ammunition(),
                requestedAmmunitionItems
        );
        int suppliedShots = saturatedMultiply(
                withdrawn,
                SHOTS_PER_STORED_AMMUNITION
        );
        int roundsLoadedFromStockpile = Math.min(
                roundsStillRequired,
                suppliedShots
        );
        if (reserveRoundsLoaded + roundsLoadedFromStockpile <= 0) {
            return false;
        }
        firearmRounds = currentRounds + reserveRoundsLoaded
                + roundsLoadedFromStockpile;
        firearmReserveShots = firearmReserveShots - reserveRoundsLoaded
                + suppliedShots - roundsLoadedFromStockpile;
        return true;
    }

    /**
     * Fires one custom hitscan shot using the live Mosin fire-mode data. This
     * preserves the weapon's damage, fire sound, magazine, RPM, and PointBlank
     * headshot multiplier without relying on a fake player firing packet.
     */
    public boolean fireMosinAt(LivingEntity target) {
        return fireFirearmAt(target);
    }

    public boolean fireFirearmAt(LivingEntity target) {
        FirearmProfile profile = firearmProfile().orElse(null);
        if (profile == null || target == null || !target.isAlive()
                || isAlliedTo(target) || mustActivelyRetreat()) {
            return false;
        }
        int currentRounds = firearmMagazineRounds();
        if (currentRounds <= 0 || !getSensing().hasLineOfSight(target)) {
            return false;
        }

        Vec3 origin = getEyePosition();
        double distance = origin.distanceTo(target.getEyePosition());
        if (distance > profile.maximumShootingDistance()) {
            return false;
        }
        boolean headshot = isRifleman() && random.nextDouble()
                < SettlementConfig.RIFLEMAN_HEADSHOT_CHANCE.get();
        Vec3 aimPoint = headshot
                ? target.getEyePosition()
                : target.getBoundingBox().getCenter();
        Vec3 direction = aimPoint.subtract(origin).normalize();
        if (isRifleman()
                && distance > SettlementConfig.RIFLEMAN_PRECISE_RANGE.get()
                && random.nextDouble()
                < SettlementConfig.RIFLEMAN_LONG_RANGE_MISS_CHANCE.get()) {
            direction = intentionalMissDirection(direction);
        }

        firearmRounds = currentRounds - 1;
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
        int pellets = isShotgunner()
                ? Math.max(1, profile.gun().getPelletCount())
                : 1;
        for (int pellet = 0; pellet < pellets; pellet++) {
            Vec3 pelletDirection = pellets == 1
                    ? direction
                    : spreadDirection(
                            direction,
                            profile.gun().getPelletSpread()
                    );
            damageFirstHit(
                    origin,
                    pelletDirection,
                    profile.maximumShootingDistance(),
                    profile.fireMode().getDamage(),
                    headshot ? profile.fireMode().getHeadshotMultiplier() : 1.0F
            );
        }
        return true;
    }

    public double mosinShotCooldownTicks() {
        return firearmShotCooldownTicks();
    }

    public double firearmShotCooldownTicks() {
        return firearmProfile().map(profile -> Math.max(
                role().minimumShotIntervalTicks(),
                1_200.0D / Math.max(1, profile.fireMode().getRpm())
        )).orElse(20.0D);
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
     * Keeps tactical movement inside the assigned camp. A survivor already
     * outside that boundary may still choose a position that moves it closer
     * to home, preventing combat movement from trapping it beyond the camp.
     */
    public boolean isTacticalPositionAllowed(BlockPos candidate) {
        if (homePosition == null) {
            return true;
        }
        double candidateDistance = homePosition.distSqr(candidate);
        return candidateDistance <= (double) homeRadius * homeRadius
                || candidateDistance < homePosition.distSqr(blockPosition());
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
        if (!isWithinCityRoamRadius()) {
            return true;
        }

        boolean ranged = hasRangedLoadout();
        int ammunition = availableAmmunition();
        if (settlement.siegeState() == SettlementSiegeState.ACTIVE) {
            return SurvivorSafetyRules.mustReturnToCamp(
                    new SurvivorSafetyRules.SafetyContext(
                            settlement.siegeState(),
                            ranged,
                            ammunition,
                            false,
                            0,
                            0
                    )
            );
        }
        if (!role().isArmed()) {
            return nearbyHostileCount() > 0;
        }

        boolean meleeFight = isInMeleeFight();
        if (!meleeFight) {
            return ranged && ammunition <= 0;
        }
        int hostileCount = nearbyHostileCount();
        return SurvivorSafetyRules.mustReturnToCamp(
                new SurvivorSafetyRules.SafetyContext(
                        settlement.siegeState(),
                        ranged,
                        ammunition,
                        true,
                        hostileCount,
                        nearbyMeleeAllyCount()
                )
        );
    }

    /**
     * Safety conditions send survivors home, but once they have reached the
     * shelter an armed survivor must defend itself instead of remaining in a
     * permanent retreat state while an infected attacks at point-blank range.
     */
    private boolean mustActivelyRetreat() {
        return shouldReturnToCamp() && !isAtCampRetreatPoint();
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
        tag.putString(ROLE_TAG, role().name());
        tag.putInt(FIREARM_ROUNDS_TAG, firearmMagazineRounds());
        tag.putInt(FIREARM_RESERVE_SHOTS_TAG, firearmReserveShots);
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
        SurvivorRole savedRole = SurvivorRole.fromSavedName(
                tag.getString(ROLE_TAG)
        );
        setRole(savedRole);
        if (tag.contains(FIREARM_ROUNDS_TAG, Tag.TAG_INT)) {
            firearmRounds = Math.max(0, tag.getInt(FIREARM_ROUNDS_TAG));
        } else {
            // Migrate survivors saved before firearm magazines were separated
            // from PointBlank's player-only equipped-item state.
            ItemStack oldFirearm = getMainHandItem();
            firearmRounds = oldFirearm.getItem() instanceof GunItem
                    ? Math.max(0, GunItem.getAmmo(
                            oldFirearm,
                            GunItem.getFireModeInstance(oldFirearm)
                    ))
                    : 0;
        }
        firearmReserveShots = Math.max(
                0,
                Math.min(
                        SHOTS_PER_STORED_AMMUNITION - 1,
                        tag.getInt(FIREARM_RESERVE_SHOTS_TAG)
                )
        );
        returnHomeOrdered = tag.getBoolean(RETURN_HOME_ORDERED_TAG);
        if (savedRole.isArmed()) {
            firearmRounds = Math.min(
                    firearmRounds,
                    magazineCapacity(savedRole)
            );
            setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
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
        return role().isArmed() && firearmProfile().isPresent();
    }

    private int availableAmmunition() {
        FirearmProfile profile = firearmProfile().orElse(null);
        if (profile == null) {
            return 0;
        }
        int magazine = firearmMagazineRounds();
        if (!(level() instanceof ServerLevel serverLevel)) {
            return magazine;
        }
        int settlementRounds = settlement(serverLevel)
                .map(snapshot -> switch (role().ammunition()) {
                    case MOSIN_762X51 -> snapshot.mosinAmmunition();
                    case PISTOL_45_ACP -> snapshot.pistolAmmunition();
                    case SHOTGUN_12_GAUGE -> snapshot.shotgunAmmunition();
                })
                .orElse(0);
        return (int) Math.min(
                Integer.MAX_VALUE,
                (long) magazine + firearmReserveShots
                        + (long) settlementRounds
                        * SHOTS_PER_STORED_AMMUNITION
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

    private boolean equipFirearm(SurvivorRole nextRole, int loadedRounds) {
        ItemStack firearm = createFirearmStack(nextRole);
        if (firearm.isEmpty() || !(firearm.getItem() instanceof GunItem gun)) {
            return false;
        }
        FireModeInstance fireMode = GunItem.getFireModeInstance(firearm);
        int capacity = Math.max(0, gun.getMaxAmmoCapacity(firearm, fireMode));
        if (capacity <= 0) {
            return false;
        }
        setRole(nextRole);
        firearmRounds = Math.min(Math.max(0, loadedRounds), capacity);
        firearmReserveShots = saturatedMultiply(
                storedAmmunitionItemsForShots(firearmRounds),
                SHOTS_PER_STORED_AMMUNITION
        ) - firearmRounds;
        // Never equip the PointBlank stack itself. GunItem's inventory tick
        // assumes its holder is a player and otherwise syncs inventory slot
        // -1, producing a packet/error storm. The client survivor layer draws
        // an equivalent display-only stack from the synchronized role.
        setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        setCustomName(Component.translatable(nextRole.translationKey()));
        return true;
    }

    private static int magazineCapacity(SurvivorRole firearmRole) {
        ItemStack firearm = createFirearmStack(firearmRole);
        if (firearm.isEmpty() || !(firearm.getItem() instanceof GunItem gun)) {
            return 0;
        }
        return Math.max(
                0,
                gun.getMaxAmmoCapacity(
                        firearm,
                        GunItem.getFireModeInstance(firearm)
                )
        );
    }

    private static int saturatedMultiply(int left, int right) {
        return (int) Math.min(
                Integer.MAX_VALUE,
                (long) Math.max(0, left) * Math.max(0, right)
        );
    }

    private Optional<FirearmProfile> firearmProfile() {
        SurvivorRole currentRole = role();
        if (!currentRole.isArmed()) {
            return Optional.empty();
        }
        if (firearmProfileCache != null) {
            return Optional.of(firearmProfileCache);
        }
        ItemStack stack = createFirearmStack(currentRole);
        if (!(stack.getItem() instanceof GunItem gun)) {
            return Optional.empty();
        }
        FireModeInstance fireMode = GunItem.getFireModeInstance(stack);
        int capacity = gun.getMaxAmmoCapacity(stack, fireMode);
        if (capacity <= 0 || fireMode.getRpm() <= 0) {
            return Optional.empty();
        }
        firearmProfileCache = new FirearmProfile(
                gun,
                fireMode,
                capacity,
                currentRole.maximumRange()
        );
        return Optional.of(firearmProfileCache);
    }

    private SurvivorRole role() {
        return SurvivorRole.fromOrdinal(entityData.get(ROLE_DATA));
    }

    private void setRole(SurvivorRole nextRole) {
        entityData.set(ROLE_DATA, nextRole.ordinal());
        firearmProfileCache = null;
    }

    private static ItemStack createFirearmStack(SurvivorRole firearmRole) {
        if (!firearmRole.isArmed()) {
            return ItemStack.EMPTY;
        }
        Item item = BuiltInRegistries.ITEM.get(firearmRole.gunId());
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

    private Vec3 spreadDirection(Vec3 direction, double spread) {
        Vec3 side = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (side.lengthSqr() < 1.0E-6D) {
            side = direction.cross(new Vec3(1.0D, 0.0D, 0.0D));
        }
        Vec3 up = side.cross(direction).normalize();
        double spreadAmount = Math.min(0.5D, Math.max(0.0D, spread));
        return direction.add(side.normalize().scale(
                (random.nextDouble() - 0.5D) * spreadAmount
        )).add(up.scale(
                (random.nextDouble() - 0.5D) * spreadAmount
        )).normalize();
    }

    private void damageFirstHit(
            Vec3 origin,
            Vec3 direction,
            int maximumDistance,
            float damage,
            float multiplier
    ) {
        Vec3 maximumEnd = origin.add(direction.scale(maximumDistance));
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
                        direction.scale(maximumDistance)
                ).inflate(1.0D),
                entity -> entity instanceof LivingEntity living
                        && living.isAlive()
                        && !(entity instanceof Player)
                        && !isAlliedTo(entity)
        );
        if (entityHit != null
                && entityHit.getEntity() instanceof LivingEntity living) {
            living.hurt(
                    level().damageSources().mobAttack(this),
                    damage * multiplier
            );
        }
    }

    private enum SurvivorRole {
        CIVILIAN(null, null, 0, 0.0D, "entity.rotwire.survivor"),
        RIFLEMAN(
                MOSIN_ID,
                SettlementAmmunition.MOSIN_762X51,
                64,
                0.0D,
                "entity.rotwire.rifleman"
        ),
        PISTOLMAN(
                M1911_ID,
                SettlementAmmunition.PISTOL_45_ACP,
                24,
                8.0D,
                "entity.rotwire.pistolman"
        ),
        SHOTGUNNER(
                M870_ID,
                SettlementAmmunition.SHOTGUN_12_GAUGE,
                18,
                20.0D,
                "entity.rotwire.shotgunner"
        );

        @Nullable
        private final ResourceLocation gunId;
        @Nullable
        private final SettlementAmmunition ammunition;
        private final int maximumRange;
        private final double minimumShotIntervalTicks;
        private final String translationKey;

        SurvivorRole(
                @Nullable ResourceLocation gunId,
                @Nullable SettlementAmmunition ammunition,
                int maximumRange,
                double minimumShotIntervalTicks,
                String translationKey
        ) {
            this.gunId = gunId;
            this.ammunition = ammunition;
            this.maximumRange = maximumRange;
            this.minimumShotIntervalTicks = minimumShotIntervalTicks;
            this.translationKey = translationKey;
        }

        private boolean isArmed() {
            return gunId != null && ammunition != null;
        }

        private ResourceLocation gunId() {
            if (gunId == null) {
                throw new IllegalStateException("Civilian has no firearm");
            }
            return gunId;
        }

        private SettlementAmmunition ammunition() {
            if (ammunition == null) {
                throw new IllegalStateException("Civilian has no ammunition");
            }
            return ammunition;
        }

        private int maximumRange() {
            return maximumRange;
        }

        private double minimumShotIntervalTicks() {
            return minimumShotIntervalTicks;
        }

        private String translationKey() {
            return translationKey;
        }

        private static SurvivorRole fromSavedName(String savedName) {
            try {
                return SurvivorRole.valueOf(savedName);
            } catch (IllegalArgumentException ignored) {
                return CIVILIAN;
            }
        }

        private static SurvivorRole fromOrdinal(int ordinal) {
            SurvivorRole[] roles = values();
            return ordinal >= 0 && ordinal < roles.length
                    ? roles[ordinal]
                    : CIVILIAN;
        }
    }

    private record FirearmProfile(
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
