package io.github.gev414.rotwire.settlement;

import io.github.gev414.rotwire.city.CityZoneKey;
import io.github.gev414.rotwire.config.SettlementConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Durable city-level settlement state. A settlement deliberately outlives an
 * individual radio so losing a primary hub does not silently move a future
 * fast-travel destination or siege target.
 */
final class Settlement {

    private static final String ID_TAG = "id";
    private static final String CITY_ZONE_TAG = "cityZone";
    private static final String NAME_TAG = "name";
    private static final String PRIMARY_CAMP_TAG = "primaryCamp";
    private static final String PRIMARY_OWNER_TAG = "primaryOwner";
    private static final String PRIMARY_POSITION_TAG = "primaryPosition";
    private static final String CIVILIANS_TAG = "civilians";
    private static final String GUARDS_TAG = "guards";
    private static final String PISTOLMEN_TAG = "pistolmen";
    private static final String SHOTGUNNERS_TAG = "shotgunners";
    private static final String RATIONS_TAG = "rations";
    private static final String RATION_CONTAINERS_TAG = "rationContainers";
    private static final String PREPARED_RATIONS_TAG = "preparedRations";
    private static final String UPGRADES_TAG = "upgrades";
    private static final String SIEGE_STATE_TAG = "siegeState";
    private static final String NEXT_SIEGE_TAG = "nextSiegeAt";
    private static final String SIEGE_ATTENDED_TAG = "siegeAttended";
    private static final String SIEGE_VIRTUAL_DEADLINE_TAG =
            "siegeVirtualDeadline";
    private static final String PENDING_RAID_PERCENT_TAG =
            "pendingRaidPercent";
    private static final String PENDING_RAID_CAMPS_TAG = "pendingRaidCamps";
    private static final String LAST_RATION_DAY_TAG = "lastRationDay";
    private static final String RADIOS_TAG = "radios";

    private final UUID id;
    private final CityZoneKey cityZone;
    private final UUID primaryCampId;
    @Nullable
    private final UUID primaryOwner;
    private final BlockPos primaryRadioPosition;
    private final Map<UUID, SettlementRadioStatus> radios =
            new LinkedHashMap<>();

    private String name = "";
    private int civilianPopulation;
    private int guardPopulation;
    private int pistolmanPopulation;
    private int shotgunnerPopulation;
    private int rations;
    private int rationContainerCount;
    private int mosinAmmunition;
    private int pistolAmmunition;
    private int shotgunAmmunition;
    private int preparedRations;
    private int upgradeMask;
    private SettlementSiegeState siegeState = SettlementSiegeState.CALM;
    private long nextSiegeAt = -1L;
    private boolean siegeAttended;
    private long siegeVirtualDeadline = -1L;
    private int pendingRaidPercent;
    private final java.util.Set<UUID> pendingRaidCamps =
            new java.util.HashSet<>();
    private long lastRationDay = -1L;
    private long lastStockpileScan = Long.MIN_VALUE;

    private Settlement(
            UUID id,
            CityZoneKey cityZone,
            UUID primaryCampId,
            @Nullable UUID primaryOwner,
            BlockPos primaryRadioPosition
    ) {
        this.id = id;
        this.cityZone = cityZone;
        this.primaryCampId = primaryCampId;
        this.primaryOwner = primaryOwner;
        this.primaryRadioPosition = primaryRadioPosition.immutable();
    }

    static Settlement createPrimary(
            CityZoneKey cityZone,
            UUID campId,
            @Nullable UUID owner,
            BlockPos radioPosition,
            BlockPos campCenter,
            int campRadius,
            boolean campActive,
            boolean connected,
            int installedModules,
            long gameTime,
            long currentDay
    ) {
        Settlement settlement = new Settlement(
                UUID.randomUUID(),
                cityZone,
                campId,
                owner,
                radioPosition
        );
        settlement.updateRadio(
                campId,
                radioPosition,
                campCenter,
                campRadius,
                campActive,
                connected,
                installedModules,
                gameTime
        );
        settlement.lastRationDay = currentDay;
        return settlement;
    }

