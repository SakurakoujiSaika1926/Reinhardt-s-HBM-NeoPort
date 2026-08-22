package com.reinhardt.hbm.integration.jei;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.recipe.VacuumDistillRecipe;
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

public class VacuumDistillRecipeCategory implements IRecipeCategory<RecipeHolder<VacuumDistillRecipe>> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/jei/gui_nei_vacuum_distill.png");
    private static final int TANK_CAPACITY = 100;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawableAnimated powerBar;
    private final IDrawableAnimated progressBar;

    public VacuumDistillRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 6, 15, 145, 55);
        this.icon = helper.createDrawableItemStack(new ItemStack(HbmBlocks.MACHINE_VACUUM_DISTILL.get()));

        IDrawableStatic power = helper.createDrawable(TEXTURE, 0, 86, 16, 52);
        this.powerBar = helper.createAnimatedDrawable(power, 480, IDrawableAnimated.StartDirection.TOP, true);

        IDrawableStatic progress = helper.createDrawable(TEXTURE, 16, 86, 24, 17);
        this.progressBar = helper.createAnimatedDrawable(progress, 48, IDrawableAnimated.StartDirection.LEFT, false);
    }

    @Override
    public RecipeType<RecipeHolder<VacuumDistillRecipe>> getRecipeType() {
        return HbmJeiPlugin.VACUUM_DISTILL;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.reinhardtshbm.machine_vacuum_distill");
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<VacuumDistillRecipe> holder, IFocusGroup focuses) {
        VacuumDistillRecipe recipe = holder.value();

        addFluid(builder.addInputSlot(46, 19), recipe.input().fluid(), recipe.input().amount());

        int[][] outputSlots = {
                {109, 1},
                {127, 10},
                {109, 19},
                {127, 28}
        };
        for (int index = 0; index < recipe.outputs().size(); index++) {
            VacuumDistillRecipe.FluidOutput output = recipe.outputs().get(index);
            addFluid(builder.addOutputSlot(outputSlots[index][0], outputSlots[index][1]), output.fluid(), output.amount());
        }

        if (!recipe.byproduct().isEmpty()) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, 109, 37)
                    .addItemStack(recipe.byproduct().copy());
        }
    }

    @Override
    public void draw(RecipeHolder<VacuumDistillRecipe> recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        this.powerBar.draw(guiGraphics, 2, 2);
        this.progressBar.draw(guiGraphics, 77, 20);
    }

    private static void addFluid(mezz.jei.api.gui.builder.IRecipeSlotBuilder slot, com.reinhardt.hbm.fluid.HbmFluidDefinition definition, int amount) {
        Fluid fluid = HbmFluids.toNeoFluid(definition);
        if (fluid == Fluids.EMPTY) {
            return;
        }
        slot.setFluidRenderer(TANK_CAPACITY, false, 16, 16)
                .addFluidStack(fluid, amount);
        HbmJeiFluidTooltips.addTo(slot);
    }
}

