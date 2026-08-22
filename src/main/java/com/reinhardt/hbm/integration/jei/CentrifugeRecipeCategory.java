package com.reinhardt.hbm.integration.jei;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.recipe.CentrifugeRecipe;
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

public class CentrifugeRecipeCategory implements IRecipeCategory<RecipeHolder<CentrifugeRecipe>> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/gui_centrifuge.png");

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawableAnimated progress;

    public CentrifugeRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 0, 0, 176, 86);
        this.icon = helper.createDrawableItemStack(new ItemStack(HbmBlocks.MACHINE_CENTRIFUGE.get()));

        IDrawableStatic progressStatic = helper.createDrawable(TEXTURE, 176, 35, 12, 36);
        this.progress = helper.createAnimatedDrawable(progressStatic, 80, IDrawableAnimated.StartDirection.TOP, false);
    }

    @Override
    public RecipeType<RecipeHolder<CentrifugeRecipe>> getRecipeType() {
        return HbmJeiPlugin.CENTRIFUGE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.reinhardtshbm.centrifuge");
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<CentrifugeRecipe> holder, IFocusGroup focuses) {
        CentrifugeRecipe recipe = holder.value();
        List<ItemStack> inputs = Arrays.stream(recipe.ingredient().getItems())
                .map(ItemStack::copy)
                .peek(stack -> stack.setCount(recipe.inputCount()))
                .toList();
        builder.addInputSlot(36, 50).addItemStacks(inputs);

        for (int index = 0; index < recipe.results().size(); index++) {
            builder.addOutputSlot(63 + index * 20, 50)
                    .addItemStack(recipe.results().get(index).copy());
        }
    }

    @Override
    public void draw(RecipeHolder<CentrifugeRecipe> recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        this.progress.draw(guiGraphics, 65, 14);
    }
}
