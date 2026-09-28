package com.reinhardt.hbm.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The hidden eternal coal added by HBM 1.0.27.
 *
 * <p>The 1.7.10 item registers itself as its container item.  Returning the
 * consumed stack here preserves that contract for crafting and for modern
 * furnace implementations which honour crafting remainders.</p>
 */
public final class EternalCoalItem extends Item {
    public EternalCoalItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        return stack.copyWithCount(1);
    }
}
