package io.github.gev414.biohazard.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.smileycorp.hordes.common.capability.HordesCapabilities;
import net.smileycorp.hordes.infection.HordesInfection;
import net.smileycorp.hordes.infection.capability.Infection;
import net.smileycorp.hordes.infection.network.CureEntityMessage;
import net.smileycorp.hordes.infection.network.InfectionPacketHandler;

import java.util.List;

/**
 * Bottle-based medicine integrated with The Hordes' infection effects.
 * Extending the active stage is deliberately different from curing it: the
 * amplifier is preserved and the infection capability is not advanced.
 */
public final class InfectionMedicineItem extends PotionItem {

    static final int TICKS_PER_SECOND = 20;
    static final int SUPPRESSANT_EXTENSION_TICKS = 5 * 60 * TICKS_PER_SECOND;
    static final int SUPPRESSANT_MAX_REMAINING_TICKS = 10 * 60 * TICKS_PER_SECOND;
    static final int SUPPRESSANT_IMMUNITY_TICKS = 5 * 60 * TICKS_PER_SECOND;

    public enum Kind {
        FULL_CURE,
        SUPPRESSANT
    }

    private final Kind kind;

    public InfectionMedicineItem(Kind kind, Item.Properties properties) {
        super(properties);
        this.kind = kind;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (kind == Kind.FULL_CURE && !player.hasEffect(HordesInfection.INFECTED)) {
            if (!level.isClientSide) {
                sendStatus(player, "message.biohazard.infection.not_infected");
            }
            return InteractionResultHolder.fail(player.getItemInHand(hand));
        }
        return super.use(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide) {
            if (kind == Kind.FULL_CURE) {
                cure(entity);
            } else {
                suppress(entity);
            }
        }

        return super.finishUsingItem(stack, level, entity);
    }

    private static void cure(LivingEntity entity) {
        if (!entity.hasEffect(HordesInfection.INFECTED)) {
            sendStatus(entity, "message.biohazard.infection.not_infected");
            return;
        }

        Infection infection = entity.getCapability(HordesCapabilities.INFECTION);
        if (infection != null) {
            // Match The Hordes' native cure behavior, including its escalating
            // future-infection bookkeeping.
            infection.increaseInfection();
        }

        if (entity.removeEffect(HordesInfection.INFECTED)) {
            InfectionPacketHandler.sendTracking(new CureEntityMessage(entity), entity);
            sendStatus(entity, "message.biohazard.infection.cured");
        }
    }

    private static void suppress(LivingEntity entity) {
        MobEffectInstance current = entity.getEffect(HordesInfection.INFECTED);
        if (current != null) {
            int extendedDuration = extendedDuration(current.getDuration());
            entity.addEffect(new MobEffectInstance(
                    HordesInfection.INFECTED,
                    extendedDuration,
                    current.getAmplifier(),
                    current.isAmbient(),
                    current.isVisible(),
                    current.showIcon()
            ));
            sendStatus(entity, "message.biohazard.infection.suppressed");
        } else {
            sendStatus(entity, "message.biohazard.infection.immunity");
        }

        entity.addEffect(new MobEffectInstance(
                HordesInfection.IMMUNITY,
                SUPPRESSANT_IMMUNITY_TICKS
        ));
    }

    static int extendedDuration(int currentDuration) {
        long extended = (long) currentDuration + SUPPRESSANT_EXTENSION_TICKS;
        return (int) Math.min(extended, SUPPRESSANT_MAX_REMAINING_TICKS);
    }

    private static void sendStatus(LivingEntity entity, String translationKey) {
        if (entity instanceof ServerPlayer player) {
            player.displayClientMessage(Component.translatable(translationKey), true);
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public String getDescriptionId(ItemStack stack) {
        return getOrCreateDescriptionId();
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        String suffix = kind == Kind.FULL_CURE ? "infection_cure" : "antiviral_suppressant";
        tooltip.add(Component.translatable("tooltip.biohazard." + suffix).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.biohazard." + suffix + ".detail")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