    CityZoneKey cityZone() {
        return cityZone;
    }

    UUID id() {
        return id;
    }

    boolean updateRadio(
            UUID campId,
            BlockPos radioPosition,
            BlockPos campCenter,
            int campRadius,
            boolean campActive,
            boolean connected,
            int installedModules,
            long gameTime
    ) {
        SettlementRadioRole role = primaryCampId.equals(campId)
                ? SettlementRadioRole.PRIMARY
                : SettlementRadioRole.RELAY;
        SettlementRadioStatus next = new SettlementRadioStatus(
                campId,
                radioPosition.immutable(),
                campCenter.immutable(),
                Math.max(0, campRadius),
                role,
                campActive,
                connected,
                false,
                installedModules,
                gameTime
        );
        SettlementRadioStatus previous = radios.put(campId, next);
        return !next.equals(previous);
    }

    boolean markRadioDestroyed(
            UUID campId,
            BlockPos radioPosition,
            long gameTime
    ) {
        SettlementRadioStatus previous = radios.get(campId);
        if (previous == null) {
            return false;
        }
        SettlementRadioStatus destroyed = new SettlementRadioStatus(
                campId,
                radioPosition.immutable(),
                previous.campCenter(),
                previous.campRadius(),
                previous.role(),
                false,
                false,
                true,
                previous.installedModules(),
                gameTime
        );
        if (destroyed.equals(previous)) {
            return false;
        }
        radios.put(campId, destroyed);
        return true;
    }

    boolean rename(
            UUID campId,
            UUID requestingPlayer,
            String proposedName
    ) {
        if (!primaryCampId.equals(campId)
                || (primaryOwner != null
                && !primaryOwner.equals(requestingPlayer))) {
            return false;
        }
        Optional<String> normalized = SettlementNameRules.normalize(
                proposedName
        );
        if (normalized.isEmpty() || name.equals(normalized.get())) {
            return false;
        }
        name = normalized.get();
        return true;
    }

    boolean setPopulation(int civilians, int guards) {
        int safeCivilians = Math.max(0, civilians);
        int safeGuards = Math.max(0, guards);
        if (civilianPopulation == safeCivilians
                && guardPopulation == safeGuards) {
            return false;
        }
        civilianPopulation = safeCivilians;
        guardPopulation = safeGuards;
        return true;
    }

    boolean addCivilian(
            int minimumRations,
            int maximumCivilians
    ) {
        if (!hasUpgrade(SettlementUpgrade.CAMP_HUB)
                || name.isEmpty()
                || rations < Math.max(0, minimumRations)
                || civilianPopulation >= Math.max(1, maximumCivilians)) {
            return false;
        }
        civilianPopulation++;
        return true;
    }

    boolean removeCivilian(UUID expectedSettlementId) {
        if (!id.equals(expectedSettlementId)
                || civilianPopulation <= 0) {
            return false;
        }
        civilianPopulation--;
        return true;
    }

    boolean addRifleman(
            ServerLevel level,
            int minimumRations,
            int minimumAmmunition,
            int initialAmmunitionItems,
            int maximumRiflemen
    ) {
        refreshStockpile(level, level.getGameTime(), true);
        int ammunitionItemsToWithdraw = Math.max(0, initialAmmunitionItems);
        if (!hasUpgrade(SettlementUpgrade.CAMP_HUB)
                || name.isEmpty()
                || rations < Math.max(0, minimumRations)
                || mosinAmmunition < Math.max(0, minimumAmmunition)
                || guardPopulation >= Math.max(1, maximumRiflemen)) {
            return false;
        }

        SettlementStockpile.AmmunitionConsumption consumption =
                SettlementStockpile.consumeMosinAmmunition(
                        level,
                        radios.values(),
                        ammunitionItemsToWithdraw
                );
        updateStockpile(consumption.remainingStockpile());
        if (consumption.roundsRemoved() != ammunitionItemsToWithdraw) {
            return false;
        }
        guardPopulation++;
        return true;
    }

