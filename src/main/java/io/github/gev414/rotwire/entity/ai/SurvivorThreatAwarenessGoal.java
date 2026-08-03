package io.github.gev414.rotwire.entity.ai;

import io.github.gev414.rotwire.config.SettlementConfig;
import io.github.gev414.rotwire.entity.SurvivorEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;

import java.util.Comparator;
/**
 * Staggered short-range hostile awareness and transient contact memory. It
 * deliberately does not require line of sight at close range, allowing an
 * armed survivor to remember an infected immediately beyond a fence without
 * permitting the firearm itself to shoot through that fence.
 */
public final class SurvivorThreatAwarenessGoal extends Goal {

    private final SurvivorEntity survivor;
    private LivingEntity threat;
    private int scanCooldown;
    private int memoryTicks;

    public SurvivorThreatAwarenessGoal(SurvivorEntity survivor) {
        this.survivor = survivor;
        int interval = SettlementConfig
                .SURVIVOR_THREAT_SCAN_INTERVAL_TICKS.get();
        scanCooldown = survivor.getRandom().nextInt(Math.max(1, interval));
    }

    @Override
    public boolean canUse() {
        if (!survivor.canFightWithFirearm()) {
            return false;
        }
        LivingEntity current = survivor.getTarget();
        if (isValidThreat(current)) {
            threat = current;
            rememberContact();
            return true;
        }
        if (scanCooldown-- > 0) {
            return false;
        }
        resetScanCooldown();
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
        if (!survivor.canFightWithFirearm() || !isValidThreat(threat)) {
            return false;
        }
        LivingEntity current = survivor.getTarget();
        if (current != null && current != threat) {
            return false;
        }
        if (hasFreshContact()) {
            rememberContact();
        } else if (memoryTicks > 0) {
            memoryTicks--;
        }
        return memoryTicks > 0;
    }

    @Override
    public void start() {
        if (threat != null) {
            survivor.setTarget(threat);
            rememberContact();
        }
    }

    @Override
    public void tick() {
        if (threat != null && survivor.getTarget() == null) {
            survivor.setTarget(threat);
        }
    }

    @Override
    public void stop() {
        if (survivor.getTarget() == threat && !hasFreshContact()) {
            survivor.setTarget(null);
        }
        threat = null;
        memoryTicks = 0;
        resetScanCooldown();
    }

    private boolean isValidThreat(LivingEntity candidate) {
        return candidate instanceof Monster
                && candidate.isAlive()
                && candidate.level() == survivor.level();
    }

    private boolean hasFreshContact() {
        if (!isValidThreat(threat)) {
            return false;
        }
        int radius = SettlementConfig
                .SURVIVOR_HOSTILE_AWARENESS_RADIUS.get();
        double distanceSqr = survivor.distanceToSqr(threat);
        if (distanceSqr <= (double) radius * radius) {
            return true;
        }
        int firearmRange = survivor.firearmMaximumShootingDistance();
        return firearmRange > 0
                && distanceSqr <= (double) firearmRange * firearmRange
                && survivor.getSensing().hasLineOfSight(threat);
    }

    private void rememberContact() {
        memoryTicks = SettlementConfig.SURVIVOR_CONTACT_MEMORY_TICKS.get();
    }

    private void resetScanCooldown() {
        int interval = SettlementConfig
                .SURVIVOR_THREAT_SCAN_INTERVAL_TICKS.get();
        scanCooldown = interval + survivor.getRandom().nextInt(
                Math.max(1, interval / 2)
        );
    }
}
