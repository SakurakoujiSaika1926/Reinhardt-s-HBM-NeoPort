package com.reinhardt.hbm.integration.jei;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.item.StampItem;
import com.reinhardt.hbm.recipe.PressRecipe;
import com.reinhardt.hbm.registry.HbmBlocks;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PressRecipeCategory implements IRecipeCategory<RecipeHolder<PressRecipe>> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/gui_epress.png");

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawableAnimated progressBar;

    public PressRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 36, 11, 124, 62);
        this.icon = helper.createDrawableItemStack(new ItemStack(HbmBlocks.MACHINE_EPRESS.get()));

        IDrawableStatic progress = helper.createDrawable(TEXTURE, 192, 0, 18, 16);
        this.progressBar = helper.createAnimatedDrawable(progress, 80, IDrawableAnimated.StartDirection.TOP, false);
    }

    @Override
    public RecipeType<RecipeHolder<PressRecipe>> getRecipeType() {
        return HbmJeiPlugin.PRESS;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.reinhardtshbm.press");
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<PressRecipe> holder, IFocusGroup focuses) {
        PressRecipe recipe = holder.value();
        List<ItemStack> inputs = Arrays.stream(recipe.ingredient().getItems())
                .map(ItemStack::copy)
                .peek(stack -> stack.setCount(recipe.inputCount()))
                .toList();
        builder.addInputSlot(44, 43).addItemStacks(inputs);
        builder.addSlot(RecipeIngredientRole.INPUT, 44, 7).addItemStacks(stampsFor(recipe.stamp()));
        builder.addOutputSlot(104, 25).addItemStack(recipe.result().copy());
    }

    @Override
    public void draw(RecipeHolder<PressRecipe> recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        this.progressBar.draw(guiGraphics, 43, 24);
    }

    private static List<ItemStack> stampsFor(StampItem.StampType type) {
        List<ItemStack> stacks = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack stack = new ItemStack(item);
            if (StampItem.isStampOfType(stack, type)) {
                stacks.add(stack);
            }
        }
        return stacks;
    }
}