    boolean removeRifleman(UUID expectedSettlementId) {
        if (!id.equals(expectedSettlementId) || guardPopulation <= 0) {
            return false;
        }
        guardPopulation--;
        return true;
    }

    boolean addPistolman(
            ServerLevel level,
            int minimumRations,
            int minimumAmmunition,
            int initialAmmunitionItems,
            int maximumPistolmen
    ) {
        return addArmedSurvivor(
                level,
                SettlementAmmunition.PISTOL_45_ACP,
                minimumRations,
                minimumAmmunition,
                initialAmmunitionItems,
                maximumPistolmen,
                GuardRole.PISTOLMAN
        );
    }

    boolean removePistolman(UUID expectedSettlementId) {
        return removeArmedSurvivor(expectedSettlementId, GuardRole.PISTOLMAN);
    }

    boolean addShotgunner(
            ServerLevel level,
            int minimumRations,
            int minimumAmmunition,
            int initialAmmunitionItems,
            int maximumShotgunners
    ) {
        return addArmedSurvivor(
                level,
                SettlementAmmunition.SHOTGUN_12_GAUGE,
                minimumRations,
                minimumAmmunition,
                initialAmmunitionItems,
                maximumShotgunners,
                GuardRole.SHOTGUNNER
        );
    }

    boolean removeShotgunner(UUID expectedSettlementId) {
        return removeArmedSurvivor(expectedSettlementId, GuardRole.SHOTGUNNER);
    }

    private boolean addArmedSurvivor(
            ServerLevel level,
            SettlementAmmunition ammunition,
            int minimumRations,
            int minimumAmmunition,
            int initialAmmunitionItems,
            int maximumPopulation,
            GuardRole role
    ) {
        refreshStockpile(level, level.getGameTime(), true);
        int ammunitionItemsToWithdraw = Math.max(0, initialAmmunitionItems);
        if (!hasUpgrade(SettlementUpgrade.CAMP_HUB)
                || name.isEmpty()
                || rations < Math.max(0, minimumRations)
                || ammunitionCount(ammunition)
                < Math.max(0, minimumAmmunition)
                || guardCount(role) >= Math.max(1, maximumPopulation)) {
            return false;
        }
        SettlementStockpile.AmmunitionConsumption consumption =
                SettlementStockpile.consumeAmmunition(
                        level,
                        radios.values(),
                        ammunition,
                        ammunitionItemsToWithdraw
                );
        updateStockpile(consumption.remainingStockpile());
        if (consumption.roundsRemoved() != ammunitionItemsToWithdraw) {
            return false;
        }
        incrementGuard(role);
        return true;
    }

    private boolean removeArmedSurvivor(
            UUID expectedSettlementId,
            GuardRole role
    ) {
        if (!id.equals(expectedSettlementId) || guardCount(role) <= 0) {
            return false;
        }
        decrementGuard(role);
        return true;
    }

    int withdrawMosinAmmunition(ServerLevel level, int requestedRounds) {
        return withdrawAmmunition(
                level,
                SettlementAmmunition.MOSIN_762X51,
                requestedRounds
        );
    }

    int withdrawAmmunition(
            ServerLevel level,
            SettlementAmmunition ammunition,
            int requestedRounds
    ) {
        SettlementStockpile.AmmunitionConsumption consumption =
                SettlementStockpile.consumeAmmunition(
                        level,
                        radios.values(),
                        ammunition,
                        requestedRounds
                );
        updateStockpile(consumption.remainingStockpile());
        return consumption.roundsRemoved();
    }

