package io.github.gev414.rotwire.quest.delivery;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DeliveryCategoryTest {

    @Test
    void categoryTagsMapToTheirCourierClass() {
        for (DeliveryCategory category : DeliveryCategory.values()) {
            assertEquals(category, DeliveryCategory.fromTags(Set.of(
                    "rotwire_category_" + category.serializedName()
            )));
        }
    }

    @Test
    void missingOrUnknownCategoriesSafelyUseSupplies() {
        assertEquals(
                DeliveryCategory.SUPPLIES,
                DeliveryCategory.fromTags(Set.of())
        );
        assertEquals(
                DeliveryCategory.SUPPLIES,
                DeliveryCategory.fromName("future_category")
        );
    }

    @Test
    void serializedNamesRoundTrip() {
        for (DeliveryCategory category : DeliveryCategory.values()) {
            assertEquals(
                    category,
                    DeliveryCategory.fromName(category.serializedName())
            );
        }
    }
}
