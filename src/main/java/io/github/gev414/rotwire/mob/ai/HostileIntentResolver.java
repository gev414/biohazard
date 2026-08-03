package io.github.gev414.rotwire.mob.ai;

/** Pure precedence rule shared by runtime AI and unit tests. */
public final class HostileIntentResolver {

    public static HostileIntent resolve(
            boolean hasLivingTarget,
            boolean hasAssaultObjective,
            boolean hasInvestigation
    ) {
        if (hasLivingTarget) {
            return HostileIntent.HUNT;
        }
        if (hasAssaultObjective) {
            return HostileIntent.ASSAULT;
        }
        if (hasInvestigation) {
            return HostileIntent.INVESTIGATE;
        }
        return HostileIntent.IDLE;
    }

    private HostileIntentResolver() {
    }
}
