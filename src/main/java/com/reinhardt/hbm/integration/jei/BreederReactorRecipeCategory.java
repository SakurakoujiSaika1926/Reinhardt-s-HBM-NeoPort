package com.reinhardt.hbm.integration.jei;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.recipe.BreederReactorRecipe;
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

public class BreederReactorRecipeCategory implements IRecipeCategory<RecipeHolder<BreederReactorRecipe>> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/processing/gui_breeder.png");

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawableAnimated progress;

    public BreederReactorRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 0, 0, 176, 84);
        this.icon = helper.createDrawableItemStack(new ItemStack(HbmBlocks.MACHINE_REACTOR_BREEDING.get()));
        IDrawableStatic progressStatic = helper.createDrawable(TEXTURE, 176, 0, 70, 20);
        this.progress = helper.createAnimatedDrawable(progressStatic, 80, IDrawableAnimated.StartDirection.LEFT, false);
    }

    @Override
    public RecipeType<RecipeHolder<BreederReactorRecipe>> getRecipeType() {
        return HbmJeiPlugin.BREEDER_REACTOR;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.reinhardtshbm.breeder_reactor");
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<BreederReactorRecipe> holder, IFocusGroup focuses) {
        BreederReactorRecipe recipe = holder.value();
        List<ItemStack> inputs = Arrays.stream(recipe.ingredient().getItems())
                .map(ItemStack::copy)
                .toList();
        builder.addInputSlot(35, 35).addItemStacks(inputs);
        builder.addOutputSlot(125, 35).addItemStack(recipe.result().copy());
    }

    @Override
    public void draw(RecipeHolder<BreederReactorRecipe> holder, mezz.jei.api.gui.ingredient.IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        this.progress.draw(guiGraphics, 53, 32);
        String flux = Integer.toString(holder.value().flux());
        guiGraphics.drawString(net.minecraft.client.Minecraft.getInstance().font, flux, 88 - net.minecraft.client.Minecraft.getInstance().font.width(flux) / 2, 21, 0x08FF00, false);
    }
}
