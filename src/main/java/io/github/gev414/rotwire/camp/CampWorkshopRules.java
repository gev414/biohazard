package io.github.gev414.rotwire.camp;

public final class CampWorkshopRules {

    public static int repairedDamage(int damage, int maximumDamage) {
        int safeMaximum = Math.max(1, maximumDamage);
        int safeDamage = Math.max(0, Math.min(damage, safeMaximum));
        int restored = Math.max(1, safeMaximum / 4);
        return Math.max(0, safeDamage - restored);
    }

    private CampWorkshopRules() {
    }
}
