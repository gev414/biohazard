package io.github.gev414.rotwire.settlement;

import io.github.gev414.rotwire.city.CityZoneKey;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettlementTest {

    @Test
    void firstCampStaysPrimaryAndStateSurvivesSerialization() {
        CityZoneKey city = new CityZoneKey(
                ResourceLocation.parse("minecraft:overworld"),
                -8,
                12,
                false
        );
        UUID primaryCamp = UUID.randomUUID();
        UUID primaryOwner = UUID.randomUUID();
        UUID relayCamp = UUID.randomUUID();
        Settlement settlement = Settlement.createPrimary(
                city,
                primaryCamp,
                primaryOwner,
                new BlockPos(20, 72, -40),
                new BlockPos(20, 72, -40),
                8,
                true,
                true,
                3,
                400L,
                4L
        );

        assertTrue(settlement.rename(
                primaryCamp,
                primaryOwner,
                "Northbridge"
        ));
        assertTrue(settlement.updateRadio(
                relayCamp,
                new BlockPos(80, 69, -16),
                new BlockPos(80, 69, -16),
                8,
                false,
                true,
                1,
                420L
        ));
        assertTrue(settlement.setPopulation(3, 1));
        assertTrue(settlement.updateStockpile(
                new SettlementStockpile.StockpileSnapshot(140, 2)
        ));
        assertTrue(settlement.setUpgrade(
                SettlementUpgrade.CAMP_HUB,
                true
        ));
        assertTrue(settlement.setUpgrade(
                SettlementUpgrade.FAST_TRAVEL,
                true
        ));
        assertTrue(settlement.setSiegeSchedule(
                SettlementSiegeState.WARNING,
                12_000L
        ));

        Settlement restored = Settlement.load(settlement.save());
        SettlementSnapshot snapshot = restored.snapshot();

        assertEquals(primaryCamp, snapshot.primaryCampId());
        assertEquals("Northbridge", snapshot.name());
        assertEquals(4, snapshot.population());
        assertEquals(140, snapshot.rations());
        assertEquals(2, snapshot.rationContainerCount());
        assertTrue(snapshot.hasUpgrade(SettlementUpgrade.CAMP_HUB));
        assertTrue(snapshot.hasUpgrade(SettlementUpgrade.FAST_TRAVEL));
        assertEquals(SettlementSiegeState.WARNING, snapshot.siegeState());
        assertEquals(12_000L, snapshot.nextSiegeAt());
        assertEquals(2, snapshot.radioCount());
        assertEquals(2, snapshot.onlineRadioCount());
        assertEquals(1, snapshot.activeRadioCount());
    }

    @Test
    void destroyedPrimaryIsRetainedInsteadOfPromotingARelay() {
        CityZoneKey city = new CityZoneKey(
                ResourceLocation.parse("minecraft:overworld"),
                0,
                0,
                false
        );
        UUID primaryCamp = UUID.randomUUID();
        UUID relayCamp = UUID.randomUUID();
        Settlement settlement = Settlement.createPrimary(
                city,
                primaryCamp,
                UUID.randomUUID(),
                BlockPos.ZERO,
                BlockPos.ZERO,
                8,
                true,
                true,
                0,
                0L,
                0L
        );
        settlement.updateRadio(
                relayCamp,
                new BlockPos(64, 70, 64),
                new BlockPos(64, 70, 64),
                8,
                true,
                true,
                0,
                20L
        );

        assertTrue(settlement.markRadioDestroyed(
                primaryCamp,
                BlockPos.ZERO,
                40L
        ));
        SettlementSnapshot snapshot = settlement.snapshot();

        assertEquals(primaryCamp, snapshot.primaryCampId());
        assertTrue(snapshot.primaryRadioDestroyed());
        assertEquals(1, snapshot.onlineRadioCount());
        assertFalse(settlement.rename(
                relayCamp,
                UUID.randomUUID(),
                "Replacement Hub"
        ));
    }

    @Test
    void campHubStartsDailyRationConsumptionOnlyAfterInstallation() {
        Settlement settlement = Settlement.createPrimary(
                new CityZoneKey(
                        ResourceLocation.parse("minecraft:overworld"),
                        0,
                        0,
                        false
                ),
                UUID.randomUUID(),
                UUID.randomUUID(),
                BlockPos.ZERO,
                BlockPos.ZERO,
                8,
                true,
                true,
                0,
                0L,
                10L
        );
        settlement.setPopulation(2, 1);
        settlement.updateStockpile(
                new SettlementStockpile.StockpileSnapshot(20, 1)
        );

        assertEquals(0, settlement.beginRationDay(11L, 2));
        assertEquals(20, settlement.snapshot().rations());

        assertTrue(settlement.setUpgrade(
                SettlementUpgrade.CAMP_HUB,
                true
        ));
        assertEquals(6, settlement.beginRationDay(12L, 2));
        assertEquals(20, settlement.snapshot().rations());
        assertEquals(-1, settlement.beginRationDay(12L, 2));

        Settlement restored = Settlement.load(settlement.save());
        assertEquals(-1, restored.beginRationDay(12L, 2));
        assertEquals(6, restored.beginRationDay(13L, 2));
    }

    @Test
    void wholeFoodKeepsUnusedNutritionAsPreparedRations() {
        Settlement settlement = Settlement.createPrimary(
                new CityZoneKey(
                        ResourceLocation.parse("minecraft:overworld"),
                        0,
                        0,
                        false
                ),
                UUID.randomUUID(),
                UUID.randomUUID(),
                BlockPos.ZERO,
                BlockPos.ZERO,
                8,
                true,
                true,
                0,
                0L,
                0L
        );
        settlement.updateStockpile(
                new SettlementStockpile.StockpileSnapshot(20, 1)
        );

        assertTrue(settlement.applyPhysicalConsumption(
                new SettlementStockpile.StockpileConsumption(
                        new SettlementStockpile.StockpileSnapshot(16, 1),
                        4
                ),
                1
        ));
        assertEquals(19, settlement.snapshot().rations());
        assertEquals(3, settlement.consumePreparedRations(3));
        assertEquals(16, settlement.snapshot().rations());
    }

    @Test
    void civilianRecruitmentRequiresAHubNameFoodAndAvailableSlot() {
        UUID owner = UUID.randomUUID();
        Settlement settlement = Settlement.createPrimary(
                new CityZoneKey(
                        ResourceLocation.parse("minecraft:overworld"),
                        2,
                        -3,
                        false
                ),
                UUID.randomUUID(),
                owner,
                BlockPos.ZERO,
                BlockPos.ZERO,
                8,
                true,
                true,
                0,
                0L,
                0L
        );

        assertFalse(settlement.addCivilian(100, 1));
        settlement.updateStockpile(
                new SettlementStockpile.StockpileSnapshot(100, 1)
        );
        assertFalse(settlement.addCivilian(100, 1));

        settlement.setUpgrade(SettlementUpgrade.CAMP_HUB, true);
        assertFalse(settlement.addCivilian(100, 1));

        assertTrue(settlement.rename(
                settlement.snapshot().primaryCampId(),
                owner,
                "Harbor Camp"
        ));
        assertTrue(settlement.addCivilian(100, 1));
        assertEquals(1, settlement.snapshot().civilianPopulation());
        assertFalse(settlement.addCivilian(100, 1));
        assertTrue(settlement.removeCivilian(settlement.snapshot().id()));
        assertEquals(0, settlement.snapshot().civilianPopulation());
    }
}
