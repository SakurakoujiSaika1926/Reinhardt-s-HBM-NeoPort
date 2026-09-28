package com.reinhardt.hbm.integration.jei;

import com.reinhardt.hbm.recipe.RockMillRecipe;
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

public final class RockMillRecipeCategory implements IRecipeCategory<RecipeHolder<RockMillRecipe>> {
    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable arrow;

    public RockMillRecipeCategory(IGuiHelper helper) {
        this.background = helper.createBlankDrawable(166, 90);
        this.icon = helper.createDrawableItemStack(new ItemStack(HbmBlocks.MACHINE_ROCKMILL.get()));
        this.arrow = helper.getRecipeArrow();
    }

    @Override
    public RecipeType<RecipeHolder<RockMillRecipe>> getRecipeType() {
        return HbmJeiPlugin.ROCK_MILL;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.reinhardtshbm.rock_mill");
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<RockMillRecipe> holder, IFocusGroup focuses) {
        RockMillRecipe recipe = holder.value();
        List<ItemStack> inputs = Arrays.stream(recipe.input().ingredient().getItems())
                .map(ItemStack::copy)
                .peek(stack -> stack.setCount(recipe.input().count()))
                .toList();
        builder.addInputSlot(8, 8)
                .setStandardSlotBackground()
                .addItemStacks(inputs);

        int totalWeight = recipe.outputs().stream().mapToInt(RockMillRecipe.WeightedOutput::weight).sum();
        for (int index = 0; index < recipe.outputs().size(); index++) {
            RockMillRecipe.WeightedOutput output = recipe.outputs().get(index);
            double chance = output.weight() * 100.0D / totalWeight;
            builder.addOutputSlot(104 + index % 3 * 18, 8 + index / 3 * 18)
                    .setOutputSlotBackground()
                    .addItemStack(output.stack().copy())
                    .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable(
                            "jei.reinhardtshbm.rock_mill.weight",
                            String.format(Locale.ROOT, "%.1f", chance))));
        }
        recipe.inputFluid().ifPresent(stack -> addFluid(
                builder.addInputSlot(8, 40), stack, 52, 16));
        recipe.outputFluid().ifPresent(stack -> addFluid(
                builder.addOutputSlot(106, 40), stack, 52, 16));
    }

    @Override
    public void draw(RecipeHolder<RockMillRecipe> recipe,
                     mezz.jei.api.gui.ingredient.IRecipeSlotsView recipeSlots,
                     GuiGraphics graphics, double mouseX, double mouseY) {
        this.arrow.draw(graphics, 69, 9);
        Minecraft minecraft = Minecraft.getInstance();
        graphics.drawCenteredString(minecraft.font,
                Component.translatable("jei.reinhardtshbm.duration", recipe.value().duration()),
                83, 65, 0xFF404040);
        graphics.drawCenteredString(minecraft.font,
                Component.translatable("jei.reinhardtshbm.power", recipe.value().power()),
                83, 78, 0xFF404040);
    }

    private static void addFluid(mezz.jei.api.gui.builder.IRecipeSlotBuilder slot,
                                 RockMillRecipe.FluidStack stack, int width, int height) {
        Fluid fluid = HbmFluids.toNeoFluid(stack.type());
        if (fluid == Fluids.EMPTY) {
            return;
        }
        slot.setFluidRenderer(Math.max(stack.amount() * 2, 4_000), false, width, height)
                .addFluidStack(fluid, stack.amount());
        HbmJeiFluidTooltips.addTo(slot);
    }
}
