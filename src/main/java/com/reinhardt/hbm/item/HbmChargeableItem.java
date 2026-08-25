package com.reinhardt.hbm.item;

import net.minecraft.world.item.ItemStack;

/**
 * Shared item-side energy contract.  It intentionally mirrors the old HBM
 * battery API so powered armor can use the same charging machines as batteries.
 */
public interface HbmChargeableItem {
    long hbmCharge(ItemStack stack);

    long hbmCapacity(ItemStack stack);

    long hbmChargeRate(ItemStack stack);

    long hbmDischargeRate(ItemStack stack);

    void hbmSetCharge(ItemStack stack, long charge);
}
