package io.github.gev414.rotwire.sleep;

import io.github.gev414.rotwire.block.TarpBlock;
import io.github.gev414.rotwire.config.SurvivalSystemsConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;

import javax.annotation.Nullable;

import java.util.Optional;

/**
 * Server-authoritative view of a campsite. This is deliberately independent
 * of the radio UI so future camp modules can reuse the same validation.
 */
public final class CampInspector {

    private static final boolean TRAVELERS_BACKPACK_LOADED =
            ModList.get().isLoaded("travelersbackpack");

    public static CampStatus inspectRadio(
            ServerLevel level,
            ServerPlayer player,
            BlockPos radioPosition
    ) {
        return inspectRadio(level, (Player) player, radioPosition);
    }

    /**
     * Radio persistence ticks need the same camp readiness check even when no
     * player currently has the hub open. Food components support a null player
     * context, so this remains a world-authoritative inspection.
     */
    public static CampStatus inspectRadio(
            ServerLevel level,
            BlockPos radioPosition
    ) {
        return inspectRadio(level, (Player) null, radioPosition);
    }

    private static CampStatus inspectRadio(
            ServerLevel level,
            @Nullable Player player,
            BlockPos radioPosition
    ) {
        int configuredRadius =
                SurvivalSystemsConfig.SLEEP_CAMPSITE_RADIUS.get();
        Optional<ShelterArea> shelter = findShelter(
                level,
                radioPosition
        );
        if (shelter.isEmpty()) {
            return CampStatus.inactive(
                    radioPosition,
                    configuredRadius
            );
        }

        ShelterArea area = shelter.get();
        boolean sleepingBagPresent = TRAVELERS_BACKPACK_LOADED
                && hasShelteredSleepingBag(level, area);
        boolean litCampfirePresent = hasLitCampfire(
                level,
                area.center(),
                area.radius()
        );
        CampContainerSupplies.CampSupplyStatus supplies =
                CampContainerSupplies.inspectCampSupplies(
                        level,
                        player,
                        area.center(),
                        area.radius(),
                        SurvivalSystemsConfig
                                .SLEEP_CAMPSITE_FOOD_NUTRITION_THRESHOLD
                                .get()
                );

        return new CampStatus(
                area.type(),
                area.center(),
                area.radius(),
                sleepingBagPresent,
                litCampfirePresent,
                supplies.containerPresent(),
                supplies.rationReady(),
                supplies.availableNutrition()
        );
    }

    public static boolean isSheltered(
            ServerLevel level,
            BlockPos position
    ) {
        return findShelter(level, position).isPresent();
    }

    static Optional<ShelterArea> findShelter(
            ServerLevel level,
            BlockPos interiorPosition
    ) {
        int configuredRadius =
                SurvivalSystemsConfig.SLEEP_CAMPSITE_RADIUS.get();
        BlockPos tarpPosition = interiorPosition.above();
        BlockState tarpState = level.getBlockState(tarpPosition);
        if (tarpState.getBlock() instanceof TarpBlock
                && TarpBlock.isComplete(
                        level,
                        tarpPosition,
                        tarpState
                )) {
            BlockPos anchor = TarpBlock.anchorPosition(
                    tarpPosition,
                    tarpState
            );
            return Optional.of(new ShelterArea(
                    CampStatus.ShelterType.TARP,
                    anchor.immutable(),
                    anchor.below(),
                    configuredRadius
            ));
        }

        return SimplyTentsCampsiteIntegration.findShelter(
                level,
                interiorPosition
        ).map(shelter -> new ShelterArea(
                shelter.type(),
                shelter.anchor(),
                shelter.center(),
                shelter.effectiveRadius(configuredRadius)
        ));
    }

    static boolean hasLitCampfire(
            ServerLevel level,
            BlockPos origin,
            int radius
    ) {
        BlockPos minimum = origin.offset(-radius, -radius, -radius);
        BlockPos maximum = origin.offset(radius, radius, radius);
        double maximumDistanceSquared = (double) radius * radius;

        for (BlockPos position : BlockPos.betweenClosed(minimum, maximum)) {
            if (origin.distSqr(position) > maximumDistanceSquared) {
                continue;
            }
            BlockState state = level.getBlockState(position);
            if (state.getBlock() instanceof CampfireBlock
                    && state.getValue(CampfireBlock.LIT)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasShelteredSleepingBag(
            ServerLevel level,
            ShelterArea campShelter
    ) {
        BlockPos origin = campShelter.center();
        int radius = campShelter.radius();
        BlockPos minimum = origin.offset(-radius, -radius, -radius);
        BlockPos maximum = origin.offset(radius, radius, radius);
        double maximumDistanceSquared = (double) radius * radius;

        for (BlockPos mutable : BlockPos.betweenClosed(minimum, maximum)) {
            if (origin.distSqr(mutable) > maximumDistanceSquared
                    || !TravelersBackpackSleepIntegration.isSleepingBag(
                            level.getBlockState(mutable)
                    )) {
                continue;
            }
            Optional<ShelterArea> sleepingBagShelter =
                    findShelter(level, mutable);
            if (sleepingBagShelter.isPresent()
                    && campShelter.sameStructure(
                            sleepingBagShelter.get()
                    )) {
                return true;
            }
        }
        return false;
    }

    record ShelterArea(
            CampStatus.ShelterType type,
            BlockPos anchor,
            BlockPos center,
            int radius
    ) {

        boolean contains(BlockPos position) {
            return center.distSqr(position)
                    <= (double) radius * radius;
        }

        boolean sameStructure(ShelterArea other) {
            return type == other.type && anchor.equals(other.anchor);
        }
    }

    private CampInspector() {
    }
}
