package com.reinhardt.hbm.pneumatic;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

/**
 * Storage-side contract used by the pneumatic logistics cache.  Counts are
 * deliberately long even though the current legacy storage implementations
 * use int-sized counters; a network may aggregate many storage blocks.
 */
public interface PneumaticStorageNode {
    BlockPos pneumaticPosition();

    boolean pneumaticAvailableFrom(BlockPos accessor);

    int pneumaticSlotCount();

    ItemStack pneumaticPrototype(int slot);

    long pneumaticAmount(int slot);

    long pneumaticInsert(int slot, ItemStack stack, long amount, boolean simulate);

    long pneumaticExtract(int slot, ItemStack stack, long amount, boolean simulate);
}
