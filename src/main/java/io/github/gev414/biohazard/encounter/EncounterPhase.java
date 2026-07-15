package io.github.gev414.biohazard.encounter;

public enum EncounterPhase {
    SAFE,
    REGULAR_WAVE,
    BOSS_PENDING,
    BOSS_ACTIVE,
    CLEARED;

    public boolean locksContainers() {
        return this == REGULAR_WAVE
                || this == BOSS_PENDING
                || this == BOSS_ACTIVE;
    }
}
