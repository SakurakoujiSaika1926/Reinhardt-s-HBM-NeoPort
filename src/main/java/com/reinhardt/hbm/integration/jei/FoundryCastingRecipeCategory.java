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

public class FoundryCastingRecipeCategory implements IRecipeCategory<FoundryCastingJeiRecipe> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/jei/gui_nei_foundry.png");

    private final IDrawable background;
    private final IDrawable icon;

    public FoundryCastingRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 5, 11, 166, 65);
        this.icon = helper.createDrawableItemStack(new ItemStack(HbmBlocks.FOUNDRY_MOLD.get()));
    }

    @Override
    public RecipeType<FoundryCastingJeiRecipe> getRecipeType() {
        return HbmJeiPlugin.CRUCIBLE_CASTING;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.reinhardtshbm.crucible_casting");
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
    public void setRecipe(IRecipeLayoutBuilder builder, FoundryCastingJeiRecipe recipe, IFocusGroup focuses) {
        builder.addInputSlot(47, 23)
                .addItemStack(recipe.input().copy());
        builder.addSlot(RecipeIngredientRole.CATALYST, 74, 5)
                .addItemStack(recipe.mold().copy());
        builder.addSlot(RecipeIngredientRole.CATALYST, 74, 41)
                .addItemStack(recipe.castingBlock().copy());
        builder.addOutputSlot(101, 23)
                .addItemStack(recipe.output().copy());
    }
}
