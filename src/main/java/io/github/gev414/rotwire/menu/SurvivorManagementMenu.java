package io.github.gev414.rotwire.menu;

import io.github.gev414.rotwire.block.ModBlocks;
import io.github.gev414.rotwire.block.entity.RadioTransmitterBlockEntity;
import io.github.gev414.rotwire.config.SettlementConfig;
import io.github.gev414.rotwire.entity.SurvivorEntity;
import io.github.gev414.rotwire.settlement.SettlementNameRules;
import io.github.gev414.rotwire.settlement.SettlementSnapshot;
import io.github.gev414.rotwire.settlement.SettlementUpgrade;
import io.github.gev414.rotwire.sleep.CampInspector;
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

/**
 * Synced settlement-survivor dashboard opened from the primary camp radio.
 * It has no inventory slots; its buttons dispatch validated server actions.
 */
public final class SurvivorManagementMenu extends AbstractContainerMenu {

    public static final int CALL_CIVILIAN_BUTTON = 0;
    public static final int CALL_RIFLEMAN_BUTTON = 1;
    public static final int RALLY_SURVIVORS_BUTTON = 2;
    public static final int BACK_BUTTON = 3;

    private static final int FLAGS_DATA = 0;
    private static final int POPULATION_DATA = 1;
    private static final int CIVILIANS_DATA = 2;
    private static final int RIFLEMEN_DATA = 3;
    private static final int RATIONS_DATA = 4;
    private static final int MOSIN_AMMUNITION_DATA = 5;
    private static final int CIVILIAN_RATIONS_REQUIRED_DATA = 6;
    private static final int RIFLEMAN_RATIONS_REQUIRED_DATA = 7;
    private static final int RIFLEMAN_AMMUNITION_REQUIRED_DATA = 8;
    private static final int MAX_CIVILIANS_DATA = 9;
    private static final int MAX_RIFLEMEN_DATA = 10;
    private static final int DATA_COUNT = 11;

    private static final int SETTLEMENT_PRESENT_FLAG = 1;
    private static final int PRIMARY_RADIO_FLAG = 1 << 1;
    private static final int CAMP_HUB_FLAG = 1 << 2;
    private static final int OWNER_FLAG = 1 << 3;
    private static final int ONLINE_FLAG = 1 << 4;
    private static final int ACTIVE_CAMP_FLAG = 1 << 5;

    private final BlockPos radioPosition;
    private final String settlementName;
    private final Inventory playerInventory;
    private final ContainerData data;
    private long lastRefresh = Long.MIN_VALUE;

    public SurvivorManagementMenu(
            int containerId,
            Inventory playerInventory,
            RegistryFriendlyByteBuf buffer
    ) {
        this(
                containerId,
                playerInventory,
                buffer.readBlockPos(),
                buffer.readUtf(SettlementNameRules.MAX_LENGTH),
                new SimpleContainerData(DATA_COUNT)
        );
    }

    public SurvivorManagementMenu(
            int containerId,
            Inventory playerInventory,
            RadioTransmitterBlockEntity radio,
            String settlementName
    ) {
        this(
                containerId,
                playerInventory,
                radio.getBlockPos(),
                settlementName,
                new SimpleContainerData(DATA_COUNT)
        );
        refreshServerData();
    }

