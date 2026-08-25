package com.reinhardt.hbm.item;

import com.reinhardt.hbm.pollution.HbmArmorProtection;
import net.minecraft.world.item.ItemStack;

import java.util.Set;

/** Implemented by wearable HBM masks which can hold a gas-mask filter. */
public interface FilterableGasMask {
    Set<HbmArmorProtection.HazardClass> blacklist();

    boolean isFilterApplicable(ItemStack filter);
}
