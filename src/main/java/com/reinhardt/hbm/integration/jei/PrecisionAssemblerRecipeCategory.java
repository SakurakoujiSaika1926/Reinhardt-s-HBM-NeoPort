package com.reinhardt.hbm.integration.jei;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.item.BlueprintItem;
import com.reinhardt.hbm.recipe.PrecisionAssemblerRecipe;
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

/** JEI view of the original 3x3 input/output precision assembly interface. */
public final class PrecisionAssemblerRecipeCategory implements IRecipeCategory<RecipeHolder<PrecisionAssemblerRecipe>> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/processing/gui_precass.png");
    private static final int TANK_CAPACITY = 4_000;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawableAnimated powerBar;
    private final IDrawableAnimated progressBar;

    public PrecisionAssemblerRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 0, 0, 176, 145);
        this.icon = helper.createDrawableItemStack(new ItemStack(HbmBlocks.MACHINE_PRECASS.get()));
        this.powerBar = helper.createAnimatedDrawable(helper.createDrawable(TEXTURE, 176, 0, 16, 61), 100, IDrawableAnimated.StartDirection.TOP, true);
        this.progressBar = helper.createAnimatedDrawable(helper.createDrawable(TEXTURE, 176, 61, 70, 16), 100, IDrawableAnimated.StartDirection.LEFT, false);
    }

    @Override
    public RecipeType<RecipeHolder<PrecisionAssemblerRecipe>> getRecipeType() {
        return HbmJeiPlugin.PRECISION_ASSEMBLER;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.reinhardtshbm.machine_precass");
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<PrecisionAssemblerRecipe> holder, IFocusGroup focuses) {
        PrecisionAssemblerRecipe recipe = holder.value();
        for (int index = 0; index < recipe.ingredients().size(); index++) {
            PrecisionAssemblerRecipe.CountedIngredient ingredient = recipe.ingredients().get(index);
            List<ItemStack> stacks = Arrays.stream(ingredient.ingredient().getItems())
                    .map(ItemStack::copy)
                    .peek(stack -> stack.setCount(ingredient.count()))
                    .toList();
            builder.addInputSlot(8 + index % 3 * 18, 27 + index / 3 * 18).addItemStacks(stacks);
        }

        if (!recipe.blueprintPools().isEmpty()) {
            builder.addInputSlot(35, 126).addItemStacks(recipe.blueprintPools().stream().map(BlueprintItem::stackFor).toList());
        }
        if (!recipe.inputFluids().isEmpty()) addFluid(builder.addInputSlot(8, 99), recipe.inputFluids().getFirst());
        if (!recipe.outputFluids().isEmpty()) addFluid(builder.addOutputSlot(80, 99), recipe.outputFluids().getFirst());

        if (recipe.outputMode() == PrecisionAssemblerRecipe.OutputMode.WEIGHTED) {
            builder.addOutputSlot(80, 27).addItemStacks(recipe.outputs().stream().map(PrecisionAssemblerRecipe.ChanceOutput::stack).map(ItemStack::copy).toList());
        } else {
            for (int index = 0; index < recipe.outputs().size(); index++) {
                builder.addOutputSlot(80 + index % 3 * 18, 27 + index / 3 * 18).addItemStack(recipe.outputs().get(index).stack().copy());
            }
        }
    }

    @Override
    public void draw(RecipeHolder<PrecisionAssemblerRecipe> recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        this.powerBar.draw(graphics, 152, 18);
        this.progressBar.draw(graphics, 62, 126);
    }

    private static void addFluid(mezz.jei.api.gui.builder.IRecipeSlotBuilder slot, PrecisionAssemblerRecipe.FluidStack stack) {
        Fluid fluid = HbmFluids.toNeoFluid(stack.type());
        if (fluid == Fluids.EMPTY) return;
        slot.setFluidRenderer(TANK_CAPACITY, false, 52, 16).addFluidStack(fluid, stack.amount());
        HbmJeiFluidTooltips.addTo(slot);
    }
}
