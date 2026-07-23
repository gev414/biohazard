package io.github.gev414.biohazard.encumbrance;

public record EncumbranceSnapshot(
        double weight,
        EncumbranceTier tier
) {

    public static final EncumbranceSnapshot EMPTY =
            new EncumbranceSnapshot(0.0D, EncumbranceTier.LIGHT);

    public EncumbranceSnapshot {
        weight = Math.max(0.0D, weight);
    }
}
