package com.reinhardt.hbm.integration.jei;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.recipe.CyclotronRecipe;
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

public class CyclotronRecipeCategory implements IRecipeCategory<RecipeHolder<CyclotronRecipe>> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/machine/gui_cyclotron.png");

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawableAnimated powerBar;
    private final IDrawableAnimated progress;

    public CyclotronRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 0, 0, 190, 133);
        this.icon = helper.createDrawableItemStack(new ItemStack(HbmBlocks.MACHINE_CYCLOTRON.get()));
        IDrawableStatic power = helper.createDrawable(TEXTURE, 190, 0, 16, 63);
        this.powerBar = helper.createAnimatedDrawable(power, 100, IDrawableAnimated.StartDirection.TOP, true);
        IDrawableStatic progress = helper.createDrawable(TEXTURE, 206, 0, 34, 34);
        this.progress = helper.createAnimatedDrawable(progress, 100, IDrawableAnimated.StartDirection.LEFT, false);
    }

    @Override
    public RecipeType<RecipeHolder<CyclotronRecipe>> getRecipeType() {
        return HbmJeiPlugin.CYCLOTRON;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.reinhardtshbm.machine_cyclotron");
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<CyclotronRecipe> holder, IFocusGroup focuses) {
        CyclotronRecipe recipe = holder.value();
        builder.addInputSlot(11, 18).addItemStacks(stacks(recipe.particle().getItems()));
        builder.addInputSlot(101, 18).addItemStacks(stacks(recipe.input().getItems()));
        builder.addOutputSlot(131, 18).addItemStack(recipe.output().copy());
    }

    @Override
    public void draw(RecipeHolder<CyclotronRecipe> recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        this.powerBar.draw(guiGraphics, 168, 18);
        this.progress.draw(guiGraphics, 48, 27);
    }

    private static List<ItemStack> stacks(ItemStack[] stacks) {
        return Arrays.stream(stacks).map(ItemStack::copy).toList();
    }
}
