package com.reinhardt.hbm.integration.jei;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.recipe.PurexRecipe;
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

public class PurexRecipeCategory implements IRecipeCategory<RecipeHolder<PurexRecipe>> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/processing/gui_purex.png");

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawableAnimated powerBar;
    private final IDrawableAnimated progressBar;

    public PurexRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 0, 0, 176, 145);
        this.icon = helper.createDrawableItemStack(new ItemStack(HbmBlocks.MACHINE_PUREX.get()));
        IDrawableStatic power = helper.createDrawable(TEXTURE, 176, 0, 16, 61);
        this.powerBar = helper.createAnimatedDrawable(power, 100, IDrawableAnimated.StartDirection.TOP, true);
        IDrawableStatic progress = helper.createDrawable(TEXTURE, 176, 61, 70, 16);
        this.progressBar = helper.createAnimatedDrawable(progress, 100, IDrawableAnimated.StartDirection.LEFT, false);
    }

    @Override
    public RecipeType<RecipeHolder<PurexRecipe>> getRecipeType() {
        return HbmJeiPlugin.PUREX;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.reinhardtshbm.purex");
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<PurexRecipe> holder, IFocusGroup focuses) {
        PurexRecipe recipe = holder.value();
        for (int index = 0; index < recipe.inputFluids().size(); index++) {
            addFluid(builder.addInputSlot(8 + index * 18, 18), recipe.inputFluids().get(index), 52);
        }
        if (!recipe.outputFluids().isEmpty()) {
            addFluid(builder.addOutputSlot(116, 36), recipe.outputFluids().getFirst(), 52);
        }
        for (int index = 0; index < recipe.inputItems().size(); index++) {
            PurexRecipe.CountedIngredient ingredient = recipe.inputItems().get(index);
            List<ItemStack> inputs = stacks(ingredient.ingredient().getItems());
            inputs.forEach(stack -> stack.setCount(ingredient.count()));
            builder.addInputSlot(8, 90 + index * 18).addItemStacks(inputs);
        }
        for (int index = 0; index < recipe.outputItems().size(); index++) {
            PurexRecipe.PurexItemOutput output = recipe.outputItems().get(index);
            builder.addOutputSlot(80 + (index % 3) * 18, 36 + (index / 3) * 18)
                    .addItemStack(output.stack().copy())
                    .addRichTooltipCallback((view, tooltip) -> {
                        if (output.chance() < 1.0F) {
                            tooltip.add(Component.literal((int) (output.chance() * 1000.0F) / 10.0F + "%"));
                        }
                    });
        }
    }

    @Override
    public void draw(RecipeHolder<PurexRecipe> recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        this.powerBar.draw(guiGraphics, 152, 18);
        this.progressBar.draw(guiGraphics, 62, 126);
    }

    private static List<ItemStack> stacks(ItemStack[] stacks) {
        return Arrays.stream(stacks).map(ItemStack::copy).toList();
    }

    private static void addFluid(mezz.jei.api.gui.builder.IRecipeSlotBuilder slot, PurexRecipe.PurexFluidStack stack, int height) {
        Fluid fluid = HbmFluids.toNeoFluid(stack.type());
        if (fluid == Fluids.EMPTY) {
            return;
        }
        slot.setFluidRenderer(24_000, false, 16, height).addFluidStack(fluid, stack.amount());
        HbmJeiFluidTooltips.addTo(slot);
    }
}
