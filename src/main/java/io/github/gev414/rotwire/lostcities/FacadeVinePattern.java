package io.github.gev414.rotwire.lostcities;

import java.util.SplittableRandom;

final class FacadeVinePattern {

    private static final int FACADE_WIDTH = 16;

    static boolean[][] create(
            long seed,
            float vineChance,
            int height
    ) {
        if (height <= 0) {
            return new boolean[FACADE_WIDTH][0];
        }

        boolean[][] pattern = new boolean[FACADE_WIDTH][height];
        if (vineChance <= 0.0F) {
            return pattern;
        }

        double chance = Math.min(1.0D, vineChance);
        double columnChance = Math.min(
                0.72D,
                0.10D + 0.67D * chance
        );
        double startChance = 0.03D + 0.20D * chance;
        double continuationChance = 0.80D + 0.08D * chance;

        SplittableRandom facadeRandom = new SplittableRandom(seed);
        for (int along = 0; along < FACADE_WIDTH; along++) {
            SplittableRandom columnRandom = facadeRandom.split();
            if (columnRandom.nextDouble() >= columnChance) {
                continue;
            }

            boolean running = false;
            for (int offset = height - 1; offset >= 0; offset--) {
                running = columnRandom.nextDouble() < (
                        running ? continuationChance : startChance
                );
                pattern[along][offset] = running;
            }
        }
        return pattern;
    }

    private FacadeVinePattern() {
    }
}
