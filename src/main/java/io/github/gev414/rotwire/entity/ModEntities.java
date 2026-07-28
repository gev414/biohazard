    package io.github.gev414.rotwire.entity;

    import io.github.gev414.rotwire.Rotwire;
    import net.minecraft.core.registries.Registries;
    import net.minecraft.world.entity.EntityType;
    import net.minecraft.world.entity.MobCategory;
    import net.neoforged.neoforge.registries.DeferredRegister;
    import io.github.gev414.rotwire.entity.projectile.BruteRockProjectile;

    import java.util.function.Supplier;

    public final class ModEntities {

        public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
                DeferredRegister.create(
                        Registries.ENTITY_TYPE,
                        Rotwire.MOD_ID
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
                                .build(Rotwire.MOD_ID + ":brute")
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
                                .build(Rotwire.MOD_ID + ":brute_rock")
                );

        private ModEntities() {
        }
    }