package com.reinhardt.hbm.integration.jei;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.recipe.ChemicalPlantRecipe;
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
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import java.util.Arrays;
import java.util.List;

public class ChemicalPlantRecipeCategory implements IRecipeCategory<RecipeHolder<ChemicalPlantRecipe>> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/processing/gui_chemplant.png");
    private static final int TANK_CAPACITY = 16_000;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawableAnimated powerBar;
    private final IDrawableAnimated progressBar;

    public ChemicalPlantRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 0, 0, 176, 145);
        this.icon = helper.createDrawableItemStack(new ItemStack(HbmBlocks.MACHINE_CHEMICAL_PLANT.get()));

        IDrawableStatic power = helper.createDrawable(TEXTURE, 176, 0, 16, 61);
        this.powerBar = helper.createAnimatedDrawable(power, 100, IDrawableAnimated.StartDirection.TOP, true);

        IDrawableStatic progress = helper.createDrawable(TEXTURE, 176, 61, 70, 16);
        this.progressBar = helper.createAnimatedDrawable(progress, 100, IDrawableAnimated.StartDirection.LEFT, false);
    }

    @Override
    public RecipeType<RecipeHolder<ChemicalPlantRecipe>> getRecipeType() {
        return HbmJeiPlugin.CHEMICAL_PLANT;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.reinhardtshbm.chemical_plant");
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<ChemicalPlantRecipe> holder, IFocusGroup focuses) {
        ChemicalPlantRecipe recipe = holder.value();

        for (int index = 0; index < recipe.inputFluids().size(); index++) {
            ChemicalPlantRecipe.ChemicalFluidStack stack = recipe.inputFluids().get(index);
            addFluid(builder.addInputSlot(8 + index * 18, 18), stack);
        }

        for (int index = 0; index < recipe.outputFluids().size(); index++) {
            ChemicalPlantRecipe.ChemicalFluidStack stack = recipe.outputFluids().get(index);
            addFluid(builder.addOutputSlot(80 + index * 18, 18), stack);
        }

        for (int index = 0; index < recipe.inputItems().size(); index++) {
            ChemicalPlantRecipe.CountedIngredient ingredient = recipe.inputItems().get(index);
            List<ItemStack> stacks = Arrays.stream(ingredient.ingredient().getItems())
                    .map(ItemStack::copy)
                    .peek(stack -> stack.setCount(ingredient.count()))
                    .toList();
            builder.addInputSlot(8 + index * 18, 99)
                    .addItemStacks(stacks);
        }

        for (int index = 0; index < recipe.outputItems().size(); index++) {
            builder.addOutputSlot(80 + index * 18, 99)
                    .addItemStack(recipe.outputItems().get(index).copy());
        }
    }

    @Override
    public void draw(RecipeHolder<ChemicalPlantRecipe> recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        this.powerBar.draw(guiGraphics, 152, 18);
        this.progressBar.draw(guiGraphics, 62, 126);
    }

    private static void addFluid(mezz.jei.api.gui.builder.IRecipeSlotBuilder slot, ChemicalPlantRecipe.ChemicalFluidStack stack) {
        Fluid fluid = HbmFluids.toNeoFluid(stack.type());
        if (fluid == Fluids.EMPTY) {
            return;
        }
        slot.setFluidRenderer(TANK_CAPACITY, false, 16, 34)
                .addFluidStack(fluid, stack.amount());
        HbmJeiFluidTooltips.addTo(slot);
    }
}
