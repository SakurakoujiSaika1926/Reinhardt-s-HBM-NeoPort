package com.reinhardt.hbm.integration.jei;

import com.reinhardt.hbm.item.PwrFuelItem;
import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.stream.IntStream;

/** Exact JEI counterpart of the 1.7.10 PWRRecipeHandler fuel-state listing. */
public record PwrJeiRecipe(ItemStack input, ItemStack output) {
    public static List<PwrJeiRecipe> createAll() {
        return IntStream.range(0, PwrFuelItem.FUELS.size())
                .mapToObj(index -> new PwrJeiRecipe(
                        PwrFuelItem.stackForFuel(HbmItems.PWR_FUEL.get(), index),
                        PwrFuelItem.stackForFuel(HbmItems.PWR_FUEL_HOT.get(), index)
                ))
                .toList();
    }
}
