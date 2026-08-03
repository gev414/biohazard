package io.github.gev414.rotwire.settlement;

import io.github.gev414.rotwire.city.CityZoneKey;
import io.github.gev414.rotwire.config.SettlementConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

/**
 * Settlement application service used by radios today and survivors, travel,
 * and siege systems later.
 */
public final class SettlementManager {

    public static Optional<SettlementSnapshot> syncRadio(
            ServerLevel level,
            CityZoneKey cityZone,
            UUID campId,
            @Nullable UUID owner,
            BlockPos radioPosition,
            BlockPos campCenter,
            int campRadius,
            boolean campActive,
            boolean connected,
            int installedModules
    ) {
        return SettlementSavedData.get(level.getServer()).syncRadio(
                cityZone,
                campId,
                owner,
                radioPosition,
                campCenter,
                campRadius,
                campActive,
                connected,
                installedModules,
                level.getGameTime(),
                level.getDayTime() / 24_000L
        );
    }

    public static Optional<SettlementSnapshot> status(
            ServerLevel level,
            CityZoneKey cityZone
    ) {
        return SettlementSavedData.get(level.getServer()).status(cityZone);
    }

    public static boolean renamePrimary(
            ServerLevel level,
            CityZoneKey cityZone,
            UUID campId,
            UUID playerId,
            String proposedName
    ) {
        return SettlementSavedData.get(level.getServer()).renamePrimary(
                cityZone,
                campId,
                playerId,
                proposedName
        );
    }

    public static boolean setPopulation(
            ServerLevel level,
            CityZoneKey cityZone,
            int civilians,
            int guards
    ) {
        return SettlementSavedData.get(level.getServer()).setPopulation(
                cityZone,
                civilians,
                guards
        );
    }

    /**
     * Reserves one persistent civilian population slot after all current hub,
     * food, and capacity requirements have been satisfied.
     */
    public static boolean addCivilian(
            ServerLevel level,
            CityZoneKey cityZone,
            int minimumRations,
            int maximumCivilians
    ) {
        return SettlementSavedData.get(level.getServer()).addCivilian(
                cityZone,
                minimumRations,
                maximumCivilians
        );
    }

    /**
     * Releases a civilian slot only when it belongs to the same settlement.
     * This is called from the survivor's server-side death lifecycle.
     */
    public static boolean removeCivilian(
            ServerLevel level,
            CityZoneKey cityZone,
            UUID settlementId
    ) {
        return SettlementSavedData.get(level.getServer()).removeCivilian(
                cityZone,
                settlementId
        );
    }

    /**
     * Reserves a guard slot and withdraws the rounds loaded into the new
     * rifleman's physical Mosin magazine.
     */
    public static boolean addRifleman(
            ServerLevel level,
            CityZoneKey cityZone,
            int minimumRations,
            int minimumAmmunition,
            int loadedAmmunition,
            int maximumRiflemen
    ) {
        return SettlementSavedData.get(level.getServer()).addRifleman(
                level,
                cityZone,
                minimumRations,
                minimumAmmunition,
                loadedAmmunition,
                maximumRiflemen
        );
    }

    public static boolean removeRifleman(
            ServerLevel level,
            CityZoneKey cityZone,
            UUID settlementId
    ) {
        return SettlementSavedData.get(level.getServer()).removeRifleman(
                cityZone,
                settlementId
        );
    }

    public static int withdrawMosinAmmunition(
            ServerLevel level,
            CityZoneKey cityZone,
            int requestedRounds
    ) {
        return SettlementSavedData.get(level.getServer())
                .withdrawMosinAmmunition(
                        level,
                        cityZone,
                        requestedRounds
                );
    }

    public static void refreshStockpile(
            ServerLevel level,
            CityZoneKey cityZone,
            boolean force
    ) {
        SettlementSavedData.get(level.getServer()).refreshStockpile(
                level,
                cityZone,
                force
        );
    }

    public static boolean setUpgrade(
            ServerLevel level,
            CityZoneKey cityZone,
            SettlementUpgrade upgrade,
            boolean installed
    ) {
        return SettlementSavedData.get(level.getServer()).setUpgrade(
                cityZone,
                upgrade,
                installed
        );
    }

    public static boolean setSiegeSchedule(
            ServerLevel level,
            CityZoneKey cityZone,
            SettlementSiegeState state,
            long nextSiegeAt
    ) {
        return SettlementSavedData.get(level.getServer()).setSiegeSchedule(
                cityZone,
                state,
                nextSiegeAt
        );
    }

    public static void markRadioDestroyed(
            ServerLevel level,
            CityZoneKey cityZone,
            UUID campId,
            BlockPos radioPosition
    ) {
        SettlementSavedData.get(level.getServer()).markRadioDestroyed(
                cityZone,
                campId,
                radioPosition,
                level.getGameTime()
        );
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        ServerLevel overworld = event.getServer().overworld();
        if (overworld.getGameTime() % 20L != 0L) {
            return;
        }
        SettlementSavedData data = SettlementSavedData.get(
                event.getServer()
        );
        data.refreshStockpiles(event.getServer());
        data.consumeDailyRations(
                event.getServer(),
                overworld.getDayTime() / 24_000L,
                SettlementConfig.RATIONS_PER_SETTLER_PER_DAY.get()
        );
    }

    private SettlementManager() {
    }
}
