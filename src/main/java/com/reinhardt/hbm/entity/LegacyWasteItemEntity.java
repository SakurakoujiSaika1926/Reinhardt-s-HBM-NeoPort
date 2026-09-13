package com.reinhardt.hbm.entity;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** EntityItemWaste: nuclear-waste item entities cannot be damaged or removed by explosions. */
public final class LegacyWasteItemEntity extends ItemEntity {
    public LegacyWasteItemEntity(EntityType<? extends LegacyWasteItemEntity> type, Level level) {
        super(type, level);
    }

    public LegacyWasteItemEntity(Level level, double x, double y, double z, ItemStack stack) {
        this(com.reinhardt.hbm.registry.HbmEntityTypes.WASTE_ITEM.get(), level);
        setPos(x, y, z);
        setItem(stack);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }
}