    int beginRationDay(
            long currentDay,
            int rationsPerSettler
    ) {
        if (currentDay <= lastRationDay) {
            return -1;
        }
        lastRationDay = currentDay;
        if (!hasUpgrade(SettlementUpgrade.CAMP_HUB)) {
            return 0;
        }
        return SettlementRationRules.dailyCost(
                snapshot().population(),
                rationsPerSettler
        );
    }

    boolean updateStockpile(
            SettlementStockpile.StockpileSnapshot stockpile
    ) {
        int nextRations = saturatedAdd(
                stockpile.rations(),
                preparedRations
        );
        int nextContainers = Math.max(0, stockpile.containerCount());
        int nextMosinAmmunition = Math.max(0, stockpile.mosinRounds());
        int nextPistolAmmunition = Math.max(0, stockpile.pistolRounds());
        int nextShotgunAmmunition = Math.max(0, stockpile.shotgunShells());
        if (rations == nextRations
                && rationContainerCount == nextContainers
                && mosinAmmunition == nextMosinAmmunition
                && pistolAmmunition == nextPistolAmmunition
                && shotgunAmmunition == nextShotgunAmmunition) {
            return false;
        }
        rations = nextRations;
        rationContainerCount = nextContainers;
        mosinAmmunition = nextMosinAmmunition;
        pistolAmmunition = nextPistolAmmunition;
        shotgunAmmunition = nextShotgunAmmunition;
        return true;
    }

    boolean refreshStockpile(
            ServerLevel level,
            long gameTime,
            boolean force
    ) {
        if (!hasUpgrade(SettlementUpgrade.CAMP_HUB)
                || (!force && lastStockpileScan != Long.MIN_VALUE
                && gameTime - lastStockpileScan
                < SettlementConfig.STOCKPILE_SCAN_INTERVAL_TICKS.get())) {
            return false;
        }
        lastStockpileScan = gameTime;
        return updateStockpile(SettlementStockpile.inspect(
                level,
                radios.values()
        ));
    }

    SettlementStockpile.StockpileConsumption consumePhysicalStockpile(
            ServerLevel level,
            int requestedRations
    ) {
        return SettlementStockpile.consume(
                level,
                radios.values(),
                requestedRations
        );
    }

    int consumePreparedRations(int requestedRations) {
        int consumed = Math.min(
                Math.max(0, requestedRations),
                preparedRations
        );
        if (consumed > 0) {
            preparedRations -= consumed;
            rations = Math.max(0, rations - consumed);
        }
        return consumed;
    }

    boolean applyPhysicalConsumption(
            SettlementStockpile.StockpileConsumption consumption,
            int requiredRations
    ) {
        int surplus = Math.max(
                0,
                consumption.nutritionRemoved()
                        - Math.max(0, requiredRations)
        );
        preparedRations = saturatedAdd(preparedRations, surplus);
        return updateStockpile(consumption.remainingStockpile());
    }

    boolean setUpgrade(SettlementUpgrade upgrade, boolean installed) {
        int next = installed
                ? upgradeMask | upgrade.mask()
                : upgradeMask & ~upgrade.mask();
        if (next == upgradeMask) {
            return false;
        }
        upgradeMask = next;
        return true;
    }

    boolean hasUpgrade(SettlementUpgrade upgrade) {
        return (upgradeMask & upgrade.mask()) != 0;
    }

    boolean setSiegeSchedule(
            SettlementSiegeState proposedState,
            long proposedNextSiegeAt
    ) {
        if (siegeState == proposedState
                && nextSiegeAt == proposedNextSiegeAt) {
            return false;
        }
        siegeState = proposedState;
        nextSiegeAt = proposedNextSiegeAt;
        return true;
    }

