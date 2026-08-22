package com.reinhardt.hbm.integration.jei;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.recipe.ArcWelderRecipe;
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

public class ArcWelderRecipeCategory implements IRecipeCategory<RecipeHolder<ArcWelderRecipe>> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/processing/gui_arc_welder.png");
    private static final int TANK_CAPACITY = 1_000;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawableAnimated powerBar;
    private final IDrawableAnimated progressBar;

    public ArcWelderRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 0, 0, 176, 104);
        this.icon = helper.createDrawableItemStack(new ItemStack(HbmBlocks.MACHINE_ARC_WELDER.get()));

        IDrawableStatic power = helper.createDrawable(TEXTURE, 176, 0, 16, 52);
        this.powerBar = helper.createAnimatedDrawable(power, 100, IDrawableAnimated.StartDirection.TOP, true);

        IDrawableStatic progress = helper.createDrawable(TEXTURE, 192, 0, 33, 14);
        this.progressBar = helper.createAnimatedDrawable(progress, 100, IDrawableAnimated.StartDirection.LEFT, false);
    }

    @Override
    public RecipeType<RecipeHolder<ArcWelderRecipe>> getRecipeType() {
        return HbmJeiPlugin.ARC_WELDER;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.reinhardtshbm.arc_welder");
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<ArcWelderRecipe> holder, IFocusGroup focuses) {
        ArcWelderRecipe recipe = holder.value();
        addInputs(builder, recipe.inputs());
        recipe.fluid().ifPresent(fluid -> addFluid(builder.addInputSlot(35, 63), fluid.fluid(), fluid.amount()));
        builder.addOutputSlot(107, 36).addItemStack(recipe.result().copy());
    }

    @Override
    public void draw(RecipeHolder<ArcWelderRecipe> recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        this.powerBar.draw(guiGraphics, 152, 18);
        this.progressBar.draw(guiGraphics, 72, 28);
    }

    private static void addInputs(IRecipeLayoutBuilder builder, List<ArcWelderRecipe.CountedIngredient> ingredients) {
        for (int index = 0; index < Math.min(3, ingredients.size()); index++) {
            ArcWelderRecipe.CountedIngredient ingredient = ingredients.get(index);
            List<ItemStack> stacks = Arrays.stream(ingredient.ingredient().getItems())
                    .map(ItemStack::copy)
                    .peek(stack -> stack.setCount(ingredient.count()))
                    .toList();
            builder.addInputSlot(17 + index * 18, 36).addItemStacks(stacks);
        }
    }

    private static void addFluid(mezz.jei.api.gui.builder.IRecipeSlotBuilder slot, com.reinhardt.hbm.fluid.HbmFluidDefinition definition, int amount) {
        Fluid fluid = HbmFluids.toNeoFluid(definition);
        if (fluid == Fluids.EMPTY || amount <= 0) {
            return;
        }
        slot.setFluidRenderer(TANK_CAPACITY, false, 34, 16)
                .addFluidStack(fluid, amount);
        HbmJeiFluidTooltips.addTo(slot);
    }
}
