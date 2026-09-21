package com.reinhardt.hbm.item;

import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Legacy spent ZIRNOX rods remain stackable and return their empty casing when crafted. */
public final class DepletedZirnoxRodItem extends LegacyVariantItem {
    public DepletedZirnoxRodItem(Properties properties, List<Variant> variants) {
        super(properties, "rod_zirnox_depleted", variants);
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        return new ItemStack(HbmItems.ROD_ZIRNOX_EMPTY.get());
    }
}
