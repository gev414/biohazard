package io.github.gev414.biohazard.damage;

import io.github.gev414.biohazard.Biohazard;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;

public final class ModDamageTypes {

    public static final ResourceKey<DamageType> BRUTE_ROCK_SPLASH =
            ResourceKey.create(
                    Registries.DAMAGE_TYPE,
                    ResourceLocation.fromNamespaceAndPath(
                            Biohazard.MOD_ID,
                            "brute_rock_splash"
                    )
            );

    private ModDamageTypes() {
    }
}