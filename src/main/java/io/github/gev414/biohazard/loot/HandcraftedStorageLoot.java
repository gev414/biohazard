package io.github.gev414.biohazard.loot;

import io.github.gev414.biohazard.Biohazard;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.Set;

public final class HandcraftedStorageLoot {

    private static final String STOCKED_KEY =
            "biohazard_handcrafted_storage_stocked";
    private static final ResourceKey<LootTable> LOOT_TABLE = ResourceKey.create(
            Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(
                    Biohazard.MOD_ID,
                    "chests/handcrafted_storage"
            )
    );
    private static final Set<String> STORAGE_BLOCKS = Set.of(
            "handcrafted:oak_cupboard",
            "handcrafted:dark_oak_cupboard",
            "handcrafted:spruce_cupboard",
            "handcrafted:oak_drawer",
            "handcrafted:dark_oak_drawer",
            "handcrafted:spruce_drawer",
            "handcrafted:oak_shelf",
            "handcrafted:oak_nightstand",
            "handcrafted:oak_desk"
    );

    public static boolean tryStock(
            ServerLevel level,
            PlayerInteractEvent.RightClickBlock event
    ) {
        BlockEntity blockEntity = level.getBlockEntity(event.getPos());
        if (!isStorage(blockEntity)) {
            return false;
        }
        if (!(blockEntity instanceof Container container)
                || blockEntity.getPersistentData().getBoolean(STOCKED_KEY)) {
            return true;
        }

        LootTable lootTable = level.getServer().reloadableRegistries()
                .getLootTable(LOOT_TABLE);
        LootParams lootParams = new LootParams.Builder(level)
                .withParameter(
                        LootContextParams.ORIGIN,
                        Vec3.atCenterOf(event.getPos())
                )
                .withParameter(LootContextParams.THIS_ENTITY, event.getEntity())
                .withLuck(event.getEntity().getLuck())
                .create(LootContextParamSets.CHEST);

        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            CriteriaTriggers.GENERATE_LOOT.trigger(serverPlayer, LOOT_TABLE);
        }

        lootTable.fill(container, lootParams, level.random.nextLong());
        blockEntity.getPersistentData().putBoolean(STOCKED_KEY, true);
        blockEntity.setChanged();
        return true;
    }

    private static boolean isStorage(BlockEntity blockEntity) {
        if (blockEntity == null) {
            return false;
        }
        String id = BuiltInRegistries.BLOCK.getKey(
                blockEntity.getBlockState().getBlock()
        ).toString();
        return STORAGE_BLOCKS.contains(id);
    }

    private HandcraftedStorageLoot() {
    }
}
