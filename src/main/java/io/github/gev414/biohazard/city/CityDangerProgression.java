package io.github.gev414.biohazard.city;

public final class CityDangerProgression {

    public static int level(
            int clearedBuildings,
            int buildingsPerLevel,
            int maximumLevel
    ) {
        int safeCleared = Math.max(0, clearedBuildings);
        int safePerLevel = Math.max(1, buildingsPerLevel);
        int safeMaximum = Math.max(0, maximumLevel);
        return Math.min(safeMaximum, safeCleared / safePerLevel);
    }

    public static int remainingUntilNextLevel(
            int clearedBuildings,
            int buildingsPerLevel,
            int maximumLevel
    ) {
        int currentLevel = level(
                clearedBuildings,
                buildingsPerLevel,
                maximumLevel
        );
        if (currentLevel >= Math.max(0, maximumLevel)) {
            return 0;
        }
        int safeCleared = Math.max(0, clearedBuildings);
        int safePerLevel = Math.max(1, buildingsPerLevel);
        return safePerLevel - Math.floorMod(safeCleared, safePerLevel);
    }

    private CityDangerProgression() {
    }
}
