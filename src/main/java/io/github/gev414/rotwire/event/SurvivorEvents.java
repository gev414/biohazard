package io.github.gev414.rotwire.event;

import io.github.gev414.rotwire.entity.SurvivorEntity;
import io.github.gev414.rotwire.settlement.SettlementManager;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/**
 * Synchronizes the durable settlement population with physical survivors.
 */
public final class SurvivorEvents {

    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.isCanceled()
                || !(event.getEntity() instanceof SurvivorEntity survivor)
                || !(survivor.level() instanceof ServerLevel level)) {
            return;
        }
        survivor.settlementBinding().ifPresent(binding -> {
            if (survivor.isRifleman()) {
                SettlementManager.removeRifleman(
                        level,
                        binding.cityZone(),
                        binding.settlementId()
                );
            } else if (survivor.isPistolman()) {
                SettlementManager.removePistolman(
                        level,
                        binding.cityZone(),
                        binding.settlementId()
                );
            } else if (survivor.isShotgunner()) {
                SettlementManager.removeShotgunner(
                        level,
                        binding.cityZone(),
                        binding.settlementId()
                );
            } else {
                SettlementManager.removeCivilian(
                        level,
                        binding.cityZone(),
                        binding.settlementId()
                );
            }
        });
    }

    private SurvivorEvents() {
    }
}
