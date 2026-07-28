package io.github.gev414.rotwire.effect;

import io.github.gev414.rotwire.config.SurvivalSystemsConfig;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import toughasnails.api.thirst.IThirst;
import toughasnails.api.thirst.ThirstHelper;

final class SurvivalMeterEffect extends MobEffect {

    private static final int MAX_METER_POINTS = 20;

    private final int direction;

    SurvivalMeterEffect(
            MobEffectCategory category,
            int color,
            int direction
    ) {
        super(category, color);
        this.direction = Integer.signum(direction);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(
            int duration,
            int amplifier
    ) {
        int interval = SurvivalSystemsConfig
                .SLEEP_EFFECT_PULSE_INTERVAL_TICKS
                .get();
        return duration % interval == 0;
    }

    @Override
    public boolean applyEffectTick(
            LivingEntity livingEntity,
            int amplifier
    ) {
        if (!(livingEntity instanceof Player player)) {
            return false;
        }

        int change = direction
                * SurvivalSystemsConfig.SLEEP_METER_POINTS_PER_PULSE.get()
                * (amplifier + 1);
        player.getFoodData().setFoodLevel(adjustMeter(
                player.getFoodData().getFoodLevel(),
                change
        ));

        IThirst thirst = ThirstHelper.getThirst(player);
        thirst.setThirst(adjustMeter(thirst.getThirst(), change));
        return true;
    }

    static int adjustMeter(int current, int change) {
        return Mth.clamp(current + change, 0, MAX_METER_POINTS);
    }
}
