package io.github.gev414.rotwire.entity.ai;

import io.github.gev414.rotwire.config.SettlementConfig;
import io.github.gev414.rotwire.entity.SurvivorEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Selects normal navigable paths around the city while keeping a survivor in
 * reach of its own camp. Unlike a vanilla restriction, this leaves room for
 * the survivor to leave the small shelter radius during calm periods.
 */
public final class SurvivorCityRoamGoal extends RandomStrollGoal {

    private static final int POSITION_ATTEMPTS = 8;
    private static final int STEP_RADIUS = 16;

    private final SurvivorEntity survivor;

    public SurvivorCityRoamGoal(
            SurvivorEntity survivor,
            double speedModifier
    ) {
        super(survivor, speedModifier, DEFAULT_INTERVAL, false);
        this.survivor = survivor;
    }

    @Override
    public void start() {
        if (!(survivor.level() instanceof ServerLevel level)
                || !SurvivorNavigationBudget.requestPath(level)) {
            return;
        }
        long pathStartedAt = System.nanoTime();
        super.start();
        SurvivorNavigationBudget.recordPathCalculation(
                level,
                System.nanoTime() - pathStartedAt
        );
    }

    @Override
    protected @Nullable Vec3 getPosition() {
        if (!survivor.canRoamCity()) {
            return null;
        }

        BlockPos home = survivor.homePosition();
        if (home == null) {
            return null;
        }

        int roamRadius = SettlementConfig.CIVILIAN_CITY_ROAM_RADIUS.get();
        double maximumDistanceSqr = (double) roamRadius * roamRadius;

        for (int attempt = 0; attempt < POSITION_ATTEMPTS; attempt++) {
            Vec3 candidate = LandRandomPos.getPos(
                    survivor,
                    STEP_RADIUS,
                    7
            );
            if (candidate != null
                    && home.distSqr(BlockPos.containing(candidate))
                    <= maximumDistanceSqr) {
                return candidate;
            }
        }

        return DefaultRandomPos.getPosTowards(
                survivor,
                STEP_RADIUS,
                7,
                Vec3.atBottomCenterOf(home),
                (float) (Math.PI / 2.0D)
        );
    }
}
