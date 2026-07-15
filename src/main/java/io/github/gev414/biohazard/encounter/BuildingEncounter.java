package io.github.gev414.biohazard.encounter;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.UUID;

public final class BuildingEncounter {

    private static final int DATA_VERSION = 1;

    private final ResourceLocation buildingId;
    private final boolean bossSelected;
    private final int targetKills;

    private EncounterPhase phase;
    private int regularDeaths;
    @Nullable
    private UUID bossUuid;
    private long bossReadyGameTime;

    private BuildingEncounter(
            ResourceLocation buildingId,
            boolean bossSelected,
            int targetKills,
            EncounterPhase phase,
            int regularDeaths,
            @Nullable UUID bossUuid,
            long bossReadyGameTime
    ) {
        this.buildingId = buildingId;
        this.bossSelected = bossSelected;
        this.targetKills = Math.max(0, targetKills);
        this.phase = phase;
        this.regularDeaths = Math.clamp(
                regularDeaths,
                0,
                this.targetKills
        );
        this.bossUuid = bossUuid;
        this.bossReadyGameTime = bossReadyGameTime;
    }

    public static BuildingEncounter materialize(
            ResourceLocation buildingId,
            EncounterSelection selection
    ) {
        EncounterPhase initialPhase = selection.haunted()
                ? EncounterPhase.REGULAR_WAVE
                : EncounterPhase.SAFE;
        return new BuildingEncounter(
                buildingId,
                selection.bossSelected(),
                selection.targetKills(),
                initialPhase,
                0,
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

    public EncounterPhase phase() {
        return phase;
    }

    public int regularDeaths() {
        return regularDeaths;
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

    public boolean clear() {
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
        tag.putString("phase", phase.name());
        tag.putInt("regularDeaths", regularDeaths);
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

        return new BuildingEncounter(
                ResourceLocation.parse(tag.getString("buildingId")),
                tag.getBoolean("bossSelected"),
                tag.getInt("targetKills"),
                loadedPhase,
                tag.getInt("regularDeaths"),
                loadedBossUuid,
                tag.getLong("bossReadyGameTime")
        );
    }
}