    boolean beginSiege(long activeEndsAt, long virtualDeadlineAt) {
        if (siegeState == SettlementSiegeState.ACTIVE
                && nextSiegeAt == activeEndsAt
                && siegeVirtualDeadline == virtualDeadlineAt
                && !siegeAttended) {
            return false;
        }
        siegeState = SettlementSiegeState.ACTIVE;
        nextSiegeAt = activeEndsAt;
        siegeVirtualDeadline = virtualDeadlineAt;
        siegeAttended = false;
        return true;
    }

    boolean markSiegeAttended() {
        if (siegeState != SettlementSiegeState.ACTIVE || siegeAttended) {
            return false;
        }
        siegeAttended = true;
        return true;
    }

    boolean isUnattendedSiegeDue(long gameTime) {
        return siegeState == SettlementSiegeState.ACTIVE
                && !siegeAttended
                && siegeVirtualDeadline >= 0L
                && gameTime >= siegeVirtualDeadline;
    }

    boolean resolveVirtualSiege(int raidPercent, long recoveryEndsAt) {
        int safePercent = Math.clamp(raidPercent, 0, 100);
        boolean changed = siegeState != SettlementSiegeState.RECOVERY
                || nextSiegeAt != recoveryEndsAt
                || siegeVirtualDeadline != -1L;
        siegeState = SettlementSiegeState.RECOVERY;
        nextSiegeAt = recoveryEndsAt;
        siegeVirtualDeadline = -1L;
        siegeAttended = false;
        if (safePercent > 0) {
            pendingRaidPercent = Math.max(pendingRaidPercent, safePercent);
            for (SettlementRadioStatus radio : radios.values()) {
                if (radio.contributesStockpile()) {
                    pendingRaidCamps.add(radio.campId());
                }
            }
            changed = true;
        }
        return changed;
    }

    boolean applyPendingRaids(ServerLevel level) {
        if (pendingRaidPercent <= 0 || pendingRaidCamps.isEmpty()) {
            return false;
        }
        boolean changed = false;
        for (UUID campId : java.util.Set.copyOf(pendingRaidCamps)) {
            SettlementRadioStatus radio = radios.get(campId);
            if (radio == null || !radio.contributesStockpile()) {
                pendingRaidCamps.remove(campId);
                changed = true;
                continue;
            }
            if (!level.isAreaLoaded(radio.campCenter(), radio.campRadius())) {
                continue;
            }
            SettlementStockpile.destroyPercentage(
                    level,
                    java.util.List.of(radio),
                    pendingRaidPercent
            );
            pendingRaidCamps.remove(campId);
            changed = true;
        }
        if (pendingRaidCamps.isEmpty()) {
            pendingRaidPercent = 0;
        }
        if (changed) {
            updateStockpile(SettlementStockpile.inspect(level, radios.values()));
        }
        return changed;
    }

    boolean canRunSieges(int minimumPopulation) {
        SettlementRadioStatus primary = radios.get(primaryCampId);
        return hasUpgrade(SettlementUpgrade.CAMP_HUB)
                && !name.isEmpty()
                && snapshot().population() >= Math.max(1, minimumPopulation)
                && primary != null
                && primary.contributesStockpile()
                && primary.connected();
    }

    Optional<SettlementRadioStatus> primaryRadio() {
        return Optional.ofNullable(radios.get(primaryCampId))
                .filter(radio -> !radio.destroyed());
    }

    int raidRations(ServerLevel level, int requestedRations) {
        int requested = Math.max(0, requestedRations);
        if (requested == 0) {
            return 0;
        }
        int prepared = consumePreparedRations(requested);
        int remaining = requested - prepared;
        if (remaining <= 0) {
            return prepared;
        }
        SettlementStockpile.StockpileConsumption consumption =
                consumePhysicalStockpile(level, remaining);
        updateStockpile(consumption.remainingStockpile());
        return saturatedAdd(prepared, consumption.nutritionRemoved());
    }

