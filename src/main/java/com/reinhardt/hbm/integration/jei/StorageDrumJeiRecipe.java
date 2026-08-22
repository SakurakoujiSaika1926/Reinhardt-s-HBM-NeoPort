package com.reinhardt.hbm.integration.jei;

import com.reinhardt.hbm.item.NuclearWasteItem;
import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Exact 1.7.10 RBMKWasteDecayHandler input/output listing. */
public record StorageDrumJeiRecipe(ItemStack input, ItemStack output) {
    public static List<StorageDrumJeiRecipe> createAll() {
        List<StorageDrumJeiRecipe> recipes = new ArrayList<>();
        addFamily(recipes, HbmItems.NUCLEAR_WASTE_LONG.get(), HbmItems.NUCLEAR_WASTE_LONG_DEPLETED.get());
        addFamily(recipes, HbmItems.NUCLEAR_WASTE_LONG_TINY.get(), HbmItems.NUCLEAR_WASTE_LONG_DEPLETED_TINY.get());
        addFamily(recipes, HbmItems.NUCLEAR_WASTE_SHORT.get(), HbmItems.NUCLEAR_WASTE_SHORT_DEPLETED.get());
        addFamily(recipes, HbmItems.NUCLEAR_WASTE_SHORT_TINY.get(), HbmItems.NUCLEAR_WASTE_SHORT_DEPLETED_TINY.get());
        return List.copyOf(recipes);
    }

    private static void addFamily(List<StorageDrumJeiRecipe> recipes, Item input, Item output) {
        if (!(input instanceof NuclearWasteItem waste)) {
            return;
        }
        for (NuclearWasteItem.WasteClass wasteClass : NuclearWasteItem.WasteClass.forFamily(waste.family())) {
            recipes.add(new StorageDrumJeiRecipe(waste.stackFor(wasteClass), NuclearWasteItem.copyWasteClass(waste.stackFor(wasteClass), output)));
        }
    }
}
