package io.github.gev414.rotwire.settlement;

/**
 * Durable lifecycle for the settlement siege scheduler.
 */
public enum SettlementSiegeState {
    CALM,
    WARNING,
    ACTIVE,
    RECOVERY
}
