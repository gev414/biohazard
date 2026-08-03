package io.github.gev414.rotwire.entity.ai;

import io.github.gev414.rotwire.config.SettlementConfig;
import io.github.gev414.rotwire.entity.SurvivorEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;

import java.util.Comparator;
import java.util.EnumSet;

/**
 * Short-range, direction-independent hostile awareness. It deliberately does
 * not check line of sight, allowing a survivor to notice a nearby zombie
 * behind itself or around a corner. Firearms still require line of sight in
 * the combat goal before a shot can be taken.
 */
public final class SurvivorThreatAwarenessGoal extends Goal {

    private static final int LOOK_DURATION_TICKS = 40;

    private final SurvivorEntity survivor;
    private LivingEntity threat;
    private int lookTicks;

    public SurvivorThreatAwarenessGoal(SurvivorEntity survivor) {
        this.survivor = survivor;
        setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (survivor.getTarget() != null) {
            return false;
        }
        int radius = SettlementConfig
                .SURVIVOR_HOSTILE_AWARENESS_RADIUS.get();
        double maximumDistanceSqr = (double) radius * radius;
        threat = survivor.level().getEntitiesOfClass(
                Monster.class,
                survivor.getBoundingBox().inflate(radius, 6.0D, radius),
                monster -> monster.isAlive()
                        && survivor.distanceToSqr(monster)
                        <= maximumDistanceSqr
        ).stream().min(Comparator.comparingDouble(
                survivor::distanceToSqr
        )).orElse(null);
        return threat != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (threat == null || !threat.isAlive()
                || survivor.getTarget() != null || lookTicks <= 0) {
            return false;
        }
        int radius = SettlementConfig
                .SURVIVOR_HOSTILE_AWARENESS_RADIUS.get();
        return survivor.distanceToSqr(threat)
                <= (double) radius * radius;
    }

    @Override
    public void start() {
        lookTicks = LOOK_DURATION_TICKS;
        if (threat != null && survivor.canFightWithMosin()) {
            survivor.setTarget(threat);
        }
    }

    @Override
    public void tick() {
        if (threat != null) {
            survivor.getLookControl().setLookAt(threat, 40.0F, 40.0F);
        }
        lookTicks--;
    }

    @Override
    public void stop() {
        threat = null;
        lookTicks = 0;
    }
}
