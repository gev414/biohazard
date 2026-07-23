package io.github.gev414.biohazard.stealth;

import io.github.gev414.biohazard.config.SurvivalSystemsConfig;
import mod.pbj.attachment.Attachment;
import mod.pbj.attachment.Attachments;
import mod.pbj.feature.SoundFeature;
import mod.pbj.item.GunItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class PointBlankAttention {

    private static final double SOUND_OWNER_RANGE_SQUARED = 16.0D;
    private static final Map<UUID, Long> LAST_PROCESSED_SHOT =
            new HashMap<>();

    public static void onSoundAtPosition(
            PlayLevelSoundEvent.AtPosition event
    ) {
        if (!SurvivalSystemsConfig.ENABLED.get()
                || event.getSource() != SoundSource.PLAYERS
                || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        ServerPlayer shooter = null;
        ItemStack gunStack = ItemStack.EMPTY;
        double nearestDistance = SOUND_OWNER_RANGE_SQUARED;
        SoundEvent playedSound = event.getSound().value();
        Vec3 position = event.getPosition();

        for (ServerPlayer candidate : level.players()) {
            double distance = candidate.distanceToSqr(position);
            if (distance > nearestDistance) {
                continue;
            }
            ItemStack candidateGun = matchingGun(
                    candidate,
                    playedSound
            );
            if (!candidateGun.isEmpty()) {
                shooter = candidate;
                gunStack = candidateGun;
                nearestDistance = distance;
            }
        }
        if (shooter == null) {
            return;
        }

        long now = level.getGameTime();
        if (LAST_PROCESSED_SHOT.getOrDefault(
                shooter.getUUID(),
                Long.MIN_VALUE
        ) == now) {
            return;
        }
        LAST_PROCESSED_SHOT.put(shooter.getUUID(), now);

        boolean suppressed = hasSuppressor(gunStack);
        double range = suppressed
                ? SurvivalSystemsConfig.SUPPRESSED_FIRE_RANGE.get()
                : SurvivalSystemsConfig.UNSUPPRESSED_FIRE_RANGE.get();
        AttentionManager.emit(
                level,
                position,
                shooter,
                range,
                !suppressed
        );
    }

    public static void clear() {
        LAST_PROCESSED_SHOT.clear();
    }

    static boolean hasSuppressor(ItemStack gunStack) {
        Collection<ItemStack> attachments =
                Attachments.getAttachments(gunStack);
        for (ItemStack stack : attachments) {
            String itemPath = BuiltInRegistries.ITEM
                    .getKey(stack.getItem())
                    .getPath()
                    .toLowerCase(Locale.ROOT);
            if (looksLikeSuppressor(itemPath)) {
                return true;
            }
            if (stack.getItem() instanceof Attachment attachment) {
                for (String group : attachment.getGroups()) {
                    if (looksLikeSuppressor(
                            group.toLowerCase(Locale.ROOT)
                    )) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static ItemStack matchingGun(
            ServerPlayer player,
            SoundEvent playedSound
    ) {
        if (matchesFireSound(player.getMainHandItem(), playedSound)) {
            return player.getMainHandItem();
        }
        if (matchesFireSound(player.getOffhandItem(), playedSound)) {
            return player.getOffhandItem();
        }
        return ItemStack.EMPTY;
    }

    private static boolean matchesFireSound(
            ItemStack stack,
            SoundEvent playedSound
    ) {
        if (!(stack.getItem() instanceof GunItem)) {
            return false;
        }
        SoundFeature.SoundDescriptor descriptor =
                SoundFeature.getFireSoundAndVolume(stack);
        return descriptor != null
                && descriptor.soundSupplier() != null
                && playedSound.equals(descriptor.soundSupplier().get());
    }

    private static boolean looksLikeSuppressor(String value) {
        return value.contains("suppressor")
                || value.contains("silencer")
                || value.contains("silenced");
    }

    private PointBlankAttention() {
    }
}
