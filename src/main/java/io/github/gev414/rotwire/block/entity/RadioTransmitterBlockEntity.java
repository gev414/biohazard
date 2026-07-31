package io.github.gev414.rotwire.block.entity;

import io.github.gev414.rotwire.city.CitySurvey;
import io.github.gev414.rotwire.city.CityZoneKey;
import io.github.gev414.rotwire.city.CityZoneManager;
import io.github.gev414.rotwire.camp.CampModuleType;
import io.github.gev414.rotwire.camp.CampWorkshopRules;
import io.github.gev414.rotwire.config.CityOperationsConfig;
import io.github.gev414.rotwire.config.RadioQuestConfig;
import io.github.gev414.rotwire.item.ModItems;
import io.github.gev414.rotwire.lostcities.LostCitiesCityResolver;
import io.github.gev414.rotwire.lostcities.LostCitiesIntegration;
import io.github.gev414.rotwire.menu.CampRadioMenu;
import io.github.gev414.rotwire.menu.CampStorageMenu;
import io.github.gev414.rotwire.sleep.CampInspector;
import io.github.gev414.rotwire.sleep.CampStatus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

public final class RadioTransmitterBlockEntity extends BlockEntity
        implements MenuProvider {

    private static final String READY_AT_TAG = "ready_at";
    private static final String SURVEY_COMPLETE_TAG = "survey_complete";
    private static final String CITY_ZONE_TAG = "city_zone";
    private static final String CAMP_ID_TAG = "camp_id";
    private static final String OWNER_TAG = "camp_owner";
    private static final String MODULES_TAG = "camp_modules";
    private static final String CACHE_TAG = "camp_cache";
    private static final int CAMP_REFRESH_TICKS = 20;
    private static final int CACHE_SLOTS = 27;

    private long readyAt = -1L;
    private boolean surveyComplete;
    @Nullable
    private CityZoneKey cityZone;
    @Nullable
    private CitySurvey activeSurvey;
    @Nullable
    private UUID campId;
    @Nullable
    private UUID owner;
    private int installedModules;
    private final ItemStackHandler cache = new ItemStackHandler(CACHE_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    public RadioTransmitterBlockEntity(
            BlockPos position,
            BlockState state
    ) {
        super(ModBlockEntities.RADIO_TRANSMITTER.get(), position, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level == null || level.isClientSide()) {
            return;
        }
        if (readyAt < 0L) {
            beginCalibration(level.getGameTime());
        } else {
            ensureSurveyStarted();
        }
    }

    public void beginCalibration(long gameTime) {
        readyAt = gameTime + RadioQuestConfig.CALIBRATION_TICKS.get();
        surveyComplete = false;
        cityZone = null;
        activeSurvey = null;
        ensureSurveyStarted();
        setChanged();
    }

    public void setOwner(UUID playerId) {
        if (owner == null) {
            owner = playerId;
            setChanged();
        }
    }

    public Optional<UUID> campId() {
        return Optional.ofNullable(campId);
    }

    public Optional<UUID> owner() {
        return Optional.ofNullable(owner);
    }

    public boolean hasCampIdentity() {
        return campId != null;
    }

    public boolean hasModule(CampModuleType type) {
        return (installedModules & type.mask()) != 0;
    }

    public int installedModuleMask() {
        return installedModules;
    }

    public ItemStackHandler cache() {
        return cache;
    }

    public boolean canManage(Player player) {
        return owner == null || owner.equals(player.getUUID());
    }

    public boolean installModule(
            ServerPlayer player,
            CampModuleType type
    ) {
        if (!canManage(player)) {
            player.sendSystemMessage(Component.translatable(
                    "message.rotwire.camp.owner_only"
            ));
            return false;
        }
        if (hasModule(type)) {
            player.sendSystemMessage(Component.translatable(
                    "message.rotwire.camp.module_already_installed"
            ));
            return false;
        }
        CampStatus status = CampInspector.inspectRadio(
                player.serverLevel(),
                player,
                worldPosition
        );
        if (!status.active() || !isConnected(level.getGameTime())) {
            player.sendSystemMessage(Component.translatable(
                    "message.rotwire.camp.module_requires_online"
            ));
            return false;
        }
        if (owner == null) {
            owner = player.getUUID();
        }
        installedModules |= type.mask();
        setChanged();
        player.sendSystemMessage(Component.translatable(
                "message.rotwire.camp.module_installed",
                Component.translatable(moduleTranslationKey(type))
        ));
        return true;
    }

    public boolean openStorage(ServerPlayer player) {
        if (!hasModule(CampModuleType.STORAGE)) {
            return false;
        }
        if (!canManage(player)) {
            player.sendSystemMessage(Component.translatable(
                    "message.rotwire.camp.owner_only"
            ));
            return false;
        }
        player.openMenu(
                new SimpleMenuProvider(
                        (containerId, inventory, ignored) ->
                                new CampStorageMenu(
                                        containerId,
                                        inventory,
                                        this
                                ),
                        Component.translatable(
                                "screen.rotwire.camp_storage.title"
                        )
                ),
                buffer -> buffer.writeBlockPos(worldPosition)
        );
        return true;
    }

    public boolean repairHeldItem(ServerPlayer player) {
        if (!hasModule(CampModuleType.CRAFTING)) {
            return false;
        }
        if (!canManage(player)) {
            player.sendSystemMessage(Component.translatable(
                    "message.rotwire.camp.owner_only"
            ));
            return false;
        }
        CampStatus status = CampInspector.inspectRadio(
                player.serverLevel(),
                player,
                worldPosition
        );
        if (!status.active() || !isConnected(level.getGameTime())) {
            player.sendSystemMessage(Component.translatable(
                    "message.rotwire.camp.workshop_offline"
            ));
            return false;
        }

        ItemStack target = player.getMainHandItem();
        if (!target.isDamaged()) {
            player.sendSystemMessage(Component.translatable(
                    "message.rotwire.camp.workshop_no_target"
            ));
            return false;
        }
        int kitSlot = findRepairKit(player);
        if (kitSlot < 0) {
            player.sendSystemMessage(Component.translatable(
                    "message.rotwire.camp.workshop_missing_kit"
            ));
            return false;
        }

        target.setDamageValue(CampWorkshopRules.repairedDamage(
                target.getDamageValue(),
                target.getMaxDamage()
        ));
        if (!player.getAbilities().instabuild) {
            player.getInventory().getItem(kitSlot).shrink(1);
        }
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        player.sendSystemMessage(Component.translatable(
                "message.rotwire.camp.workshop_repaired",
                target.getHoverName()
        ));
        return true;
    }

    public void dropCampContents(Level level) {
        if (level.isClientSide()) {
            return;
        }
        for (int slot = 0; slot < cache.getSlots(); slot++) {
            ItemStack stack = cache.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                Block.popResource(level, worldPosition, stack.copy());
                cache.setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
        for (CampModuleType type : CampModuleType.values()) {
            if (hasModule(type)) {
                Block.popResource(
                        level,
                        worldPosition,
                        new ItemStack(ModItems.moduleItem(type))
                );
            }
        }
        installedModules = 0;
        setChanged();
    }

    public boolean refreshCampIdentity(ServerLevel level) {
        if (campId != null) {
            return true;
        }
        if (!CampInspector.isSheltered(level, worldPosition)) {
            return false;
        }
        campId = UUID.randomUUID();
        setChanged();
        return true;
    }

    public boolean isConnected(long gameTime) {
        return readyAt >= 0L
                && gameTime >= readyAt
                && surveyComplete;
    }

    public long ticksUntilConnected(long gameTime) {
        long calibration = readyAt < 0L
                ? RadioQuestConfig.CALIBRATION_TICKS.get()
                : Math.max(0L, readyAt - gameTime);
        return !surveyComplete && calibration == 0L
                ? 20L
                : calibration;
    }

    public boolean isSurveying() {
        return !surveyComplete && activeSurvey != null;
    }

    public int surveyedChunks() {
        return activeSurvey == null ? 0 : activeSurvey.scannedChunks();
    }

    public int maximumSurveyChunks() {
        return CityOperationsConfig.MAX_SURVEYED_CHUNKS.get();
    }

    public @Nullable CityZoneKey cityZone() {
        return cityZone;
    }

    public void serverTick(ServerLevel serverLevel) {
        if (Math.floorMod(
                serverLevel.getGameTime() + worldPosition.asLong(),
                CAMP_REFRESH_TICKS
        ) == 0L) {
            refreshCampIdentity(serverLevel);
        }
        if (surveyComplete) {
            return;
        }
        if (!CityOperationsConfig.ENABLED.get()) {
            surveyComplete = true;
            activeSurvey = null;
            setChanged();
            return;
        }
        if (LostCitiesIntegration.api() == null) {
            return;
        }

        ensureSurveyStarted();
        if (activeSurvey == null) {
            return;
        }
        activeSurvey.advance(
                (chunkX, chunkZ) ->
                        LostCitiesCityResolver.isCityChunk(
                                serverLevel,
                                chunkX,
                                chunkZ
                        ),
                CityOperationsConfig.SURVEY_CHUNKS_PER_TICK.get(),
                CityOperationsConfig.MAX_SURVEYED_CHUNKS.get(),
                CityOperationsConfig.DIAGONAL_CONNECTIVITY.get()
        );
        if (!activeSurvey.complete()) {
            return;
        }

        cityZone = CityZoneManager.registerSurvey(
                serverLevel,
                activeSurvey
        ).orElse(null);
        surveyComplete = true;
        activeSurvey = null;
        setChanged();
    }

    private void ensureSurveyStarted() {
        if (surveyComplete || activeSurvey != null) {
            return;
        }
        activeSurvey = new CitySurvey(
                worldPosition.getX() >> 4,
                worldPosition.getZ() >> 4
        );
    }

    @Override
    protected void saveAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        super.saveAdditional(tag, registries);
        tag.putLong(READY_AT_TAG, readyAt);
        tag.putBoolean(SURVEY_COMPLETE_TAG, surveyComplete);
        if (cityZone != null) {
            tag.put(CITY_ZONE_TAG, cityZone.save());
        }
        if (campId != null) {
            tag.putUUID(CAMP_ID_TAG, campId);
        }
        if (owner != null) {
            tag.putUUID(OWNER_TAG, owner);
        }
        tag.putInt(MODULES_TAG, installedModules);
        tag.put(CACHE_TAG, cache.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        super.loadAdditional(tag, registries);
        readyAt = tag.contains(READY_AT_TAG)
                ? tag.getLong(READY_AT_TAG)
                : -1L;
        surveyComplete = tag.getBoolean(SURVEY_COMPLETE_TAG);
        cityZone = tag.contains(CITY_ZONE_TAG, CompoundTag.TAG_COMPOUND)
                ? CityZoneKey.load(tag.getCompound(CITY_ZONE_TAG))
                : null;
        campId = tag.hasUUID(CAMP_ID_TAG)
                ? tag.getUUID(CAMP_ID_TAG)
                : null;
        owner = tag.hasUUID(OWNER_TAG)
                ? tag.getUUID(OWNER_TAG)
                : null;
        installedModules = CampModuleType.sanitizeMask(
                tag.getInt(MODULES_TAG)
        );
        if (tag.contains(CACHE_TAG, CompoundTag.TAG_COMPOUND)) {
            cache.deserializeNBT(
                    registries,
                    tag.getCompound(CACHE_TAG)
            );
        }
        activeSurvey = null;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(
                "screen.rotwire.camp_radio.title"
        );
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(
            int containerId,
            Inventory inventory,
            Player player
    ) {
        return new CampRadioMenu(containerId, inventory, this);
    }

    private static int findRepairKit(ServerPlayer player) {
        for (int slot = 0;
             slot < player.getInventory().getContainerSize();
             slot++) {
            if (player.getInventory().getItem(slot).is(
                    ModItems.FIELD_REPAIR_KIT.get()
            )) {
                return slot;
            }
        }
        return -1;
    }

    private static String moduleTranslationKey(CampModuleType type) {
        return switch (type) {
            case STORAGE -> "item.rotwire.quartermaster_cache_module";
            case CRAFTING -> "item.rotwire.field_workshop_module";
            case OPERATIONS -> "item.rotwire.operations_relay_module";
        };
    }
}
