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

public final class IcfToolingRecipeCategory implements IRecipeCategory<IcfToolingJeiRecipe> {
    private final IDrawable canvas;
    private final IDrawable icon;
    private final IDrawable arrow;

    public IcfToolingRecipeCategory(IGuiHelper helper) {
        this.canvas = helper.createBlankDrawable(166, 54);
        this.icon = helper.createDrawableItemStack(new ItemStack(HbmBlocks.ICF_COMPONENT.get()));
        this.arrow = helper.getRecipeArrow();
    }

    @Override
    public RecipeType<IcfToolingJeiRecipe> getRecipeType() {
        return HbmJeiPlugin.ICF_TOOLING;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.reinhardtshbm.icf_tooling");
    }

    @Override
    public IDrawable getBackground() {
        return this.canvas;
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, IcfToolingJeiRecipe recipe, IFocusGroup focuses) {
        builder.addInputSlot(8, 18).addItemStack(recipe.input().copy());
        for (int index = 0; index < recipe.materials().size(); index++) {
            builder.addInputSlot(38, 9 + index * 18).addItemStacks(recipe.materials().get(index));
        }
        builder.addSlot(RecipeIngredientRole.CATALYST, 70, 18).addItemStack(recipe.tool().copy());
        builder.addOutputSlot(131, 18).addItemStack(recipe.output().copy());
    }

    @Override
    public void draw(IcfToolingJeiRecipe recipe,
                     mezz.jei.api.gui.ingredient.IRecipeSlotsView recipeSlotsView,
                     GuiGraphics guiGraphics, double mouseX, double mouseY) {
        this.arrow.draw(guiGraphics, 101, 19);
    }
}
