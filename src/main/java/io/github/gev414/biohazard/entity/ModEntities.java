package io.github.gev414.biohazard.entity;

import io.github.gev414.biohazard.Biohazard;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(
                    Registries.ENTITY_TYPE,
                    Biohazard.MOD_ID
            );

    public static final Supplier<EntityType<BruteEntity>> BRUTE =
            ENTITY_TYPES.register(
                    "brute",
                    () -> EntityType.Builder.of(
                                    BruteEntity::new,
                                    MobCategory.MONSTER
                            )
                            .sized(0.6F, 1.95F)
                            .clientTrackingRange(8)
                            .build(Biohazard.MOD_ID + ":brute")
            );

    private ModEntities() {
    }
}