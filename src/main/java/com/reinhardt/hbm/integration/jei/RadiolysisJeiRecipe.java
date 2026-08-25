package com.reinhardt.hbm.integration.jei;

import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.recipe.CrackingRecipe;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmRecipeTypes;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The 1.7.10 RadiolysisRecipes map consists of water plus every cracking
 * recipe. Keep this derived from the authoritative cracking recipe set.
 */
public record RadiolysisJeiRecipe(
        HbmFluidDefinition input,
        int inputAmount,
        HbmFluidDefinition output1,
        int output1Amount,
        HbmFluidDefinition output2,
        int output2Amount
) {
    public static List<RadiolysisJeiRecipe> createAll(RecipeManager recipes) {
        Map<HbmFluidDefinition, RadiolysisJeiRecipe> result = new LinkedHashMap<>();
        HbmFluidDefinition water = HbmFluids.byName("water").orElse(HbmFluids.none());
        result.put(water, new RadiolysisJeiRecipe(
                water, 100,
                HbmFluids.byName("peroxide").orElse(HbmFluids.none()), 80,
                HbmFluids.byName("hydrogen").orElse(HbmFluids.none()), 20
        ));

        for (var holder : recipes.getAllRecipesFor(HbmRecipeTypes.CRACKING.get())) {
            CrackingRecipe recipe = holder.value();
            result.put(recipe.input().fluid(), new RadiolysisJeiRecipe(
                    recipe.input().fluid(), 100,
                    recipe.output1().fluid(), recipe.output1().amount(),
                    recipe.output2().fluid(), recipe.output2().amount()
            ));
        }
        return List.copyOf(result.values());
    }
}
