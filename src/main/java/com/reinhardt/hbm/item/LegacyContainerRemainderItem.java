package com.reinhardt.hbm.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

/** Preserves 1.7.10 Item#setContainerItem behavior for legacy consumable containers. */
public final class LegacyContainerRemainderItem extends Item {
    private final Supplier<Item> remainder;

    public LegacyContainerRemainderItem(Properties properties, Supplier<Item> remainder) {
        super(properties);
        this.remainder = remainder;
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        return new ItemStack(this.remainder.get());
    }
}
