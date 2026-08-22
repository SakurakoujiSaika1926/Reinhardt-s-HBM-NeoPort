package com.reinhardt.hbm.integration.jei;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.recipe.MixerRecipe;
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

public class MixerRecipeCategory implements IRecipeCategory<RecipeHolder<MixerRecipe>> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/processing/gui_mixer.png");
    private static final int INPUT_CAPACITY = 16_000;
    private static final int OUTPUT_CAPACITY = 24_000;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawableAnimated progress;

    public MixerRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 0, 0, 176, 104);
        this.icon = helper.createDrawableItemStack(new ItemStack(HbmBlocks.MACHINE_MIXER.get()));
        IDrawableStatic progressStatic = helper.createDrawable(TEXTURE, 192, 0, 53, 44);
        this.progress = helper.createAnimatedDrawable(progressStatic, 100, IDrawableAnimated.StartDirection.LEFT, false);
    }

    @Override
    public RecipeType<RecipeHolder<MixerRecipe>> getRecipeType() {
        return HbmJeiPlugin.MIXER;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.reinhardtshbm.mixer");
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<MixerRecipe> holder, IFocusGroup focuses) {
        MixerRecipe recipe = holder.value();
        recipe.input1().ifPresent(input -> addFluid(builder.addInputSlot(43, 23), input.fluid(), input.amount(), INPUT_CAPACITY, 7));
        recipe.input2().ifPresent(input -> addFluid(builder.addInputSlot(52, 23), input.fluid(), input.amount(), INPUT_CAPACITY, 7));
        recipe.solidInput().ifPresent(input -> builder.addSlot(RecipeIngredientRole.INPUT, 43, 77)
                .addIngredients(input.ingredient()));
        addFluid(builder.addOutputSlot(117, 23), recipe.output().fluid(), recipe.output().amount(), OUTPUT_CAPACITY, 16);
    }

    @Override
    public void draw(RecipeHolder<MixerRecipe> recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        this.progress.draw(guiGraphics, 62, 36);
    }

    private static void addFluid(mezz.jei.api.gui.builder.IRecipeSlotBuilder slot, com.reinhardt.hbm.fluid.HbmFluidDefinition definition, int amount, int capacity, int width) {
        Fluid fluid = HbmFluids.toNeoFluid(definition);
        if (fluid == Fluids.EMPTY) {
            return;
        }
        slot.setFluidRenderer(capacity, false, width, 52)
                .addFluidStack(fluid, amount);
        HbmJeiFluidTooltips.addTo(slot);
    }
}
