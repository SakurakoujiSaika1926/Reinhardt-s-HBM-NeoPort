package com.reinhardt.hbm.integration.jei;

import com.reinhardt.hbm.ReinhardtsHBM;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

/** Exact static listing from TileEntitySawmill.getRecipes() in 1.7.10. */
public record SawmillJeiRecipe(Ingredient input, ItemStack result, ItemStack byproduct, int byproductChance) {
    public static List<SawmillJeiRecipe> createAll() {
        ItemStack sawdust = new ItemStack(sawdustItem());
        return List.of(
                new SawmillJeiRecipe(Ingredient.of(ItemTags.LOGS), new ItemStack(Blocks.OAK_PLANKS, 6), sawdust.copy(), 50),
                new SawmillJeiRecipe(Ingredient.of(ItemTags.PLANKS), new ItemStack(Items.STICK, 6), sawdust.copy(), 10),
                new SawmillJeiRecipe(Ingredient.of(Items.STICK), sawdust.copy(), ItemStack.EMPTY, 0),
                new SawmillJeiRecipe(Ingredient.of(ItemTags.SAPLINGS), new ItemStack(Items.STICK), sawdust.copy(), 10)
        );
    }

    private static Item sawdustItem() {
        return BuiltInRegistries.ITEM.get(ReinhardtsHBM.id("powder_sawdust"));
    }
}
