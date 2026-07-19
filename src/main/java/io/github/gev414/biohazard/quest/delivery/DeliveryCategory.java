package io.github.gev414.biohazard.quest.delivery;

import io.github.gev414.biohazard.config.RadioQuestConfig;

import java.util.Locale;
import java.util.Set;

public enum DeliveryCategory {
    SUPPLIES,
    AMMUNITION,
    MEDICAL,
    EQUIPMENT,
    FIREARM;

    private static final String TAG_PREFIX = "biohazard_category_";

    public int delayTicks() {
        int seconds = switch (this) {
            case SUPPLIES -> RadioQuestConfig.SUPPLIES_DELAY_SECONDS.get();
            case AMMUNITION -> RadioQuestConfig.AMMUNITION_DELAY_SECONDS.get();
            case MEDICAL -> RadioQuestConfig.MEDICAL_DELAY_SECONDS.get();
            case EQUIPMENT -> RadioQuestConfig.EQUIPMENT_DELAY_SECONDS.get();
            case FIREARM -> RadioQuestConfig.FIREARM_DELAY_SECONDS.get();
        };
        return Math.multiplyExact(seconds, 20);
    }

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static DeliveryCategory fromTags(Set<String> tags) {
        for (DeliveryCategory category : values()) {
            if (tags.contains(TAG_PREFIX + category.serializedName())) {
                return category;
            }
        }
        return SUPPLIES;
    }

    public static DeliveryCategory fromName(String name) {
        for (DeliveryCategory category : values()) {
            if (category.serializedName().equals(name)) {
                return category;
            }
        }
        return SUPPLIES;
    }
}