    SettlementSnapshot snapshot() {
        int online = 0;
        int active = 0;
        boolean primaryDestroyed = false;
        for (SettlementRadioStatus radio : radios.values()) {
            if (radio.connected() && !radio.destroyed()) {
                online++;
            }
            if (radio.campActive() && !radio.destroyed()) {
                active++;
            }
            if (radio.role() == SettlementRadioRole.PRIMARY
                    && radio.destroyed()) {
                primaryDestroyed = true;
            }
        }
        return new SettlementSnapshot(
                id,
                cityZone,
                name,
                primaryRadioPosition,
                primaryCampId,
                civilianPopulation,
                guardPopulation,
                pistolmanPopulation,
                shotgunnerPopulation,
                rations,
                rationContainerCount,
                mosinAmmunition,
                pistolAmmunition,
                shotgunAmmunition,
                upgradeMask,
                siegeState,
                nextSiegeAt,
                radios.size(),
                online,
                active,
                primaryDestroyed
        );
    }

    CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID(ID_TAG, id);
        tag.put(CITY_ZONE_TAG, cityZone.save());
        tag.putString(NAME_TAG, name);
        tag.putUUID(PRIMARY_CAMP_TAG, primaryCampId);
        if (primaryOwner != null) {
            tag.putUUID(PRIMARY_OWNER_TAG, primaryOwner);
        }
        tag.putLong(PRIMARY_POSITION_TAG, primaryRadioPosition.asLong());
        tag.putInt(CIVILIANS_TAG, civilianPopulation);
        tag.putInt(GUARDS_TAG, guardPopulation);
        tag.putInt(PISTOLMEN_TAG, pistolmanPopulation);
        tag.putInt(SHOTGUNNERS_TAG, shotgunnerPopulation);
        tag.putInt(RATIONS_TAG, rations);
        tag.putInt(RATION_CONTAINERS_TAG, rationContainerCount);
        tag.putInt(PREPARED_RATIONS_TAG, preparedRations);
        tag.putInt(UPGRADES_TAG, upgradeMask);
        tag.putString(SIEGE_STATE_TAG, siegeState.name());
        tag.putLong(NEXT_SIEGE_TAG, nextSiegeAt);
        tag.putBoolean(SIEGE_ATTENDED_TAG, siegeAttended);
        tag.putLong(SIEGE_VIRTUAL_DEADLINE_TAG, siegeVirtualDeadline);
        tag.putInt(PENDING_RAID_PERCENT_TAG, pendingRaidPercent);
        ListTag savedPendingRaidCamps = new ListTag();
        for (UUID campId : pendingRaidCamps) {
            CompoundTag savedCamp = new CompoundTag();
            savedCamp.putUUID("campId", campId);
            savedPendingRaidCamps.add(savedCamp);
        }
        tag.put(PENDING_RAID_CAMPS_TAG, savedPendingRaidCamps);
        tag.putLong(LAST_RATION_DAY_TAG, lastRationDay);

