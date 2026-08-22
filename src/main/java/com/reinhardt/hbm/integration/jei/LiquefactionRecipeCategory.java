package com.reinhardt.hbm.integration.jei;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.recipe.LiquefactionRecipe;
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

public class LiquefactionRecipeCategory implements IRecipeCategory<RecipeHolder<LiquefactionRecipe>> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/processing/gui_liquefactor.png");
    private static final int TANK_CAPACITY = 1_000;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawableAnimated progress;

    public LiquefactionRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 0, 0, 176, 96);
        this.icon = helper.createDrawableItemStack(new ItemStack(HbmBlocks.MACHINE_LIQUEFACTOR.get()));

        IDrawableStatic progressStatic = helper.createDrawable(TEXTURE, 192, 0, 42, 35);
        this.progress = helper.createAnimatedDrawable(progressStatic, 100, IDrawableAnimated.StartDirection.LEFT, false);
    }

    @Override
    public RecipeType<RecipeHolder<LiquefactionRecipe>> getRecipeType() {
        return HbmJeiPlugin.LIQUEFACTION;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.reinhardtshbm.machine_liquefactor");
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<LiquefactionRecipe> holder, IFocusGroup focuses) {
        LiquefactionRecipe recipe = holder.value();
        builder.addSlot(RecipeIngredientRole.INPUT, 35, 54)
                .addIngredients(recipe.ingredient());

        Fluid fluid = HbmFluids.toNeoFluid(recipe.output().fluid());
        if (fluid != Fluids.EMPTY) {
            var slot = builder.addOutputSlot(71, 36)
                    .setFluidRenderer(TANK_CAPACITY, false, 16, 52)
                    .addFluidStack(fluid, recipe.output().amount());
            HbmJeiFluidTooltips.addTo(slot);
        }
    }

    @Override
    public void draw(RecipeHolder<LiquefactionRecipe> recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        this.progress.draw(guiGraphics, 42, 17);
    }
}
