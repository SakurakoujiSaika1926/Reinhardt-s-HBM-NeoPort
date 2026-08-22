package com.reinhardt.hbm.integration.jei;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.recipe.PyroOvenRecipe;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.ArrayList;
import java.util.List;

/** Mirrors PyroOvenRecipes#registerSFAuto for JEI without introducing a second fuel list. */
final class PyroOvenJeiRecipes {
    private PyroOvenJeiRecipes() {
    }

    static List<RecipeHolder<PyroOvenRecipe>> appendSolidFuelRecipes(List<RecipeHolder<PyroOvenRecipe>> registered) {
        List<RecipeHolder<PyroOvenRecipe>> recipes = new ArrayList<>(registered);
        int index = 0;
        for (HbmFluidDefinition fluid : HbmFluids.definitions()) {
            long heatPerBucket = fluid.flammableHeatEnergy();
            if (fluid.isNone() || heatPerBucket <= 0L) {
                continue;
            }
            boolean balefire = fluid.name().equals("balefire");
            long targetHeat = balefire ? 24_000_000L : 1_440_000L;
            long rawAmount = targetHeat * 1_000L / 2L / heatPerBucket;
            int amount = rawAmount > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) rawAmount;
            if (amount > 10_000) amount -= amount % 1_000;
            else if (amount > 1_000) amount -= amount % 100;
            else if (amount > 100) amount -= amount % 10;
            amount = Math.max(1, amount);

            ItemStack output = new ItemStack(balefire ? HbmItems.SOLID_FUEL_BF.get() : HbmItems.SOLID_FUEL.get());
            PyroOvenRecipe recipe = new PyroOvenRecipe(
                    "pyrolysis.solid_fuel",
                    PyroOvenRecipe.ItemInput.EMPTY,
                    new PyroOvenRecipe.FluidInput(fluid, amount),
                    output,
                    PyroOvenRecipe.FluidOutput.EMPTY,
                    60
            );
            recipes.add(new RecipeHolder<>(ReinhardtsHBM.id("jei/pyrolysis/solid_fuel_" + fluid.name() + "_" + index++), recipe));
        }
        return recipes;
    }
}