        ListTag savedRadios = new ListTag();
        for (SettlementRadioStatus radio : radios.values()) {
            savedRadios.add(radio.save());
        }
        tag.put(RADIOS_TAG, savedRadios);
        return tag;
    }

    static Settlement load(CompoundTag tag) {
        Settlement settlement = new Settlement(
                tag.getUUID(ID_TAG),
                CityZoneKey.load(tag.getCompound(CITY_ZONE_TAG)),
                tag.getUUID(PRIMARY_CAMP_TAG),
                tag.hasUUID(PRIMARY_OWNER_TAG)
                        ? tag.getUUID(PRIMARY_OWNER_TAG)
                        : null,
                BlockPos.of(tag.getLong(PRIMARY_POSITION_TAG))
        );
        settlement.name = SettlementNameRules.normalize(
                tag.getString(NAME_TAG)
        ).orElse("");
        settlement.civilianPopulation = Math.max(
                0,
                tag.getInt(CIVILIANS_TAG)
        );
        settlement.guardPopulation = Math.max(0, tag.getInt(GUARDS_TAG));
        settlement.pistolmanPopulation = Math.max(
                0,
                tag.getInt(PISTOLMEN_TAG)
        );
        settlement.shotgunnerPopulation = Math.max(
                0,
                tag.getInt(SHOTGUNNERS_TAG)
        );
        settlement.rations = Math.max(0, tag.getInt(RATIONS_TAG));
        settlement.rationContainerCount = Math.max(
                0,
                tag.getInt(RATION_CONTAINERS_TAG)
        );
        settlement.preparedRations = Math.max(
                0,
                tag.getInt(PREPARED_RATIONS_TAG)
        );
        settlement.upgradeMask = SettlementUpgrade.sanitizeMask(
                tag.getInt(UPGRADES_TAG)
        );
        settlement.siegeState = parseSiegeState(
                tag.getString(SIEGE_STATE_TAG)
        );
        settlement.nextSiegeAt = tag.getLong(NEXT_SIEGE_TAG);
        settlement.siegeAttended = tag.getBoolean(SIEGE_ATTENDED_TAG);
        settlement.siegeVirtualDeadline = tag.contains(
                SIEGE_VIRTUAL_DEADLINE_TAG
        ) ? tag.getLong(SIEGE_VIRTUAL_DEADLINE_TAG) : -1L;
        settlement.pendingRaidPercent = Math.clamp(
                tag.getInt(PENDING_RAID_PERCENT_TAG),
                0,
                100
        );
        ListTag savedPendingRaidCamps = tag.getList(
                PENDING_RAID_CAMPS_TAG,
                Tag.TAG_COMPOUND
        );
        for (int index = 0; index < savedPendingRaidCamps.size(); index++) {
            CompoundTag savedCamp = savedPendingRaidCamps.getCompound(index);
            if (savedCamp.hasUUID("campId")) {
                settlement.pendingRaidCamps.add(savedCamp.getUUID("campId"));
            }
        }
        settlement.lastRationDay = tag.contains(LAST_RATION_DAY_TAG)
                ? tag.getLong(LAST_RATION_DAY_TAG)
                : -1L;

        ListTag savedRadios = tag.getList(RADIOS_TAG, Tag.TAG_COMPOUND);
        for (int index = 0; index < savedRadios.size(); index++) {
            SettlementRadioStatus radio = SettlementRadioStatus.load(
                    savedRadios.getCompound(index)
            );
            settlement.radios.put(radio.campId(), radio);
        }
        return settlement;
    }

    private static SettlementSiegeState parseSiegeState(String serialized) {
        try {
            return SettlementSiegeState.valueOf(serialized);
        } catch (IllegalArgumentException exception) {
            return SettlementSiegeState.CALM;
        }
    }

    private static int saturatedAdd(int left, int right) {
        return (int) Math.min(
                Integer.MAX_VALUE,
                (long) Math.max(0, left) + Math.max(0, right)
        );
    }

    private int ammunitionCount(SettlementAmmunition ammunition) {
        return switch (ammunition) {
            case MOSIN_762X51 -> mosinAmmunition;
            case PISTOL_45_ACP -> pistolAmmunition;
            case SHOTGUN_12_GAUGE -> shotgunAmmunition;
        };
    }

    private int guardCount(GuardRole role) {
        return switch (role) {
            case PISTOLMAN -> pistolmanPopulation;
            case SHOTGUNNER -> shotgunnerPopulation;
        };
    }

    private void incrementGuard(GuardRole role) {
        switch (role) {
            case PISTOLMAN -> pistolmanPopulation++;
            case SHOTGUNNER -> shotgunnerPopulation++;
        }
    }

    private void decrementGuard(GuardRole role) {
        switch (role) {
            case PISTOLMAN -> pistolmanPopulation--;
            case SHOTGUNNER -> shotgunnerPopulation--;
        }
    }

    private enum GuardRole {
        PISTOLMAN,
        SHOTGUNNER
    }
}
