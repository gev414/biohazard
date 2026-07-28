package io.github.gev414.rotwire.quest.delivery;

enum DeliveryKind {
    ITEMS,
    CHOICE;

    static DeliveryKind fromName(String name) {
        for (DeliveryKind kind : values()) {
            if (kind.name().equalsIgnoreCase(name)) {
                return kind;
            }
        }
        return ITEMS;
    }
}
