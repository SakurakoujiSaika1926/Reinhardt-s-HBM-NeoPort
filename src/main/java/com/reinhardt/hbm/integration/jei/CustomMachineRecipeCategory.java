package com.reinhardt.hbm.integration.jei;

import com.reinhardt.hbm.recipe.CustomMachineRecipe;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmFluids;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import java.util.Arrays;
import java.util.List;

/** Recipe-only custom-machine view. It deliberately does not reuse a machine GUI texture. */
public final class CustomMachineRecipeCategory
        implements IRecipeCategory<RecipeHolder<CustomMachineRecipe>> {
    private static final int FLUID_CAPACITY = 1_000;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable arrow;

    public CustomMachineRecipeCategory(IGuiHelper helper) {
        this.background = helper.createBlankDrawable(146, 70);
        this.icon = helper.createDrawableItemStack(new ItemStack(HbmBlocks.CUSTOM_MACHINE.get()));
        this.arrow = helper.getRecipeArrow();
    }

    @Override
    public RecipeType<RecipeHolder<CustomMachineRecipe>> getRecipeType() {
        return HbmJeiPlugin.CUSTOM_MACHINE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.reinhardtshbm.custom_machine");
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<CustomMachineRecipe> holder,
                          IFocusGroup focuses) {
        CustomMachineRecipe recipe = holder.value();
        List<ItemStack> inputs = Arrays.stream(recipe.ingredient().getItems())
                .map(ItemStack::copy)
                .peek(stack -> stack.setCount(recipe.inputCount()))
                .toList();
        builder.addInputSlot(8, 18)
                .setStandardSlotBackground()
                .addItemStacks(inputs);

        Fluid fluid = HbmFluids.toNeoFluid(recipe.inputFluid().fluid());
        if (fluid != Fluids.EMPTY) {
            var fluidSlot = builder.addInputSlot(31, 8)
                    .setFluidRenderer(FLUID_CAPACITY, false, 16, 36)
                    .addFluidStack(fluid, recipe.inputFluid().amount());
            HbmJeiFluidTooltips.addTo(fluidSlot);
            fluidSlot.addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable(
                    "info.reinhardtshbm.fluid.pressure", recipe.inputFluid().pressure())));
        }

        builder.addOutputSlot(116, 18)
                .setOutputSlotBackground()
                .addItemStack(recipe.result().copy());
    }

    @Override
    public void draw(RecipeHolder<CustomMachineRecipe> holder,
                     mezz.jei.api.gui.ingredient.IRecipeSlotsView recipeSlots,
                     GuiGraphics graphics, double mouseX, double mouseY) {
        CustomMachineRecipe recipe = holder.value();
        this.arrow.draw(graphics, 72, 19);
        var font = Minecraft.getInstance().font;
        graphics.drawString(font, Component.translatable(
                "jei.reinhardtshbm.custom_machine.duration", recipe.duration()), 7, 54, 0xFF404040, false);
        graphics.drawString(font, Component.translatable(
                "jei.reinhardtshbm.custom_machine.power", recipe.power()), 78, 54, 0xFF404040, false);
    }
}
