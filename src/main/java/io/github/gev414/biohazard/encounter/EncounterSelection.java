package io.github.gev414.biohazard.encounter;

public record EncounterSelection(
        boolean haunted,
        boolean bossSelected,
        int targetKills
) {

    private static final long GOLDEN_GAMMA =
            0x9E3779B97F4A7C15L;

    public static EncounterSelection select(
            long worldSeed,
            BuildingKey key,
            double hauntedChance,
            double bossChance,
            int minimumKills,
            int maximumKills
    ) {
        long seed = mix64(worldSeed);
        seed = mix64(seed ^ key.dimension().toString().hashCode());
        seed = mix64(seed ^ Integer.toUnsignedLong(key.rootChunkX()));
        seed = mix64(seed ^ Long.rotateLeft(
                Integer.toUnsignedLong(key.rootChunkZ()),
                32
        ));

        boolean haunted = unitDouble(seed) < hauntedChance;
        if (!haunted) {
            return new EncounterSelection(false, false, 0);
        }

        long bossRoll = mix64(seed + GOLDEN_GAMMA);
        boolean boss = unitDouble(bossRoll) < bossChance;

        int min = Math.min(minimumKills, maximumKills);
        int max = Math.max(minimumKills, maximumKills);
        long targetRoll = mix64(bossRoll + GOLDEN_GAMMA);
        int target = min + (int) Math.floorMod(
                targetRoll,
                (long) max - min + 1L
        );

        return new EncounterSelection(true, boss, target);
    }

    private static double unitDouble(long value) {
        return (value >>> 11) * 0x1.0p-53;
    }

    private static long mix64(long value) {
        value = (value ^ (value >>> 30))
                * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27))
                * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }
}
