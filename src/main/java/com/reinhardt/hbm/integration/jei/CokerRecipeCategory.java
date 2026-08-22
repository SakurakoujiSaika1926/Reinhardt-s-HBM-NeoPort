package com.reinhardt.hbm.integration.jei;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.recipe.CokerRecipe;
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

public class CokerRecipeCategory implements IRecipeCategory<RecipeHolder<CokerRecipe>> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/processing/gui_coker.png");
    private static final int TANK_CAPACITY = 1_000;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawableAnimated progressBar;
    private final IDrawableAnimated heatBar;

    public CokerRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 0, 0, 176, 96);
        this.icon = helper.createDrawableItemStack(new ItemStack(HbmBlocks.MACHINE_COKER.get()));

        IDrawableStatic progress = helper.createDrawable(TEXTURE, 176, 0, 53, 5);
        this.progressBar = helper.createAnimatedDrawable(progress, 200, IDrawableAnimated.StartDirection.LEFT, false);

        IDrawableStatic heat = helper.createDrawable(TEXTURE, 176, 5, 52, 5);
        this.heatBar = helper.createAnimatedDrawable(heat, 200, IDrawableAnimated.StartDirection.LEFT, false);
    }

    @Override
    public RecipeType<RecipeHolder<CokerRecipe>> getRecipeType() {
        return HbmJeiPlugin.COKER;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.reinhardtshbm.machine_coker");
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<CokerRecipe> holder, IFocusGroup focuses) {
        CokerRecipe recipe = holder.value();
        addFluid(builder.addInputSlot(35, 18), recipe.input().fluid(), recipe.input().amount());

        if (!recipe.output().isEmpty()) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, 97, 27)
                    .addItemStack(recipe.output().copy());
        }
        if (recipe.hasByproduct()) {
            addFluid(builder.addOutputSlot(125, 18), recipe.byproduct().fluid(), recipe.byproduct().amount());
        }
    }

    @Override
    public void draw(RecipeHolder<CokerRecipe> recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        this.progressBar.draw(guiGraphics, 61, 46);
        this.heatBar.draw(guiGraphics, 61, 55);
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
