package com.reinhardt.hbm.integration.jei;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.foundry.FoundryMaterial;
import com.reinhardt.hbm.foundry.FoundryMaterialStack;
import com.reinhardt.hbm.foundry.FoundryShape;
import com.reinhardt.hbm.item.BedrockOreFragmentItem;
import com.reinhardt.hbm.item.FoundryShapeItem;
import com.reinhardt.hbm.item.RawIngotItem;
import com.reinhardt.hbm.item.ScrapsItem;
import com.reinhardt.hbm.recipe.ArcFurnaceRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;

import java.util.ArrayList;
import java.util.List;

final class ArcFurnaceJeiRecipes {
    private ArcFurnaceJeiRecipes() {
    }

    static List<RecipeHolder<ArcFurnaceRecipe>> appendDynamic(
            RecipeManager recipeManager,
            net.minecraft.core.HolderLookup.Provider registries,
            List<RecipeHolder<ArcFurnaceRecipe>> registered
    ) {
        List<RecipeHolder<ArcFurnaceRecipe>> recipes = new ArrayList<>(registered);
        appendSmeltingRecipes(recipeManager, registries, registered, recipes);
        int index = 0;
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) {
                continue;
            }
            for (ItemStack input : variants(item)) {
                boolean explicitlyRegistered = registered.stream().anyMatch(holder ->
                        holder.value().hasLiquidOutput() && holder.value().input().test(input));
                if (explicitlyRegistered) {
                    continue;
                }
                List<FoundryMaterialStack> outputs = ArcFurnaceRecipe.dynamicOutputs(input);
                if (outputs.isEmpty()) {
                    continue;
                }
                ArcFurnaceRecipe recipe = new ArcFurnaceRecipe(
                        "dynamic",
                        DataComponentIngredient.of(true, input),
                        1,
                        ItemStack.EMPTY,
                        outputs.stream()
                                .map(output -> new ArcFurnaceRecipe.MaterialOutput(output.material(), output.amount()))
                                .toList(),
                        400
                );
                ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
                recipes.add(new RecipeHolder<>(ReinhardtsHBM.id(
                        "jei/arc_furnace_dynamic/" + itemId.getNamespace() + "/" + itemId.getPath() + "_" + index++
                ), recipe));
            }
        }
        return recipes;
    }

    private static void appendSmeltingRecipes(
            RecipeManager recipeManager,
            net.minecraft.core.HolderLookup.Provider registries,
            List<RecipeHolder<ArcFurnaceRecipe>> registered,
            List<RecipeHolder<ArcFurnaceRecipe>> recipes
    ) {
        for (var holder : recipeManager.getAllRecipesFor(RecipeType.SMELTING)) {
            if (holder.value().getIngredients().isEmpty()) {
                continue;
            }
            var ingredient = holder.value().getIngredients().getFirst();
            ItemStack output = holder.value().getResultItem(registries).copy();
            boolean accepted = java.util.Arrays.stream(ingredient.getItems())
                    .anyMatch(input -> ArcFurnaceRecipe.isArcSmeltable(input, output));
            boolean explicitlyRegistered = java.util.Arrays.stream(ingredient.getItems()).anyMatch(input ->
                    registered.stream().anyMatch(arc -> arc.value().hasSolidOutput() && arc.value().input().test(input)));
            if (!accepted || output.isEmpty() || explicitlyRegistered) {
                continue;
            }
            ArcFurnaceRecipe recipe = new ArcFurnaceRecipe(
                    "dynamic_smelting",
                    ingredient,
                    1,
                    output,
                    List.of(),
                    400
            );
            ResourceLocation id = holder.id();
            recipes.add(new RecipeHolder<>(ReinhardtsHBM.id(
                    "jei/arc_furnace_smelting/" + id.getNamespace() + "/" + id.getPath()
            ), recipe));
        }
    }

    private static List<ItemStack> variants(Item item) {
        if (item instanceof FoundryShapeItem shapeItem) {
            return FoundryMaterial.ordered().stream()
                    .filter(material -> material.behavior() == FoundryMaterial.SmeltingBehavior.SMELTABLE)
                    .filter(material -> FoundryShapeItem.supports(shapeItem.shape(), material))
                    .map(material -> FoundryShapeItem.stackFor(item, material))
                    .toList();
        }
        if (item instanceof RawIngotItem) {
            return FoundryMaterial.ordered().stream()
                    .filter(RawIngotItem::supports)
                    .map(material -> RawIngotItem.stackFor(item, material))
                    .toList();
        }
        if (item instanceof BedrockOreFragmentItem) {
            return BedrockOreFragmentItem.creativeVariants();
        }
        if (item instanceof ScrapsItem) {
            return FoundryMaterial.ordered().stream()
                    .filter(material -> material.behavior() == FoundryMaterial.SmeltingBehavior.SMELTABLE)
                    .map(material -> ScrapsItem.create(
                            new FoundryMaterialStack(material, FoundryShape.INGOT.q(1)), false
                    ))
                    .toList();
        }
        return List.of(new ItemStack(item));
    }
}
