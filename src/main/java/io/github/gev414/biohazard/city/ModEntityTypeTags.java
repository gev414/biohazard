package io.github.gev414.biohazard.city;

import io.github.gev414.biohazard.Biohazard;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

public final class ModEntityTypeTags {

    public static final TagKey<EntityType<?>> CITY_SCALED_INFECTED =
            TagKey.create(
                    Registries.ENTITY_TYPE,
                    ResourceLocation.fromNamespaceAndPath(
                            Biohazard.MOD_ID,
                            "city_scaled_infected"
                    )
            );

    public static final TagKey<EntityType<?>> STEALTH_AFFECTED_INFECTED =
            TagKey.create(
                    Registries.ENTITY_TYPE,
                    ResourceLocation.fromNamespaceAndPath(
                            Biohazard.MOD_ID,
                            "stealth_affected_infected"
                    )
            );

    private ModEntityTypeTags() {
    }
}
