package com.reinhardt.hbm.integration.jei;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.recipe.CombinationOvenRecipe;
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

public class CombinationOvenRecipeCategory implements IRecipeCategory<RecipeHolder<CombinationOvenRecipe>> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/processing/gui_furnace_combination.png");
    private static final int TANK_CAPACITY = 1_000;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawableAnimated progressBar;
    private final IDrawableAnimated heatBar;

    public CombinationOvenRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 0, 0, 176, 96);
        this.icon = helper.createDrawableItemStack(new ItemStack(HbmBlocks.FURNACE_COMBINATION.get()));

        IDrawableStatic progress = helper.createDrawable(TEXTURE, 176, 0, 38, 5);
        this.progressBar = helper.createAnimatedDrawable(progress, 200, IDrawableAnimated.StartDirection.LEFT, false);

        IDrawableStatic heat = helper.createDrawable(TEXTURE, 176, 5, 37, 5);
        this.heatBar = helper.createAnimatedDrawable(heat, 200, IDrawableAnimated.StartDirection.LEFT, false);
    }

    @Override
    public RecipeType<RecipeHolder<CombinationOvenRecipe>> getRecipeType() {
        return HbmJeiPlugin.COMBINATION_OVEN;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.reinhardtshbm.furnace_combination");
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<CombinationOvenRecipe> holder, IFocusGroup focuses) {
        CombinationOvenRecipe recipe = holder.value();
        builder.addInputSlot(26, 36)
                .addIngredients(recipe.input());

        if (recipe.hasItemOutput()) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, 89, 36)
                    .addItemStack(recipe.output().copy());
        }
        if (recipe.hasFluid()) {
            addFluid(builder.addOutputSlot(118, 18), recipe.fluid().fluid(), recipe.fluid().amount());
        }
    }

    @Override
    public void draw(RecipeHolder<CombinationOvenRecipe> recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        this.progressBar.draw(guiGraphics, 45, 37);
        this.heatBar.draw(guiGraphics, 45, 46);
    }

    private static void addFluid(mezz.jei.api.gui.builder.IRecipeSlotBuilder slot, com.reinhardt.hbm.fluid.HbmFluidDefinition definition, int amount) {
        Fluid fluid = HbmFluids.toNeoFluid(definition);
        if (fluid == Fluids.EMPTY) {
            return;
        }
        slot.setFluidRenderer(TANK_CAPACITY, false, 16, 52)
                .addFluidStack(fluid, amount);
        HbmJeiFluidTooltips.addTo(slot);
    }
}
