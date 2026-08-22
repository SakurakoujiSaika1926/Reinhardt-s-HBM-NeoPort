package com.reinhardt.hbm.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class PhosphorusBurnEffect extends MobEffect {
    public PhosphorusBurnEffect() {
        super(MobEffectCategory.HARMFUL, 0xFFFF00);
    }

    @Override
    public boolean applyEffectTick(LivingEntity living, int amplifier) {
        if (!living.level().isClientSide) {
            living.igniteForSeconds(1.0F);
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}
