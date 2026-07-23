package io.github.gev414.biohazard.encounter;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.UUID;

public final class BuildingEncounter {

    private static final int DATA_VERSION = 2;

    private final ResourceLocation buildingId;
    private final boolean bossSelected;
    private final int targetKills;
    private final EncounterSpawnMode spawnMode;

    private EncounterPhase phase;
    private int regularDeaths;
    private int regularSpawns;
    private boolean initialPopulationAttempted;
    @Nullable
    private UUID bossUuid;
    private long bossReadyGameTime;

    private BuildingEncounter(
            ResourceLocation buildingId,
            boolean bossSelected,
            int targetKills,
            EncounterSpawnMode spawnMode,
            EncounterPhase phase,
            int regularDeaths,
            int regularSpawns,
            boolean initialPopulationAttempted,
            @Nullable UUID bossUuid,
            long bossReadyGameTime
    ) {
        this.buildingId = buildingId;
        this.bossSelected = bossSelected;
        this.targetKills = Math.max(0, targetKills);
        this.spawnMode = Objects.requireNonNull(spawnMode, "spawnMode");
        this.phase = phase;
        this.regularDeaths = Math.clamp(
                regularDeaths,
                0,
                this.targetKills
        );
        this.regularSpawns = Math.clamp(
                regularSpawns,
                0,
                this.targetKills
        );
        this.initialPopulationAttempted = initialPopulationAttempted;
        this.bossUuid = bossUuid;
        this.bossReadyGameTime = bossReadyGameTime;
    }

    public static BuildingEncounter materialize(
            ResourceLocation buildingId,
            EncounterSelection selection,
            EncounterSpawnMode spawnMode
    ) {
        EncounterPhase initialPhase;
        if (selection.haunted()) {
            initialPhase = EncounterPhase.REGULAR_WAVE;
        } else if (selection.bossSelected()) {
            initialPhase = EncounterPhase.BOSS_PENDING;
        } else {
            initialPhase = EncounterPhase.SAFE;
        }
        return new BuildingEncounter(
                buildingId,
                selection.bossSelected(),
                selection.targetKills(),
                spawnMode,
                initialPhase,
                0,
                0,
                false,
                null,
                0L
        );
    }

    public ResourceLocation buildingId() {
        return buildingId;
    }

    public boolean bossSelected() {
        return bossSelected;
    }

    public int targetKills() {
        return targetKills;
    }

    public EncounterSpawnMode spawnMode() {
        return spawnMode;
    }

    public EncounterPhase phase() {
        return phase;
    }

    public int regularDeaths() {
        return regularDeaths;
    }

    public int regularSpawns() {
        return regularSpawns;
    }

    public int remainingInitialSpawns() {
        return Math.max(0, targetKills - regularSpawns);
    }

    public boolean initialPopulationAttempted() {
        return initialPopulationAttempted;
    }

    @Nullable
    public UUID bossUuid() {
        return bossUuid;
    }

    public long bossReadyGameTime() {
        return bossReadyGameTime;
    }

    public boolean recordRegularDeath() {
        if (phase == EncounterPhase.SAFE
                || phase == EncounterPhase.CLEARED
                || regularDeaths >= targetKills) {
            return false;
        }
        regularDeaths++;
        return true;
    }

    public boolean recordRegularSpawn() {
        if (spawnMode != EncounterSpawnMode.INSTANT
                || phase != EncounterPhase.REGULAR_WAVE
                || regularSpawns >= targetKills) {
            return false;
        }
        regularSpawns++;
        return true;
    }

    public boolean beginInitialPopulation() {
        if (spawnMode != EncounterSpawnMode.INSTANT
                || phase != EncounterPhase.REGULAR_WAVE
                || initialPopulationAttempted) {
            return false;
        }
        initialPopulationAttempted = true;
        return true;
    }

    public boolean beginBossWarning(long readyGameTime) {
        if (phase != EncounterPhase.REGULAR_WAVE) {
            return false;
        }
        phase = EncounterPhase.BOSS_PENDING;
        bossReadyGameTime = readyGameTime;
        return true;
    }

    public boolean activateBoss(UUID uuid) {
        if (phase != EncounterPhase.BOSS_PENDING) {
            return false;
        }
        phase = EncounterPhase.BOSS_ACTIVE;
        bossUuid = uuid;
        return true;
    }

    boolean clear() {
        if (phase == EncounterPhase.SAFE
                || phase == EncounterPhase.CLEARED) {
            return false;
        }
        phase = EncounterPhase.CLEARED;
        bossReadyGameTime = 0L;
        return true;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("version", DATA_VERSION);
        tag.putString("buildingId", buildingId.toString());
        tag.putBoolean("bossSelected", bossSelected);
        tag.putInt("targetKills", targetKills);
        tag.putString("spawnMode", spawnMode.name());
        tag.putString("phase", phase.name());
        tag.putInt("regularDeaths", regularDeaths);
        tag.putInt("regularSpawns", regularSpawns);
        tag.putBoolean(
                "initialPopulationAttempted",
                initialPopulationAttempted
        );
        if (bossUuid != null) {
            tag.putUUID("bossUuid", bossUuid);
        }
        tag.putLong("bossReadyGameTime", bossReadyGameTime);
        return tag;
    }

    public static BuildingEncounter load(CompoundTag tag) {
        EncounterPhase loadedPhase;
        try {
            loadedPhase = EncounterPhase.valueOf(
                    tag.getString("phase")
            );
        } catch (IllegalArgumentException exception) {
            loadedPhase = EncounterPhase.SAFE;
        }

        UUID loadedBossUuid = tag.hasUUID("bossUuid")
                ? tag.getUUID("bossUuid")
                : null;

        EncounterSpawnMode loadedSpawnMode;
        try {
            loadedSpawnMode = EncounterSpawnMode.valueOf(
                    tag.getString("spawnMode")
            );
        } catch (IllegalArgumentException exception) {
            // Version 1 encounters predate instant population mode.
            loadedSpawnMode = EncounterSpawnMode.WAVE;
        }

        return new BuildingEncounter(
                ResourceLocation.parse(tag.getString("buildingId")),
                tag.getBoolean("bossSelected"),
                tag.getInt("targetKills"),
                loadedSpawnMode,
                loadedPhase,
                tag.getInt("regularDeaths"),
                tag.getInt("regularSpawns"),
                tag.getBoolean("initialPopulationAttempted"),
                loadedBossUuid,
                tag.getLong("bossReadyGameTime")
        );
    }
}
