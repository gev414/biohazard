    package io.github.gev414.biohazard.entity;

    import io.github.gev414.biohazard.Biohazard;
    import net.minecraft.core.registries.Registries;
    import net.minecraft.world.entity.EntityType;
    import net.minecraft.world.entity.MobCategory;
    import net.neoforged.neoforge.registries.DeferredRegister;
    import io.github.gev414.biohazard.entity.projectile.BruteRockProjectile;

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
                                .sized(
                                        BruteEntity.WIDTH,
                                        BruteEntity.HEIGHT
                                )
                                .eyeHeight(BruteEntity.EYE_HEIGHT)
                                .clientTrackingRange(8)
                                .build(Biohazard.MOD_ID + ":brute")
                );

        public static final Supplier<EntityType<BruteRockProjectile>> BRUTE_ROCK =
                ENTITY_TYPES.register(
                        "brute_rock",
                        () -> EntityType.Builder.<BruteRockProjectile>of(                                        BruteRockProjectile::new,
                                        MobCategory.MISC
                                )
                                .sized(0.25F, 0.25F)
                                .clientTrackingRange(4)
                                .updateInterval(10)
                                .build(Biohazard.MOD_ID + ":brute_rock")
                );

        private ModEntities() {
        }
    }