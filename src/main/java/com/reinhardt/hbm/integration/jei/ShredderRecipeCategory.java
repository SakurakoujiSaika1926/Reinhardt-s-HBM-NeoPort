package com.reinhardt.hbm.integration.jei;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.recipe.ShredderRecipe;
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
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.Arrays;
import java.util.List;

public class ShredderRecipeCategory implements IRecipeCategory<RecipeHolder<ShredderRecipe>> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/jei/gui_nei_shredder.png");

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawableAnimated powerBar;
    private final IDrawableAnimated progressBar;

    public ShredderRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 5, 11, 166, 65);
        this.icon = helper.createDrawableItemStack(new ItemStack(HbmBlocks.MACHINE_SHREDDER.get()));

        IDrawableStatic power = helper.createDrawable(TEXTURE, 36, 86, 16, 52);
        this.powerBar = helper.createAnimatedDrawable(power, 480, IDrawableAnimated.StartDirection.TOP, true);

        IDrawableStatic progress = helper.createDrawable(TEXTURE, 100, 118, 24, 16);
        this.progressBar = helper.createAnimatedDrawable(progress, 48, IDrawableAnimated.StartDirection.LEFT, false);
    }

    @Override
    public RecipeType<RecipeHolder<ShredderRecipe>> getRecipeType() {
        return HbmJeiPlugin.SHREDDER;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.reinhardtshbm.shredder");
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<ShredderRecipe> holder, IFocusGroup focuses) {
        ShredderRecipe recipe = holder.value();
        List<ItemStack> inputs = Arrays.stream(recipe.ingredient().getItems())
                .map(ItemStack::copy)
                .toList();

        builder.addInputSlot(38, 23)
                .addItemStacks(inputs);
        builder.addOutputSlot(128, 23)
                .addItemStack(recipe.result().copy());

        ItemStack steelBlades = new ItemStack(com.reinhardt.hbm.registry.HbmItems.BLADES_STEEL.get());
        builder.addSlot(RecipeIngredientRole.CATALYST, 83, 5)
                .addItemStack(steelBlades);
        builder.addSlot(RecipeIngredientRole.CATALYST, 83, 41)
                .addItemStack(steelBlades.copy());
    }

    @Override
    public void draw(RecipeHolder<ShredderRecipe> recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        this.powerBar.draw(guiGraphics, 3, 6);
        this.progressBar.draw(guiGraphics, 80, 23);
    }
}
