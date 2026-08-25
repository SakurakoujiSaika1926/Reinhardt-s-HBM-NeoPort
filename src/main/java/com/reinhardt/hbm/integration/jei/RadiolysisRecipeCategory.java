package com.reinhardt.hbm.integration.jei;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmFluids;
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
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/** Direct layout port of RadiolysisRecipeHandler from 1.7.10. */
public final class RadiolysisRecipeCategory implements IRecipeCategory<RadiolysisJeiRecipe> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/jei/gui_nei_radiolysis.png");
    private static final int TANK_CAPACITY = 1_000;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawableAnimated progress;

    public RadiolysisRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 5, 11, 166, 65);
        this.icon = helper.createDrawableItemStack(new ItemStack(HbmBlocks.MACHINE_RADIOLYSIS.get()));
        IDrawableStatic progressStatic = helper.createDrawable(TEXTURE, 5, 87, 64, 28);
        this.progress = helper.createAnimatedDrawable(progressStatic, 60, IDrawableAnimated.StartDirection.LEFT, false);
    }

    @Override
    public RecipeType<RadiolysisJeiRecipe> getRecipeType() {
        return HbmJeiPlugin.RADIOLYSIS;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.reinhardtshbm.machine_radiolysis");
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
    public void setRecipe(IRecipeLayoutBuilder builder, RadiolysisJeiRecipe recipe, IFocusGroup focuses) {
        addFluid(builder.addInputSlot(34, 25), recipe.input(), recipe.inputAmount());
        addFluid(builder.addOutputSlot(118, 16), recipe.output1(), recipe.output1Amount());
        addFluid(builder.addOutputSlot(118, 34), recipe.output2(), recipe.output2Amount());
    }

    @Override
    public void draw(RadiolysisJeiRecipe recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView recipeSlotsView,
                     GuiGraphics guiGraphics, double mouseX, double mouseY) {
        this.progress.draw(guiGraphics, 47, 8);
    }

    private static void addFluid(mezz.jei.api.gui.builder.IRecipeSlotBuilder slot,
                                 HbmFluidDefinition definition, int amount) {
        Fluid fluid = HbmFluids.toNeoFluid(definition);
        if (fluid == Fluids.EMPTY || amount <= 0) return;
        slot.setFluidRenderer(TANK_CAPACITY, false, 16, 16).addFluidStack(fluid, amount);
        HbmJeiFluidTooltips.addTo(slot);
    }
}
