package io.github.gev414.rotwire.settlement;

import io.github.gev414.rotwire.city.CityZoneKey;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Server-wide city settlement state. The Overworld store is intentional: a
 * {@link CityZoneKey} already includes the dimension and settlement state must
 * remain available while a city dimension is unloaded.
 */
public final class SettlementSavedData extends SavedData {

    private static final String FILE_NAME = "rotwire_settlements";
    private static final int FORMAT_VERSION = 2;
    private static final Factory<SettlementSavedData> FACTORY = new Factory<>(
            SettlementSavedData::new,
            SettlementSavedData::load,
            DataFixTypes.LEVEL
    );

    private final Map<CityZoneKey, Settlement> settlements =
            new LinkedHashMap<>();

    public static SettlementSavedData get(MinecraftServer server) {
        return server.overworld()
                .getDataStorage()
                .computeIfAbsent(FACTORY, FILE_NAME);
    }

    public Optional<SettlementSnapshot> syncRadio(
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
        Settlement settlement = settlements.get(cityZone);
        if (settlement == null) {
            if (!campActive) {
                return Optional.empty();
            }
            settlement = Settlement.createPrimary(
                    cityZone,
                    campId,
                    owner,
                    radioPosition,
                    campCenter,
                    campRadius,
                    campActive,
                    connected,
                    installedModules,
                    gameTime,
                    currentDay
            );
            settlements.put(cityZone, settlement);
            setDirty();
            return Optional.of(settlement.snapshot());
        }

        if (settlement.updateRadio(
                campId,
                radioPosition,
                campCenter,
                campRadius,
                campActive,
                connected,
                installedModules,
                gameTime
        )) {
            setDirty();
        }
        return Optional.of(settlement.snapshot());
    }

    public Optional<SettlementSnapshot> status(CityZoneKey cityZone) {
        return Optional.ofNullable(settlements.get(cityZone))
                .map(Settlement::snapshot);
    }

    public boolean renamePrimary(
            CityZoneKey cityZone,
            UUID campId,
            UUID playerId,
            String proposedName
    ) {
        Settlement settlement = settlements.get(cityZone);
        if (settlement == null
                || !settlement.rename(campId, playerId, proposedName)) {
            return false;
        }
        setDirty();
        return true;
    }

    public boolean setPopulation(
            CityZoneKey cityZone,
            int civilians,
            int guards
    ) {
        return update(cityZone, settlement -> settlement.setPopulation(
                civilians,
                guards
        ));
    }

    public boolean addCivilian(
            CityZoneKey cityZone,
            int minimumRations,
            int maximumCivilians
    ) {
        return update(cityZone, settlement -> settlement.addCivilian(
                minimumRations,
                maximumCivilians
        ));
    }

    public boolean removeCivilian(
            CityZoneKey cityZone,
            UUID settlementId
    ) {
        return update(cityZone, settlement -> settlement.removeCivilian(
                settlementId
        ));
    }

    public boolean addRifleman(
            ServerLevel level,
            CityZoneKey cityZone,
            int minimumRations,
            int minimumAmmunition,
            int loadedAmmunition,
            int maximumRiflemen
    ) {
        return update(cityZone, settlement -> settlement.addRifleman(
                level,
                minimumRations,
                minimumAmmunition,
                loadedAmmunition,
                maximumRiflemen
        ));
    }

    public boolean removeRifleman(
            CityZoneKey cityZone,
            UUID settlementId
    ) {
        return update(cityZone, settlement -> settlement.removeRifleman(
                settlementId
        ));
    }

    public int withdrawMosinAmmunition(
            ServerLevel level,
            CityZoneKey cityZone,
            int requestedRounds
    ) {
        Settlement settlement = settlements.get(cityZone);
        if (settlement == null) {
            return 0;
        }
        int removed = settlement.withdrawMosinAmmunition(
                level,
                requestedRounds
        );
        if (removed > 0) {
            setDirty();
        }
        return removed;
    }

