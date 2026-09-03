package com.reinhardt.hbm.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.level.Level;

/** 1.7.10 EntityQuackos giant duck. */
public final class LegacyQuackosEntity extends Chicken {
    public LegacyQuackosEntity(EntityType<? extends LegacyQuackosEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Chicken.createAttributes().add(Attributes.MAX_HEALTH, 20.0D);
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        return false;
    }

    @Override
    public void die(net.minecraft.world.damagesource.DamageSource source) {
        if (level().isClientSide) {
            super.die(source);
        }
    }
}
