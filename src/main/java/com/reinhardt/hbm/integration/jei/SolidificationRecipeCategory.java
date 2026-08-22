package com.reinhardt.hbm.integration.jei;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.recipe.SolidificationRecipe;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmFluids;
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
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public class SolidificationRecipeCategory implements IRecipeCategory<RecipeHolder<SolidificationRecipe>> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/processing/gui_solidifier.png");
    private static final int TANK_CAPACITY = 1_000;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawableAnimated progress;

    public SolidificationRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 0, 0, 176, 96);
        this.icon = helper.createDrawableItemStack(new ItemStack(HbmBlocks.MACHINE_SOLIDIFIER.get()));

        IDrawableStatic progressStatic = helper.createDrawable(TEXTURE, 192, 0, 42, 35);
        this.progress = helper.createAnimatedDrawable(progressStatic, 100, IDrawableAnimated.StartDirection.LEFT, false);
    }

    @Override
    public RecipeType<RecipeHolder<SolidificationRecipe>> getRecipeType() {
        return HbmJeiPlugin.SOLIDIFICATION;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.reinhardtshbm.machine_solidifier");
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<SolidificationRecipe> holder, IFocusGroup focuses) {
        SolidificationRecipe recipe = holder.value();
        Fluid fluid = HbmFluids.toNeoFluid(recipe.input().fluid());
        if (fluid != Fluids.EMPTY) {
            var slot = builder.addInputSlot(35, 36)
                    .setFluidRenderer(TANK_CAPACITY, false, 16, 52)
                    .addFluidStack(fluid, recipe.input().amount());
            HbmJeiFluidTooltips.addTo(slot);
        }

        if (!recipe.output().isEmpty()) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, 71, 45)
                    .addItemStack(recipe.output().copy());
        }
    }

    @Override
    public void draw(RecipeHolder<SolidificationRecipe> recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        this.progress.draw(guiGraphics, 42, 17);
    }
}
