package com.reinhardt.hbm.integration.jei;

import com.reinhardt.hbm.ReinhardtsHBM;
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

/** Restores the 1.7.10 PWRRecipeHandler input -> hot-fuel view. */
public final class PwrRecipeCategory implements IRecipeCategory<PwrJeiRecipe> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/nei/gui_nei.png");

    private final IDrawable background;
    private final IDrawable icon;

    public PwrRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 5, 11, 166, 65);
        this.icon = helper.createDrawableItemStack(new ItemStack(HbmBlocks.PWR_CONTROLLER.get()));
    }

    @Override
    public RecipeType<PwrJeiRecipe> getRecipeType() {
        return HbmJeiPlugin.PWR;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.reinhardtshbm.pwr");
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
    public void setRecipe(IRecipeLayoutBuilder builder, PwrJeiRecipe recipe, IFocusGroup focuses) {
        builder.addInputSlot(48, 24).addItemStack(recipe.input().copy());
        builder.addSlot(RecipeIngredientRole.CATALYST, 75, 31)
                .addItemStack(new ItemStack(HbmBlocks.PWR_CONTROLLER.get()));
        builder.addOutputSlot(102, 24).addItemStack(recipe.output().copy());
    }
}
