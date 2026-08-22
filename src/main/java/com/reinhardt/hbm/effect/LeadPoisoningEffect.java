package com.reinhardt.hbm.effect;

import com.reinhardt.hbm.registry.HbmDamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class LeadPoisoningEffect extends MobEffect {
    public LeadPoisoningEffect() {
        super(MobEffectCategory.HARMFUL, 0x767682);
    }

    @Override
    public boolean applyEffectTick(LivingEntity living, int amplifier) {
        if (!living.level().isClientSide) {
            living.hurt(living.damageSources().source(HbmDamageTypes.LEAD), amplifier + 1.0F);
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 60 == 0;
    }
}
