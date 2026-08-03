package io.github.gev414.rotwire.settlement;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

/**
 * Last authoritative state reported by one physical camp radio.
 */
public record SettlementRadioStatus(
        UUID campId,
        BlockPos position,
        BlockPos campCenter,
        int campRadius,
        SettlementRadioRole role,
        boolean campActive,
        boolean connected,
        boolean destroyed,
        int installedModules,
        long updatedAt
) {

    CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("campId", campId);
        tag.putLong("position", position.asLong());
        tag.putLong("campCenter", campCenter.asLong());
        tag.putInt("campRadius", campRadius);
        tag.putString("role", role.name());
        tag.putBoolean("campActive", campActive);
        tag.putBoolean("connected", connected);
        tag.putBoolean("destroyed", destroyed);
        tag.putInt("installedModules", installedModules);
        tag.putLong("updatedAt", updatedAt);
        return tag;
    }

    static SettlementRadioStatus load(CompoundTag tag) {
        return new SettlementRadioStatus(
                tag.getUUID("campId"),
                BlockPos.of(tag.getLong("position")),
                tag.contains("campCenter")
                        ? BlockPos.of(tag.getLong("campCenter"))
                        : BlockPos.of(tag.getLong("position")),
                Math.max(0, tag.getInt("campRadius")),
                parseRole(tag.getString("role")),
                tag.getBoolean("campActive"),
                tag.getBoolean("connected"),
                tag.getBoolean("destroyed"),
                tag.getInt("installedModules"),
                tag.getLong("updatedAt")
        );
    }

    boolean contributesStockpile() {
        return campActive && !destroyed && campRadius > 0;
    }

    private static SettlementRadioRole parseRole(String serialized) {
        try {
            return SettlementRadioRole.valueOf(serialized);
        } catch (IllegalArgumentException exception) {
            return SettlementRadioRole.RELAY;
        }
    }
}
