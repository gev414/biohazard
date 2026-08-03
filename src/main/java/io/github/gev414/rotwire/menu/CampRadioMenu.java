package io.github.gev414.rotwire.menu;

import io.github.gev414.rotwire.block.ModBlocks;
import io.github.gev414.rotwire.block.entity.RadioTransmitterBlockEntity;
import io.github.gev414.rotwire.camp.CampModuleType;
import io.github.gev414.rotwire.city.CityZoneManager;
import io.github.gev414.rotwire.config.SettlementConfig;
import io.github.gev414.rotwire.quest.RadioNetwork;
import io.github.gev414.rotwire.quest.RadioServices;
import io.github.gev414.rotwire.quest.delivery.DeliveryManager;
import io.github.gev414.rotwire.settlement.SettlementNameRules;
import io.github.gev414.rotwire.settlement.SettlementRationRules;
import io.github.gev414.rotwire.settlement.SettlementSnapshot;
import io.github.gev414.rotwire.settlement.SettlementUpgrade;
import io.github.gev414.rotwire.sleep.CampInspector;
import io.github.gev414.rotwire.sleep.CampStatus;
import io.github.gev414.rotwire.weather.ScheduledWeather;
import io.github.gev414.rotwire.weather.WeatherManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

public final class CampRadioMenu extends AbstractContainerMenu {

    public static final int CONTRACTS_BUTTON = 0;
    public static final int STORAGE_BUTTON = 1;
    public static final int WORKSHOP_BUTTON = 2;
    public static final int SURVIVORS_BUTTON = 3;
    private static final int SHELTER_DATA = 0;
    private static final int RADIUS_DATA = 1;
    private static final int FLAGS_DATA = 2;
    private static final int NUTRITION_DATA = 3;
    private static final int CONNECTION_SECONDS_DATA = 4;
    private static final int MODULES_DATA = 5;
    private static final int HOSTILES_DATA = 6;
    private static final int WEATHER_DATA = 7;
    private static final int CITY_DANGER_DATA = 8;
    private static final int DELIVERY_READY_DATA = 9;
    private static final int DELIVERY_PENDING_DATA = 10;
    private static final int SETTLEMENT_FLAGS_DATA = 11;
    private static final int SETTLEMENT_POPULATION_DATA = 12;
    private static final int SETTLEMENT_RATIONS_DATA = 13;
    private static final int SETTLEMENT_ACTIVE_RADIOS_DATA = 14;
    private static final int SETTLEMENT_RADIOS_DATA = 15;
    private static final int SETTLEMENT_UPGRADES_DATA = 16;
    private static final int SETTLEMENT_RATION_CONTAINERS_DATA = 17;
    private static final int SETTLEMENT_DAILY_RATIONS_DATA = 18;
    private static final int SETTLEMENT_CIVILIANS_DATA = 19;
    private static final int SETTLEMENT_RIFLEMEN_DATA = 20;
    private static final int SETTLEMENT_MOSIN_AMMUNITION_DATA = 21;
    private static final int DATA_COUNT = 22;

    private static final int ESTABLISHED_FLAG = 1;
    private static final int SLEEPING_BAG_FLAG = 1 << 1;
    private static final int CAMPFIRE_FLAG = 1 << 2;
    private static final int CONTAINER_FLAG = 1 << 3;
    private static final int RATION_FLAG = 1 << 4;
    private static final int CONNECTED_FLAG = 1 << 5;
    private static final int ACTIVE_FLAG = 1 << 6;
    private static final int OWNER_FLAG = 1 << 7;
    private static final int OPERATIONS_ACTIVE_FLAG = 1 << 8;
    private static final int SETTLEMENT_PRESENT_FLAG = 1;
    private static final int PRIMARY_RADIO_FLAG = 1 << 1;
    private static final int NAME_REQUIRED_FLAG = 1 << 2;
    private static final int CAMP_HUB_FLAG = 1 << 3;

