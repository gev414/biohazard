package io.github.gev414.biohazard.effect;

import io.github.gev414.biohazard.Biohazard;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEffects {

    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, Biohazard.MOD_ID);

    public static final DeferredHolder<MobEffect, MobEffect> RESTLESS_SLEEP =
            MOB_EFFECTS.register(
                    "restless_sleep",
                    () -> new SurvivalMeterEffect(
                            MobEffectCategory.HARMFUL,
                            0x6B625E,
                            -1
                    )
            );

    public static final DeferredHolder<MobEffect, MobEffect> NEW_DAWN =
            MOB_EFFECTS.register(
                    "new_dawn",
                    () -> new SurvivalMeterEffect(
                            MobEffectCategory.BENEFICIAL,
                            0xF4C95D,
                            1
                    )
            );

    private ModEffects() {
    }
}
