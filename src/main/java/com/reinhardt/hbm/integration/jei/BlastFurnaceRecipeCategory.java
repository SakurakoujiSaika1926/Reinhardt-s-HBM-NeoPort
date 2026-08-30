package com.reinhardt.hbm.integration.jei;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.recipe.BlastFurnaceRecipe;
import com.reinhardt.hbm.registry.HbmBlocks;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.Arrays;
import java.util.List;

public class BlastFurnaceRecipeCategory implements IRecipeCategory<RecipeHolder<BlastFurnaceRecipe>> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/guidifurnace.png");

    private final IDrawable background;
    private final IDrawable icon;

    public BlastFurnaceRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 0, 0, 176, 83);
        this.icon = helper.createDrawableItemStack(new ItemStack(HbmBlocks.MACHINE_BLAST_FURNACE.get()));
    }

    @Override
    public RecipeType<RecipeHolder<BlastFurnaceRecipe>> getRecipeType() {
        return HbmJeiPlugin.BLAST_FURNACE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.reinhardtshbm.machine_blast_furnace");
    }

    @Override
    public IDrawable getBackground() {
        return this.background;
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<BlastFurnaceRecipe> holder, IFocusGroup focuses) {
        BlastFurnaceRecipe recipe = holder.value();
        builder.addInputSlot(80, 18)
                .addItemStacks(stacks(recipe.inputA(), recipe.inputACount()));
        builder.addInputSlot(80, 54)
                .addItemStacks(stacks(recipe.inputB(), recipe.inputBCount()));
        builder.addOutputSlot(134, 36)
                .addItemStack(recipe.result().copy());
    }

    private static List<ItemStack> stacks(net.minecraft.world.item.crafting.Ingredient ingredient, int count) {
        return Arrays.stream(ingredient.getItems())
                .map(ItemStack::copy)
                .peek(stack -> stack.setCount(count))
                .toList();
    }
}
