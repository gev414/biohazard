package io.github.gev414.biohazard.city;

import net.minecraft.world.level.ChunkPos;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.Set;

public final class CitySurvey {

    private static final int[][] CARDINAL_NEIGHBORS = {
            {1, 0},
            {-1, 0},
            {0, 1},
            {0, -1}
    };
    private static final int[][] DIAGONAL_NEIGHBORS = {
            {1, 0},
            {-1, 0},
            {0, 1},
            {0, -1},
            {1, 1},
            {1, -1},
            {-1, 1},
            {-1, -1}
    };

    private final int originChunkX;
    private final int originChunkZ;
    private final Deque<Long> frontier = new ArrayDeque<>();
    private final Set<Long> scheduled = new LinkedHashSet<>();
    private final Set<Long> visited = new LinkedHashSet<>();
    private final Set<Long> cityChunks = new LinkedHashSet<>();

    private boolean complete;
    private boolean capped;

    public CitySurvey(int originChunkX, int originChunkZ) {
        this.originChunkX = originChunkX;
        this.originChunkZ = originChunkZ;
        schedule(originChunkX, originChunkZ);
    }

    public void advance(
            ChunkClassifier classifier,
            int chunkBudget,
            int maximumChunks,
            boolean diagonalConnectivity
    ) {
        if (complete) {
            return;
        }

        int safeBudget = Math.max(1, chunkBudget);
        int safeMaximum = Math.max(1, maximumChunks);
        int[][] neighbors = diagonalConnectivity
                ? DIAGONAL_NEIGHBORS
                : CARDINAL_NEIGHBORS;

        int inspected = 0;
        while (inspected < safeBudget && !frontier.isEmpty()) {
            if (visited.size() >= safeMaximum) {
                capped = true;
                complete = true;
                return;
            }

            long packed = frontier.removeFirst();
            if (!visited.add(packed)) {
                continue;
            }
            inspected++;

            int chunkX = ChunkPos.getX(packed);
            int chunkZ = ChunkPos.getZ(packed);
            if (!classifier.isCity(chunkX, chunkZ)) {
                continue;
            }

            cityChunks.add(packed);
            for (int[] offset : neighbors) {
                schedule(chunkX + offset[0], chunkZ + offset[1]);
            }
        }

        if (frontier.isEmpty()) {
            complete = true;
        } else if (visited.size() >= safeMaximum) {
            capped = true;
            complete = true;
        }
    }

    public int originChunkX() {
        return originChunkX;
    }

    public int originChunkZ() {
        return originChunkZ;
    }

    public int scannedChunks() {
        return visited.size();
    }

    public boolean complete() {
        return complete;
    }

    public boolean capped() {
        return capped;
    }

    public Set<Long> cityChunks() {
        return Set.copyOf(cityChunks);
    }

    private void schedule(int chunkX, int chunkZ) {
        long packed = ChunkPos.asLong(chunkX, chunkZ);
        if (scheduled.add(packed)) {
            frontier.addLast(packed);
        }
    }

    @FunctionalInterface
    public interface ChunkClassifier {
        boolean isCity(int chunkX, int chunkZ);
    }
}
