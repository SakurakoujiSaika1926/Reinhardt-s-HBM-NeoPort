package com.reinhardt.hbm.effect;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/** The delayed B93 detonation applied by the legacy CBT device. */
public final class BangEffect extends MobEffect {
    public BangEffect() {
        super(MobEffectCategory.HARMFUL, 0x111111);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide) {
            return false;
        }
        entity.hurt(entity.damageSources().magic(), 1_000.0F);
        entity.setHealth(0.0F);
        entity.level().playSound(null, entity.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 4.0F, 1.0F);
        entity.level().explode(null, entity.getX(), entity.getY(), entity.getZ(), 0.0F, false, net.minecraft.world.level.Level.ExplosionInteraction.NONE);
        return false;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration <= 10;
    }
}
