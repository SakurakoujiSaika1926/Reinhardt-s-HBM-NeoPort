package com.reinhardt.hbm.integration.jei;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.recipe.PyroOvenRecipe;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmFluids;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
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

import java.util.Arrays;
import java.util.List;

/** JEI layout taken directly from GUIPyroOven's 176x204 upper processing panel. */
public final class PyroOvenRecipeCategory implements IRecipeCategory<RecipeHolder<PyroOvenRecipe>> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/processing/gui_pyrooven.png");
    private static final int TANK_CAPACITY = 24_000;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawableAnimated progress;

    public PyroOvenRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 0, 0, 176, 96);
        this.icon = helper.createDrawableItemStack(new ItemStack(HbmBlocks.MACHINE_PYROOVEN.get()));
        IDrawableStatic staticProgress = helper.createDrawable(TEXTURE, 176, 0, 27, 12);
        this.progress = helper.createAnimatedDrawable(staticProgress, 200, IDrawableAnimated.StartDirection.LEFT, false);
    }

    @Override
    public RecipeType<RecipeHolder<PyroOvenRecipe>> getRecipeType() {
        return HbmJeiPlugin.PYRO_OVEN;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.reinhardtshbm.machine_pyrooven");
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<PyroOvenRecipe> holder, IFocusGroup focuses) {
        PyroOvenRecipe recipe = holder.value();
        if (!recipe.itemInput().isEmpty()) {
            List<ItemStack> inputs = Arrays.stream(recipe.itemInput().ingredient().getItems())
                    .map(ItemStack::copy)
                    .peek(stack -> stack.setCount(recipe.itemInput().count()))
                    .toList();
            builder.addInputSlot(35, 45).addItemStacks(inputs);
        }
        if (!recipe.fluidInput().isEmpty()) {
            addFluid(builder.addInputSlot(8, 18), recipe.fluidInput().fluid(), recipe.fluidInput().amount());
        }
        if (!recipe.itemOutput().isEmpty()) {
            builder.addOutputSlot(89, 45).addItemStack(recipe.itemOutput().copy());
        }
        if (!recipe.fluidOutput().isEmpty()) {
            addFluid(builder.addOutputSlot(116, 18), recipe.fluidOutput().fluid(), recipe.fluidOutput().amount());
        }
    }

    @Override
    public void draw(RecipeHolder<PyroOvenRecipe> recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        this.progress.draw(guiGraphics, 57, 47);
    }

    private static void addFluid(IRecipeSlotBuilder slot, com.reinhardt.hbm.fluid.HbmFluidDefinition definition, int amount) {
        Fluid fluid = HbmFluids.toNeoFluid(definition);
        if (fluid == Fluids.EMPTY || amount <= 0) {
            return;
        }
        slot.setFluidRenderer(TANK_CAPACITY, false, 16, 52).addFluidStack(fluid, amount);
        HbmJeiFluidTooltips.addTo(slot);
    }
}
