package com.reinhardt.hbm.event;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.recipe.AssemblyMachineRecipe;
import com.reinhardt.hbm.registry.HbmRecipeTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@EventBusSubscriber(modid = ReinhardtsHBM.MOD_ID)
public final class RecipeDiagnostics {
    private static final List<String> PETROLEUM_ASSEMBLY_RECIPES = List.of(
            "machine_well",
            "machine_pumpjack",
            "machine_fracking_tower",
            "machine_flare",
            "machine_refinery",
            "machine_catalytic_cracker",
            "machine_coker",
            "machine_vacuum_distill",
            "machine_catalytic_reformer",
            "machine_hydrotreater",
            "machine_liquefactor",
            "machine_solidifier",
            "machine_diesel"
    );

    private RecipeDiagnostics() {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        List<RecipeHolder<AssemblyMachineRecipe>> recipes = event.getServer()
                .getRecipeManager()
                .getAllRecipesFor(HbmRecipeTypes.ASSEMBLY_MACHINE.get());
        List<RecipeHolder<AssemblyMachineRecipe>> noBlueprintRecipes = AssemblyMachineRecipe.activeVariants(
                recipes.stream()
                        .filter(holder -> holder.value().isVisibleForPool(Optional.empty()))
                        .toList()
        );
        List<String> missing = PETROLEUM_ASSEMBLY_RECIPES.stream()
                .filter(path -> !hasRecipe(recipes, path) && !hasRecipe(recipes, path + "_expensive"))
                .toList();

        ReinhardtsHBM.LOGGER.info(
                "HBM assembly recipe diagnostic: loaded={}, no-blueprint-visible={}, pumpjack={}, pumpjack_expensive={}, petroleum_missing={}",
                recipes.size(),
                noBlueprintRecipes.size(),
                hasRecipe(recipes, "machine_pumpjack"),
                hasRecipe(recipes, "machine_pumpjack_expensive"),
                missing
        );
    }

    private static boolean hasRecipe(Collection<RecipeHolder<AssemblyMachineRecipe>> recipes, String path) {
        ResourceLocation id = ReinhardtsHBM.id("assembly_machine/" + path);
        return recipes.stream().anyMatch(holder -> holder.id().equals(id));
    }
}
