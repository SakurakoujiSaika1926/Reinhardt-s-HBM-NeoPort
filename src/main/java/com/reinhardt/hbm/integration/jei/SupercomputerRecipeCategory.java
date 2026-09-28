package com.reinhardt.hbm.integration.jei;

import com.reinhardt.hbm.recipe.SupercomputerRecipe;
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
import java.util.Locale;

/** JEI recipe-only view; intentionally does not reuse the machine GUI texture. */
public final class SupercomputerRecipeCategory
        implements IRecipeCategory<RecipeHolder<SupercomputerRecipe>> {
    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable arrow;

    public SupercomputerRecipeCategory(IGuiHelper helper) {
        this.background = helper.createBlankDrawable(166, 90);
        this.icon = helper.createDrawableItemStack(new ItemStack(HbmBlocks.MACHINE_SUPERCOMPUTER.get()));
        this.arrow = helper.getRecipeArrow();
    }

    @Override
    public RecipeType<RecipeHolder<SupercomputerRecipe>> getRecipeType() {
        return HbmJeiPlugin.SUPERCOMPUTER;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.reinhardtshbm.supercomputer");
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<SupercomputerRecipe> holder,
                          IFocusGroup focuses) {
        SupercomputerRecipe recipe = holder.value();
        for (int index = 0; index < recipe.ingredients().size(); index++) {
            SupercomputerRecipe.CountedIngredient ingredient = recipe.ingredients().get(index);
            List<ItemStack> stacks = Arrays.stream(ingredient.ingredient().getItems())
                    .map(ItemStack::copy)
                    .peek(stack -> stack.setCount(ingredient.count()))
                    .toList();
            builder.addInputSlot(8 + index * 20, 8)
                    .setStandardSlotBackground()
                    .addItemStacks(stacks);
        }

        int totalWeight = recipe.outputs().stream()
                .mapToInt(SupercomputerRecipe.ChanceOutput::weight)
                .sum();
        for (int index = 0; index < recipe.outputs().size(); index++) {
            SupercomputerRecipe.ChanceOutput output = recipe.outputs().get(index);
            double chance = output.weight() * 100.0D / totalWeight;
            builder.addOutputSlot(108 + index * 20, 8)
                    .setOutputSlotBackground()
                    .addItemStack(output.stack().copy())
                    .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable(
                            "jei.reinhardtshbm.supercomputer.chance",
                            String.format(Locale.ROOT, "%.1f", chance))));
        }

        if (!recipe.inputFluids().isEmpty()) {
            addFluid(builder.addInputSlot(8, 40), recipe.inputFluids().getFirst());
        }
        if (!recipe.outputFluids().isEmpty()) {
            addFluid(builder.addOutputSlot(106, 40), recipe.outputFluids().getFirst());
        }
    }

    @Override
    public void draw(RecipeHolder<SupercomputerRecipe> holder,
                     mezz.jei.api.gui.ingredient.IRecipeSlotsView recipeSlots,
                     GuiGraphics graphics, double mouseX, double mouseY) {
        SupercomputerRecipe recipe = holder.value();
        this.arrow.draw(graphics, 70, 9);
        Minecraft minecraft = Minecraft.getInstance();
        graphics.drawCenteredString(minecraft.font,
                Component.translatable("jei.reinhardtshbm.supercomputer.duration", recipe.duration()),
                83, 65, 0xFF404040);
        graphics.drawCenteredString(minecraft.font,
                Component.translatable("jei.reinhardtshbm.supercomputer.power", recipe.power()),
                83, 78, 0xFF404040);
    }

    private static void addFluid(mezz.jei.api.gui.builder.IRecipeSlotBuilder slot,
                                 SupercomputerRecipe.FluidStack stack) {
        Fluid fluid = HbmFluids.toNeoFluid(stack.type());
        if (fluid == Fluids.EMPTY) {
            return;
        }
        slot.setFluidRenderer(Math.max(stack.amount() * 2, 4_000), false, 52, 16)
                .addFluidStack(fluid, stack.amount());
        HbmJeiFluidTooltips.addTo(slot);
    }
}
