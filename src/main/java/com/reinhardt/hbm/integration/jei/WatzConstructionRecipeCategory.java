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

public final class WatzConstructionRecipeCategory implements IRecipeCategory<WatzConstructionJeiRecipe> {
    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable arrow;

    public WatzConstructionRecipeCategory(IGuiHelper helper) {
        this.background = helper.createBlankDrawable(166, 62);
        this.icon = helper.createDrawableItemStack(new ItemStack(HbmBlocks.STRUCT_WATZ_CORE.get()));
        this.arrow = helper.getRecipeArrow();
    }

    @Override
    public RecipeType<WatzConstructionJeiRecipe> getRecipeType() {
        return HbmJeiPlugin.WATZ_CONSTRUCTION;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.reinhardtshbm.watz_construction");
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
    public void setRecipe(IRecipeLayoutBuilder builder, WatzConstructionJeiRecipe recipe, IFocusGroup focuses) {
        for (int i = 0; i < recipe.materials().size(); i++) {
            int x = 4 + i % 3 * 18;
            int y = 8 + i / 3 * 18;
            builder.addInputSlot(x, y).addItemStack(recipe.materials().get(i).copy());
        }
        builder.addSlot(RecipeIngredientRole.CATALYST, 74, 17).addItemStack(recipe.tool().copy());
        builder.addSlot(RecipeIngredientRole.CATALYST, 98, 17).addItemStack(recipe.core().copy());
        builder.addOutputSlot(147, 17).addItemStack(recipe.result().copy());
    }

    @Override
    public void draw(WatzConstructionJeiRecipe recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView recipeSlotsView,
                     GuiGraphics guiGraphics, double mouseX, double mouseY) {
        this.arrow.draw(guiGraphics, 121, 18);
    }
}
