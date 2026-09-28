package com.reinhardt.hbm.util;

import net.minecraft.world.item.ItemStack;

/** Shared vanilla-compatible single-fuel consumption and container handling. */
public final class FuelItemConsumption {
    private FuelItemConsumption() {
    }

    /**
     * Shrinks one item and returns the replacement for the fuel slot when the
     * stack became empty. This is the 1.7.10 furnace container-item contract
     * used by buckets and eternal coal.
     */
    public static ItemStack consumeOne(ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack consumed = stack.copyWithCount(1);
        stack.shrink(1);
        if (!stack.isEmpty() || !consumed.getItem().hasCraftingRemainingItem(consumed)) {
            return ItemStack.EMPTY;
        }
        ItemStack remainder = consumed.getItem().getCraftingRemainingItem(consumed);
        return remainder.isEmpty() ? ItemStack.EMPTY : remainder.copy();
    }
}
