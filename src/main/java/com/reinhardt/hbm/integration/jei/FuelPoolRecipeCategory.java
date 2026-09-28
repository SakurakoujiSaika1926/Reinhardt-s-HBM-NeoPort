package com.reinhardt.hbm.integration.jei;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.recipe.FuelPoolRecipe;
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

public class FuelPoolRecipeCategory implements IRecipeCategory<RecipeHolder<FuelPoolRecipe>> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/nei/gui_nei.png");

    private final IDrawable background;
    private final IDrawable icon;

    public FuelPoolRecipeCategory(IGuiHelper helper) {
        // FuelPoolHandler inherited these exact coordinates from the 1.7.10
        // NEIUniversalHandler.  Cropping gui_waste_drum used a different
        // origin and left the JEI stacks outside the texture's slot frames.
        this.background = helper.createDrawable(TEXTURE, 5, 11, 166, 65);
        this.icon = helper.createDrawableItemStack(new ItemStack(HbmBlocks.MACHINE_WASTE_DRUM.get()));
    }

    @Override
    public RecipeType<RecipeHolder<FuelPoolRecipe>> getRecipeType() {
        return HbmJeiPlugin.FUEL_POOL;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.reinhardtshbm.waste_drum");
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<FuelPoolRecipe> holder, IFocusGroup focuses) {
        FuelPoolRecipe recipe = holder.value();
        builder.addInputSlot(48, 24).addItemStacks(stacks(recipe.input().getItems()));
        builder.addSlot(RecipeIngredientRole.CATALYST, 75, 31)
                .addItemStack(new ItemStack(HbmBlocks.MACHINE_WASTE_DRUM.get()));
        builder.addOutputSlot(102, 24).addItemStack(recipe.result().copy());
    }

    private static List<ItemStack> stacks(ItemStack[] stacks) {
        return Arrays.stream(stacks).map(ItemStack::copy).toList();
    }
}