    public void refreshStockpiles(MinecraftServer server) {
        boolean changed = false;
        for (Settlement settlement : settlements.values()) {
            ServerLevel level = levelFor(server, settlement);
            if (level != null) {
                changed |= settlement.refreshStockpile(
                        level,
                        level.getGameTime(),
                        false
                );
            }
        }
        if (changed) {
            setDirty();
        }
    }

    public void refreshStockpile(
            ServerLevel level,
            CityZoneKey cityZone,
            boolean force
    ) {
        Settlement settlement = settlements.get(cityZone);
        if (settlement != null && settlement.refreshStockpile(
                level,
                level.getGameTime(),
                force
        )) {
            setDirty();
        }
    }

    public void consumeDailyRations(
            MinecraftServer server,
            long currentDay,
            int rationsPerSettler
    ) {
        boolean changed = false;
        for (Settlement settlement : settlements.values()) {
            int requiredRations = settlement.beginRationDay(
                    currentDay,
                    rationsPerSettler
            );
            if (requiredRations < 0) {
                continue;
            }
            changed = true;
            int preparedConsumed = settlement.consumePreparedRations(
                    requiredRations
            );
            int physicalRations = requiredRations - preparedConsumed;
            ServerLevel level = levelFor(server, settlement);
            if (physicalRations > 0 && level != null) {
                changed |= settlement.applyPhysicalConsumption(
                        settlement.consumePhysicalStockpile(
                                level,
                                physicalRations
                        ),
                        physicalRations
                );
            }
        }
        if (changed) {
            setDirty();
        }
    }

    @Nullable
    private static ServerLevel levelFor(
            MinecraftServer server,
            Settlement settlement
    ) {
        ResourceKey<Level> key = ResourceKey.create(
                Registries.DIMENSION,
                settlement.cityZone().dimension()
        );
        return server.getLevel(key);
    }

    public boolean setUpgrade(
            CityZoneKey cityZone,
            SettlementUpgrade upgrade,
            boolean installed
    ) {
        return update(
                cityZone,
                settlement -> settlement.setUpgrade(upgrade, installed)
        );
    }

    public boolean setSiegeSchedule(
            CityZoneKey cityZone,
            SettlementSiegeState state,
            long nextSiegeAt
    ) {
        return update(
                cityZone,
                settlement -> settlement.setSiegeSchedule(state, nextSiegeAt)
        );
    }

    public void markRadioDestroyed(
            CityZoneKey cityZone,
            UUID campId,
            BlockPos radioPosition,
            long gameTime
    ) {
        Settlement settlement = settlements.get(cityZone);
        if (settlement != null && settlement.markRadioDestroyed(
                campId,
                radioPosition,
                gameTime
        )) {
            setDirty();
        }
    }

    @Override
    public CompoundTag save(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        tag.putInt("version", FORMAT_VERSION);
        ListTag savedSettlements = new ListTag();
        for (Settlement settlement : settlements.values()) {
            savedSettlements.add(settlement.save());
        }
        tag.put("settlements", savedSettlements);
        return tag;
    }

    private static SettlementSavedData load(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        SettlementSavedData data = new SettlementSavedData();
        ListTag savedSettlements = tag.getList(
                "settlements",
                Tag.TAG_COMPOUND
        );
        for (int index = 0; index < savedSettlements.size(); index++) {
            try {
                Settlement settlement = Settlement.load(
                        savedSettlements.getCompound(index)
                );
                data.settlements.put(settlement.cityZone(), settlement);
            } catch (RuntimeException ignored) {
                // Keep every valid city settlement when one entry is corrupt.
            }
        }
        return data;
    }

    private boolean update(
            CityZoneKey cityZone,
            SettlementMutation mutation
    ) {
        Settlement settlement = settlements.get(cityZone);
        if (settlement == null || !mutation.apply(settlement)) {
            return false;
        }
        setDirty();
        return true;
    }

    @FunctionalInterface
    private interface SettlementMutation {

        boolean apply(Settlement settlement);
    }
}
