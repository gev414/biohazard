package io.github.gev414.rotwire.camp;

public enum CampModuleType {
    STORAGE,
    CRAFTING,
    OPERATIONS;

    public int mask() {
        return 1 << ordinal();
    }

    public static int sanitizeMask(int mask) {
        int valid = 0;
        for (CampModuleType type : values()) {
            valid |= type.mask();
        }
        return mask & valid;
    }
}
