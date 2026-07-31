package io.github.gev414.rotwire.menu;

import io.github.gev414.rotwire.block.ModBlocks;
import io.github.gev414.rotwire.block.entity.RadioTransmitterBlockEntity;
import io.github.gev414.rotwire.quest.RadioNetwork;
import io.github.gev414.rotwire.quest.RadioServices;
import io.github.gev414.rotwire.sleep.CampInspector;
import io.github.gev414.rotwire.sleep.CampStatus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class CampRadioMenu extends AbstractContainerMenu {

    public static final int CONTRACTS_BUTTON = 0;

    private static final int SHELTER_DATA = 0;
    private static final int RADIUS_DATA = 1;
    private static final int FLAGS_DATA = 2;
    private static final int NUTRITION_DATA = 3;
    private static final int CONNECTION_SECONDS_DATA = 4;
    private static final int DATA_COUNT = 5;

    private static final int ESTABLISHED_FLAG = 1;
    private static final int SLEEPING_BAG_FLAG = 1 << 1;
    private static final int CAMPFIRE_FLAG = 1 << 2;
    private static final int BACKPACK_FLAG = 1 << 3;
    private static final int RATION_FLAG = 1 << 4;
    private static final int CONNECTED_FLAG = 1 << 5;
    private static final int ACTIVE_FLAG = 1 << 6;

    private final BlockPos radioPosition;
    private final Inventory playerInventory;
    private final ContainerData data;
    private long lastRefresh = Long.MIN_VALUE;

    public CampRadioMenu(
            int containerId,
            Inventory playerInventory,
            RegistryFriendlyByteBuf buffer
    ) {
        this(
                containerId,
                playerInventory,
                buffer.readBlockPos(),
                new SimpleContainerData(DATA_COUNT)
        );
    }

    public CampRadioMenu(
            int containerId,
            Inventory playerInventory,
            RadioTransmitterBlockEntity radio
    ) {
        this(
                containerId,
                playerInventory,
                radio.getBlockPos(),
                new SimpleContainerData(DATA_COUNT)
        );
        refreshServerData();
    }

    private CampRadioMenu(
            int containerId,
            Inventory playerInventory,
            BlockPos radioPosition,
            ContainerData data
    ) {
        super(ModMenus.CAMP_RADIO.get(), containerId);
        this.playerInventory = playerInventory;
        this.radioPosition = radioPosition.immutable();
        this.data = data;
        checkContainerDataCount(data, DATA_COUNT);
        addDataSlots(data);
    }

    @Override
    public void broadcastChanges() {
        refreshServerData();
        super.broadcastChanges();
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockState(radioPosition).is(
                ModBlocks.RADIO_TRANSMITTER.get()
        ) && player.distanceToSqr(
                radioPosition.getX() + 0.5D,
                radioPosition.getY() + 0.5D,
                radioPosition.getZ() + 0.5D
        ) <= 64.0D;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != CONTRACTS_BUTTON
                || !(player instanceof ServerPlayer serverPlayer)
                || !stillValid(player)) {
            return false;
        }
        return RadioServices.openNetwork(
                serverPlayer,
                radioPosition
        );
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    public BlockPos radioPosition() {
        return radioPosition;
    }

    public CampStatus.ShelterType shelter() {
        return CampStatus.ShelterType.fromNetworkId(
                data.get(SHELTER_DATA)
        );
    }

    public int radius() {
        return data.get(RADIUS_DATA);
    }

    public int availableNutrition() {
        return data.get(NUTRITION_DATA);
    }

    public int connectionSeconds() {
        return data.get(CONNECTION_SECONDS_DATA);
    }

    public boolean established() {
        return flag(ESTABLISHED_FLAG);
    }

    public boolean sleepingBagPresent() {
        return flag(SLEEPING_BAG_FLAG);
    }

    public boolean campfirePresent() {
        return flag(CAMPFIRE_FLAG);
    }

    public boolean backpackPresent() {
        return flag(BACKPACK_FLAG);
    }

    public boolean rationReady() {
        return flag(RATION_FLAG);
    }

    public boolean connected() {
        return flag(CONNECTED_FLAG);
    }

    public boolean active() {
        return flag(ACTIVE_FLAG);
    }

    private boolean flag(int flag) {
        return (data.get(FLAGS_DATA) & flag) != 0;
    }

    private void refreshServerData() {
        if (!(playerInventory.player instanceof ServerPlayer player)
                || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        long gameTime = level.getGameTime();
        if (lastRefresh != Long.MIN_VALUE
                && gameTime - lastRefresh < 20L) {
            return;
        }
        lastRefresh = gameTime;

        BlockEntity blockEntity = level.getBlockEntity(radioPosition);
        boolean established =
                blockEntity instanceof RadioTransmitterBlockEntity radio
                        && radio.hasCampIdentity();
        CampStatus status = CampInspector.inspectRadio(
                level,
                player,
                radioPosition
        );
        boolean connected = RadioNetwork.isConnected(
                level,
                radioPosition
        );

        int flags = established ? ESTABLISHED_FLAG : 0;
        flags |= status.sleepingBagPresent() ? SLEEPING_BAG_FLAG : 0;
        flags |= status.litCampfirePresent() ? CAMPFIRE_FLAG : 0;
        flags |= status.backpackPresent() ? BACKPACK_FLAG : 0;
        flags |= status.rationReady() ? RATION_FLAG : 0;
        flags |= connected ? CONNECTED_FLAG : 0;
        flags |= status.active() ? ACTIVE_FLAG : 0;

        data.set(SHELTER_DATA, status.shelter().ordinal());
        data.set(RADIUS_DATA, status.radius());
        data.set(FLAGS_DATA, flags);
        data.set(NUTRITION_DATA, status.availableNutrition());
        data.set(
                CONNECTION_SECONDS_DATA,
                connected
                        ? 0
                        : (int) Math.min(
                                Integer.MAX_VALUE,
                                RadioNetwork.calibrationSecondsRemaining(
                                        level,
                                        radioPosition
                                )
                        )
        );
    }
}
