package com.reinhardt.hbm.effect;

import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.ItemStack;

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
        // HBM 1.7.10's only cheese source: a cow killed by the delayed B93
        // effect drops three cheese (ten when it is a calf).
        if (entity instanceof Cow cow) {
            cow.spawnAtLocation(new ItemStack(HbmItems.CHEESE.get(), cow.isBaby() ? 10 : 3), 1.0F);
        }
        return false;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration <= 10;
    }
}
