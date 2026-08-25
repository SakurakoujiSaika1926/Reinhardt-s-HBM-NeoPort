package com.reinhardt.hbm.effect;

import com.reinhardt.hbm.radiation.HbmLivingRadiation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/** Exact 1.7.10 RadAway tick behaviour: remove amplifier + 1 RAD each tick. */
public class RadawayEffect extends MobEffect {
    public RadawayEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xBB4B00);
    }

    @Override
    public boolean applyEffectTick(LivingEntity living, int amplifier) {
        if (!living.level().isClientSide) {
            HbmLivingRadiation radiation = HbmLivingRadiation.get(living);
            radiation.addRadiation(-(amplifier + 1.0F));
            HbmLivingRadiation.set(living, radiation);
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}
