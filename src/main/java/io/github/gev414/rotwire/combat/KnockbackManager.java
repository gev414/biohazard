package io.github.gev414.rotwire.combat;

import io.github.gev414.rotwire.config.SurvivalSystemsConfig;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;

public final class KnockbackManager {

    public static void onLivingKnockBack(LivingKnockBackEvent event) {
        if (event.getEntity().level().isClientSide()
                || !SurvivalSystemsConfig.KNOCKBACK_ENABLED.get()) {
            return;
        }

        double retainedFraction;
        if (event.getEntity() instanceof Zombie) {
            retainedFraction =
                    SurvivalSystemsConfig.ZOMBIE_KNOCKBACK_RETENTION.get();
        } else if (event.getEntity() instanceof Player) {
            retainedFraction =
                    SurvivalSystemsConfig.PLAYER_KNOCKBACK_RETENTION.get();
        } else {
            return;
        }

        event.setStrength(retainedStrength(
                event.getStrength(),
                retainedFraction
        ));
    }

    static float retainedStrength(
            float strength,
            double retainedFraction
    ) {
        return (float) (strength * retainedFraction);
    }

    private KnockbackManager() {
    }
}