    private final BlockPos radioPosition;
    private final String settlementName;
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
                buffer.readUtf(SettlementNameRules.MAX_LENGTH),
                new SimpleContainerData(DATA_COUNT)
        );
    }

    public CampRadioMenu(
            int containerId,
            Inventory playerInventory,
            RadioTransmitterBlockEntity radio
    ) {
        this(containerId, playerInventory, radio, "");
    }

    public CampRadioMenu(
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

    private CampRadioMenu(
            int containerId,
            Inventory playerInventory,
            BlockPos radioPosition,
            String settlementName,
            ContainerData data
    ) {
        super(ModMenus.CAMP_RADIO.get(), containerId);
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
            case CONTRACTS_BUTTON -> RadioServices.openNetwork(
                    serverPlayer,
                    radioPosition
            );
            case STORAGE_BUTTON -> radio.openStorage(serverPlayer);
            case WORKSHOP_BUTTON -> radio.repairHeldItem(serverPlayer);
            case SURVIVORS_BUTTON -> {
                radio.openSurvivorManagement(serverPlayer);
                yield true;
            }
            default -> false;
        };
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    public BlockPos radioPosition() {
        return radioPosition;
    }

    public String settlementName() {
        return settlementName;
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

    public boolean containerPresent() {
        return flag(CONTAINER_FLAG);
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

    public boolean owner() {
        return flag(OWNER_FLAG);
    }

    public boolean operationsActive() {
        return flag(OPERATIONS_ACTIVE_FLAG);
    }

    public boolean hasModule(CampModuleType type) {
        return (data.get(MODULES_DATA) & type.mask()) != 0;
    }

    public int nearbyHostiles() {
        return data.get(HOSTILES_DATA);
    }

    public ScheduledWeather weather() {
        return ScheduledWeather.fromNetwork(data.get(WEATHER_DATA));
    }

    public int cityDanger() {
        return data.get(CITY_DANGER_DATA);
    }

    public int readyDeliveries() {
        return data.get(DELIVERY_READY_DATA);
    }

    public int pendingDeliveries() {
        return data.get(DELIVERY_PENDING_DATA);
    }

    public boolean hasSettlement() {
        return settlementFlag(SETTLEMENT_PRESENT_FLAG);
    }

    public boolean primarySettlementRadio() {
        return settlementFlag(PRIMARY_RADIO_FLAG);
    }

    public boolean needsSettlementName() {
        return settlementFlag(NAME_REQUIRED_FLAG);
    }

    public int settlementPopulation() {
        return data.get(SETTLEMENT_POPULATION_DATA);
    }

    public int settlementRations() {
        return data.get(SETTLEMENT_RATIONS_DATA);
    }

    public int activeSettlementRadios() {
        return data.get(SETTLEMENT_ACTIVE_RADIOS_DATA);
    }

    public int settlementRadioCount() {
        return data.get(SETTLEMENT_RADIOS_DATA);
    }

    public int settlementUpgradeMask() {
        return data.get(SETTLEMENT_UPGRADES_DATA);
    }

    public boolean campHubInstalled() {
        return settlementFlag(CAMP_HUB_FLAG);
    }

    public int settlementRationContainers() {
        return data.get(SETTLEMENT_RATION_CONTAINERS_DATA);
    }

    public int settlementDailyRations() {
        return data.get(SETTLEMENT_DAILY_RATIONS_DATA);
    }

    public int civilianPopulation() {
        return data.get(SETTLEMENT_CIVILIANS_DATA);
    }

    public int riflemanPopulation() {
        return data.get(SETTLEMENT_RIFLEMEN_DATA);
    }

    public int mosinAmmunition() {
        return data.get(SETTLEMENT_MOSIN_AMMUNITION_DATA);
    }

    private boolean flag(int flag) {
        return (data.get(FLAGS_DATA) & flag) != 0;
    }

    private boolean settlementFlag(int flag) {
        return (data.get(SETTLEMENT_FLAGS_DATA) & flag) != 0;
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
        boolean established = radio != null && radio.hasCampIdentity();
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
        flags |= status.containerPresent() ? CONTAINER_FLAG : 0;
        flags |= status.rationReady() ? RATION_FLAG : 0;
        flags |= connected ? CONNECTED_FLAG : 0;
        flags |= status.active() ? ACTIVE_FLAG : 0;
        flags |= radio != null && radio.canManage(player) ? OWNER_FLAG : 0;

        int modules = radio == null ? 0 : radio.installedModuleMask();
        boolean operationsActive = radio != null
                && radio.hasModule(CampModuleType.OPERATIONS)
                && status.active()
                && connected;
        flags |= operationsActive ? OPERATIONS_ACTIVE_FLAG : 0;

        data.set(SHELTER_DATA, status.shelter().ordinal());
        data.set(RADIUS_DATA, status.radius());
        data.set(FLAGS_DATA, flags);
        data.set(NUTRITION_DATA, status.availableNutrition());
        data.set(MODULES_DATA, modules);
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

        SettlementSnapshot settlement = radio == null
                ? null
                : radio.settlement(level).orElse(null);
        int settlementFlags = 0;
        if (settlement == null) {
            data.set(SETTLEMENT_POPULATION_DATA, 0);
            data.set(SETTLEMENT_RATIONS_DATA, 0);
            data.set(SETTLEMENT_ACTIVE_RADIOS_DATA, 0);
            data.set(SETTLEMENT_RADIOS_DATA, 0);
            data.set(SETTLEMENT_UPGRADES_DATA, 0);
            data.set(SETTLEMENT_RATION_CONTAINERS_DATA, 0);
            data.set(SETTLEMENT_DAILY_RATIONS_DATA, 0);
            data.set(SETTLEMENT_CIVILIANS_DATA, 0);
            data.set(SETTLEMENT_RIFLEMEN_DATA, 0);
            data.set(SETTLEMENT_MOSIN_AMMUNITION_DATA, 0);
        } else {
            settlementFlags |= SETTLEMENT_PRESENT_FLAG;
            boolean primary = radio != null
                    && radio.isPrimarySettlementRadio(level);
            settlementFlags |= primary ? PRIMARY_RADIO_FLAG : 0;
            settlementFlags |= primary
                    && settlement.name().isEmpty()
                    && radio != null
                    && radio.canManage(player)
                    ? NAME_REQUIRED_FLAG
                    : 0;
            settlementFlags |= settlement.hasUpgrade(
                    SettlementUpgrade.CAMP_HUB
            ) ? CAMP_HUB_FLAG : 0;
            data.set(
                    SETTLEMENT_POPULATION_DATA,
                    settlement.population()
            );
            data.set(SETTLEMENT_RATIONS_DATA, settlement.rations());
            data.set(
                    SETTLEMENT_ACTIVE_RADIOS_DATA,
                    settlement.activeRadioCount()
            );
            data.set(
                    SETTLEMENT_RADIOS_DATA,
                    settlement.radioCount()
            );
            data.set(
                    SETTLEMENT_UPGRADES_DATA,
                    settlement.upgradeMask()
            );
            data.set(
                    SETTLEMENT_RATION_CONTAINERS_DATA,
                    settlement.rationContainerCount()
            );
            data.set(
                    SETTLEMENT_DAILY_RATIONS_DATA,
                    SettlementRationRules.dailyCost(
                            settlement.population(),
                            SettlementConfig
                                    .RATIONS_PER_SETTLER_PER_DAY
                                    .get()
                    )
            );
            data.set(
                    SETTLEMENT_CIVILIANS_DATA,
                    settlement.civilianPopulation()
            );
            data.set(
                    SETTLEMENT_RIFLEMEN_DATA,
                    settlement.guardPopulation()
            );
            data.set(
                    SETTLEMENT_MOSIN_AMMUNITION_DATA,
                    settlement.mosinAmmunition()
            );
        }
        data.set(SETTLEMENT_FLAGS_DATA, settlementFlags);

        if (!operationsActive) {
            data.set(HOSTILES_DATA, 0);
            data.set(WEATHER_DATA, ScheduledWeather.CLEAR.ordinal());
            data.set(CITY_DANGER_DATA, 0);
            data.set(DELIVERY_READY_DATA, 0);
            data.set(DELIVERY_PENDING_DATA, 0);
            return;
        }

        AABB campBounds = new AABB(status.center()).inflate(status.radius());
        int hostiles = level.getEntitiesOfClass(
                Monster.class,
                campBounds,
                monster -> monster.isAlive()
                        && status.center().distSqr(monster.blockPosition())
                        <= (double) status.radius() * status.radius()
        ).size();
        data.set(HOSTILES_DATA, Math.min(Short.MAX_VALUE, hostiles));
        data.set(WEATHER_DATA, WeatherManager.current(level).ordinal());

        int danger = radio.cityZone() == null
                ? 0
                : CityZoneManager.status(
                        player.getServer(),
                        radio.cityZone()
                ).map(statusValue -> statusValue.dangerLevel())
                        .orElse(0);
        data.set(CITY_DANGER_DATA, danger);

        DeliveryManager.DeliveryStatus deliveries =
                DeliveryManager.status(player);
        data.set(
                DELIVERY_READY_DATA,
                Math.min(Short.MAX_VALUE, deliveries.ready())
        );
        data.set(
                DELIVERY_PENDING_DATA,
                Math.min(Short.MAX_VALUE, deliveries.pending())
        );
    }
}
