package io.github.gev414.rotwire.settlement;

import io.github.gev414.rotwire.city.CityZoneKey;
import net.minecraft.core.BlockPos;

import java.util.UUID;

/**
 * Immutable settlement view for radio UI and future survivor/travel systems.
 * Rations are hunger points from the last physical stockpile scan plus any
 * prepared portions; container counts are not a duplicate inventory.
 */
public record SettlementSnapshot(
        UUID id,
        CityZoneKey cityZone,
        String name,
        BlockPos primaryRadioPosition,
        UUID primaryCampId,
        int civilianPopulation,
        int guardPopulation,
        int rations,
        int rationContainerCount,
        int mosinAmmunition,
        int upgradeMask,
        SettlementSiegeState siegeState,
        long nextSiegeAt,
        int radioCount,
        int onlineRadioCount,
        int activeRadioCount,
        boolean primaryRadioDestroyed
) {

    public int population() {
        return (int) Math.min(
                Integer.MAX_VALUE,
                (long) civilianPopulation + guardPopulation
        );
    }

    public boolean hasUpgrade(SettlementUpgrade upgrade) {
        return (upgradeMask & upgrade.mask()) != 0;
    }
}
