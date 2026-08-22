package com.reinhardt.hbm.integration.jei;

import com.reinhardt.hbm.foundry.FoundryMaterial;
import com.reinhardt.hbm.foundry.FoundryMaterialStack;
import com.reinhardt.hbm.item.FoundryMoldItem;
import com.reinhardt.hbm.item.ScrapsItem;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public record FoundryCastingJeiRecipe(ItemStack input, ItemStack mold, ItemStack castingBlock, ItemStack output) {
    public static List<FoundryCastingJeiRecipe> createAll() {
        List<FoundryCastingJeiRecipe> recipes = new ArrayList<>();
        for (FoundryMaterial material : FoundryMaterial.ordered()) {
            if (material.behavior() != FoundryMaterial.SmeltingBehavior.SMELTABLE) {
                continue;
            }
            for (FoundryMoldItem.Mold mold : FoundryMoldItem.molds()) {
                ItemStack output = mold.outputFor(material).orElse(ItemStack.EMPTY);
                if (output.isEmpty()) {
                    continue;
                }
                recipes.add(new FoundryCastingJeiRecipe(
                        ScrapsItem.create(new FoundryMaterialStack(material, mold.cost()), true),
                        FoundryMoldItem.stackFor(HbmItems.MOLD.get(), mold.id()),
                        castingBlockFor(mold),
                        output.copy()
                ));
            }
        }
        return recipes;
    }

    private static ItemStack castingBlockFor(FoundryMoldItem.Mold mold) {
        return new ItemStack(mold.size() == 0 ? HbmBlocks.FOUNDRY_MOLD.get() : HbmBlocks.FOUNDRY_BASIN.get());
    }
}
