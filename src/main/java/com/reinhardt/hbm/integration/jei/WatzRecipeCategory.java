package com.reinhardt.hbm.integration.jei;

import com.reinhardt.hbm.registry.HbmBlocks;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class WatzRecipeCategory implements IRecipeCategory<WatzJeiRecipe> {
    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable arrow;

    public WatzRecipeCategory(IGuiHelper helper) {
        this.background = helper.createBlankDrawable(128, 54);
        this.icon = helper.createDrawableItemStack(new ItemStack(HbmBlocks.WATZ.get()));
        this.arrow = helper.getRecipeArrow();
    }

    @Override
    public RecipeType<WatzJeiRecipe> getRecipeType() {
        return HbmJeiPlugin.WATZ;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.reinhardtshbm.watz");
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
    public void setRecipe(IRecipeLayoutBuilder builder, WatzJeiRecipe recipe, IFocusGroup focuses) {
        builder.addInputSlot(16, 19).addItemStack(recipe.input().copy());
        builder.addSlot(RecipeIngredientRole.CATALYST, 53, 19).addItemStack(new ItemStack(HbmBlocks.WATZ.get()));
        builder.addOutputSlot(98, 19).addItemStack(recipe.output().copy());
    }

    @Override
    public void draw(WatzJeiRecipe recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView recipeSlotsView,
                     GuiGraphics guiGraphics, double mouseX, double mouseY) {
        this.arrow.draw(guiGraphics, 73, 20);
    }
}