    private SurvivorManagementMenu(
            int containerId,
            Inventory playerInventory,
            BlockPos radioPosition,
            String settlementName,
            ContainerData data
    ) {
        super(ModMenus.SURVIVOR_MANAGEMENT.get(), containerId);
        this.playerInventory = playerInventory;
        this.radioPosition = radioPosition.immutable();
        this.settlementName = settlementName;
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
        if (!(player instanceof ServerPlayer serverPlayer)
                || !stillValid(player)
                || !(player.level().getBlockEntity(radioPosition)
                instanceof RadioTransmitterBlockEntity radio)) {
            return false;
        }
        return switch (id) {
            case CALL_CIVILIAN_BUTTON -> radio.callCivilian(serverPlayer);
            case CALL_RIFLEMAN_BUTTON -> radio.callRifleman(serverPlayer);
            case RALLY_SURVIVORS_BUTTON -> radio.rallySurvivors(serverPlayer);
            case BACK_BUTTON -> {
                radio.openCampHub(serverPlayer);
                yield true;
            }
            default -> false;
        };
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    public String settlementName() {
        return settlementName;
    }

    public int population() {
        return data.get(POPULATION_DATA);
    }

    public int civilians() {
        return data.get(CIVILIANS_DATA);
    }

    public int riflemen() {
        return data.get(RIFLEMEN_DATA);
    }

    public int rations() {
        return data.get(RATIONS_DATA);
    }

    public int mosinAmmunition() {
        return data.get(MOSIN_AMMUNITION_DATA);
    }

    public int civilianRationsRequired() {
        return data.get(CIVILIAN_RATIONS_REQUIRED_DATA);
    }

    public int riflemanRationsRequired() {
        return data.get(RIFLEMAN_RATIONS_REQUIRED_DATA);
    }

    public int riflemanAmmunitionRequired() {
        return data.get(RIFLEMAN_AMMUNITION_REQUIRED_DATA);
    }

    public int maximumCivilians() {
        return data.get(MAX_CIVILIANS_DATA);
    }

    public int maximumRiflemen() {
        return data.get(MAX_RIFLEMEN_DATA);
    }

    public boolean canManageSurvivors() {
        return flag(SETTLEMENT_PRESENT_FLAG)
                && flag(PRIMARY_RADIO_FLAG)
                && flag(CAMP_HUB_FLAG)
                && flag(OWNER_FLAG)
                && flag(ONLINE_FLAG)
                && flag(ACTIVE_CAMP_FLAG);
    }

    private boolean flag(int mask) {
        return (data.get(FLAGS_DATA) & mask) != 0;
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
        RadioTransmitterBlockEntity radio =
                blockEntity instanceof RadioTransmitterBlockEntity value
                        ? value
                        : null;
        if (radio != null) {
            radio.refreshSettlement(level);
        }
        SettlementSnapshot settlement = radio == null
                ? null
                : radio.settlement(level).orElse(null);

        int flags = settlement == null ? 0 : SETTLEMENT_PRESENT_FLAG;
        if (settlement != null && radio != null) {
            flags |= radio.isPrimarySettlementRadio(level)
                    ? PRIMARY_RADIO_FLAG
                    : 0;
            flags |= settlement.hasUpgrade(SettlementUpgrade.CAMP_HUB)
                    ? CAMP_HUB_FLAG
                    : 0;
            flags |= radio.canManage(player) ? OWNER_FLAG : 0;
            flags |= radio.isConnected(gameTime) ? ONLINE_FLAG : 0;
            flags |= CampInspector.inspectRadio(
                    level,
                    player,
                    radioPosition
            ).active() ? ACTIVE_CAMP_FLAG : 0;
        }
        data.set(FLAGS_DATA, flags);
        data.set(POPULATION_DATA, settlement == null
                ? 0
                : settlement.population());
        data.set(CIVILIANS_DATA, settlement == null
                ? 0
                : settlement.civilianPopulation());
        data.set(RIFLEMEN_DATA, settlement == null
                ? 0
                : settlement.guardPopulation());
        data.set(RATIONS_DATA, settlement == null
                ? 0
                : settlement.rations());
        data.set(MOSIN_AMMUNITION_DATA, settlement == null
                ? 0
                : settlement.mosinAmmunition());
        data.set(
                CIVILIAN_RATIONS_REQUIRED_DATA,
                SettlementConfig.CIVILIAN_CALL_RATION_REQUIREMENT.get()
        );
        data.set(
                RIFLEMAN_RATIONS_REQUIRED_DATA,
                SettlementConfig.RIFLEMAN_CALL_RATION_REQUIREMENT.get()
        );
        data.set(
                RIFLEMAN_AMMUNITION_REQUIRED_DATA,
                Math.max(
                        SurvivorEntity.mosinMagazineCapacity(),
                        SettlementConfig
                                .RIFLEMAN_CALL_AMMUNITION_REQUIREMENT
                                .get()
                )
        );
        data.set(
                MAX_CIVILIANS_DATA,
                SettlementConfig.MAX_CIVILIAN_SURVIVORS.get()
        );
        data.set(
                MAX_RIFLEMEN_DATA,
                SettlementConfig.MAX_RIFLEMEN.get()
        );
    }
}
