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

public final class IcfConstructionRecipeCategory implements IRecipeCategory<IcfConstructionJeiRecipe> {
    private final IDrawable canvas;
    private final IDrawable icon;
    private final IDrawable arrow;

    public IcfConstructionRecipeCategory(IGuiHelper helper) {
        this.canvas = helper.createBlankDrawable(166, 54);
        this.icon = helper.createDrawableItemStack(new ItemStack(HbmBlocks.STRUCT_ICF_CORE.get()));
        this.arrow = helper.getRecipeArrow();
    }

    @Override
    public RecipeType<IcfConstructionJeiRecipe> getRecipeType() {
        return HbmJeiPlugin.ICF_CONSTRUCTION;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.reinhardtshbm.icf_construction");
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
    public void setRecipe(IRecipeLayoutBuilder builder, IcfConstructionJeiRecipe recipe, IFocusGroup focuses) {
        for (int index = 0; index < recipe.materials().size(); index++) {
            int x = 4 + index % 3 * 18;
            int y = 5 + index / 3 * 18;
            builder.addInputSlot(x, y).addItemStacks(recipe.materials().get(index));
        }
        builder.addSlot(RecipeIngredientRole.CATALYST, 66, 5).addItemStack(recipe.tools().get(0).copy());
        builder.addSlot(RecipeIngredientRole.CATALYST, 66, 23).addItemStack(recipe.tools().get(1).copy());
        builder.addSlot(RecipeIngredientRole.CATALYST, 94, 14).addItemStack(recipe.core().copy());
        builder.addOutputSlot(147, 14).addItemStack(recipe.result().copy());
    }

    @Override
    public void draw(IcfConstructionJeiRecipe recipe,
                     mezz.jei.api.gui.ingredient.IRecipeSlotsView recipeSlotsView,
                     GuiGraphics guiGraphics, double mouseX, double mouseY) {
        this.arrow.draw(guiGraphics, 119, 15);
    }
}
