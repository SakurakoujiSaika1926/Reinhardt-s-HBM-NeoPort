package com.reinhardt.hbm.integration.jei;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.registry.HbmBlocks;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** NeoForge JEI counterpart of the 1.7.10 RBMKWasteDecayHandler. */
public final class StorageDrumRecipeCategory implements IRecipeCategory<StorageDrumJeiRecipe> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/gui_utility.png");

    private final IDrawable background;
    private final IDrawable icon;

    public StorageDrumRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 43, 16, 90, 54);
        this.icon = helper.createDrawableItemStack(new ItemStack(HbmBlocks.MACHINE_STORAGE_DRUM.get()));
    }

    @Override
    public RecipeType<StorageDrumJeiRecipe> getRecipeType() {
        return HbmJeiPlugin.STORAGE_DRUM;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.reinhardtshbm.storage_drum");
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
    public void setRecipe(IRecipeLayoutBuilder builder, StorageDrumJeiRecipe recipe, IFocusGroup focuses) {
        builder.addInputSlot(1, 19).addItemStack(recipe.input().copy());
        builder.addOutputSlot(73, 19).addItemStack(recipe.output().copy());
    }
}
