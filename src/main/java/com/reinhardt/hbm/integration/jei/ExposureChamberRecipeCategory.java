package com.reinhardt.hbm.integration.jei;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.recipe.ExposureChamberRecipe;
import com.reinhardt.hbm.registry.HbmBlocks;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.Arrays;
import java.util.List;

public class ExposureChamberRecipeCategory implements IRecipeCategory<RecipeHolder<ExposureChamberRecipe>> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/processing/gui_exposure_chamber.png");

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawableAnimated powerBar;
    private final IDrawableAnimated progressBar;

    public ExposureChamberRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 0, 0, 176, 104);
        this.icon = helper.createDrawableItemStack(new ItemStack(HbmBlocks.MACHINE_EXPOSURE_CHAMBER.get()));
        IDrawableStatic power = helper.createDrawable(TEXTURE, 176, 0, 16, 52);
        this.powerBar = helper.createAnimatedDrawable(power, 100, IDrawableAnimated.StartDirection.TOP, true);
        IDrawableStatic progress = helper.createDrawable(TEXTURE, 192, 0, 42, 10);
        this.progressBar = helper.createAnimatedDrawable(progress, 100, IDrawableAnimated.StartDirection.LEFT, false);
    }

    @Override
    public RecipeType<RecipeHolder<ExposureChamberRecipe>> getRecipeType() {
        return HbmJeiPlugin.EXPOSURE_CHAMBER;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.reinhardtshbm.machine_exposure_chamber");
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<ExposureChamberRecipe> holder, IFocusGroup focuses) {
        ExposureChamberRecipe recipe = holder.value();
        builder.addInputSlot(8, 18).addItemStacks(stacks(recipe.particle().getItems()));
        builder.addInputSlot(80, 36).addItemStacks(stacks(recipe.ingredient().getItems()));
        builder.addOutputSlot(116, 36).addItemStack(recipe.output().copy());
    }

    @Override
    public void draw(RecipeHolder<ExposureChamberRecipe> recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        this.powerBar.draw(guiGraphics, 152, 18);
        this.progressBar.draw(guiGraphics, 36, 39);
    }

    private static List<ItemStack> stacks(ItemStack[] stacks) {
        return Arrays.stream(stacks).map(ItemStack::copy).toList();
    }
}
