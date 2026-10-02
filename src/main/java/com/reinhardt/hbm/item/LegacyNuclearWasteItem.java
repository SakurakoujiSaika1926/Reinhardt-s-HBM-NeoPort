package com.reinhardt.hbm.item;

import com.reinhardt.hbm.entity.LegacyWasteItemEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The five non-isotopic 1.7.10 waste items used EntityItemWaste as well:
 * they never despawn and their dropped form is immune to every damage source.
 */
public final class LegacyNuclearWasteItem extends Item {
    public LegacyNuclearWasteItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getEntityLifespan(ItemStack stack, Level level) {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean hasCustomEntity(ItemStack stack) {
        return true;
    }

    @Override
    public Entity createEntity(Level level, Entity original, ItemStack stack) {
        if (!(original instanceof ItemEntity itemEntity)) {
            return original;
        }
        LegacyWasteItemEntity replacement = new LegacyWasteItemEntity(
                level, itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(), stack.copy());
        replacement.setDeltaMovement(itemEntity.getDeltaMovement());
        replacement.setPickUpDelay(10);
        original.discard();
        return replacement;
    }
}
