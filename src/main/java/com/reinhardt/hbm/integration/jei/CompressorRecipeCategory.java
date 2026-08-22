package com.reinhardt.hbm.integration.jei;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.recipe.CompressorRecipe;
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
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public class CompressorRecipeCategory implements IRecipeCategory<RecipeHolder<CompressorRecipe>> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/processing/gui_compressor.png");
    private static final int TANK_CAPACITY = 16_000;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawableAnimated progress;

    public CompressorRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 0, 0, 176, 104);
        this.icon = helper.createDrawableItemStack(new ItemStack(HbmBlocks.MACHINE_COMPRESSOR.get()));
        IDrawableStatic progressStatic = helper.createDrawable(TEXTURE, 192, 0, 55, 17);
        this.progress = helper.createAnimatedDrawable(progressStatic, 100, IDrawableAnimated.StartDirection.LEFT, false);
    }

    @Override
    public RecipeType<RecipeHolder<CompressorRecipe>> getRecipeType() {
        return HbmJeiPlugin.COMPRESSOR;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.reinhardtshbm.compressor");
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<CompressorRecipe> holder, IFocusGroup focuses) {
        CompressorRecipe recipe = holder.value();
        addFluid(builder.addInputSlot(17, 18), recipe.input().fluid(), recipe.input().amount(), recipe.input().pressure());
        addFluid(builder.addOutputSlot(107, 18), recipe.output().fluid(), recipe.output().amount(), recipe.output().pressure());
    }

    @Override
    public void draw(RecipeHolder<CompressorRecipe> recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        this.progress.draw(guiGraphics, 42, 26);
        var font = Minecraft.getInstance().font;
        Component inputPressure = Component.translatable("gui.reinhardtshbm.compressor.pressure", recipe.value().input().pressure());
        Component outputPressure = Component.translatable("gui.reinhardtshbm.compressor.pressure", recipe.value().output().pressure());
        guiGraphics.drawString(font, inputPressure, 1, 73, 0x404040, false);
        guiGraphics.drawString(font, outputPressure, 91, 73, 0x404040, false);
    }

    private static void addFluid(
            mezz.jei.api.gui.builder.IRecipeSlotBuilder slot,
            com.reinhardt.hbm.fluid.HbmFluidDefinition definition,
            int amount,
            int pressure
    ) {
        Fluid fluid = HbmFluids.toNeoFluid(definition);
        if (fluid == Fluids.EMPTY) {
            return;
        }
        slot.setFluidRenderer(TANK_CAPACITY, false, 16, 52)
                .addFluidStack(fluid, amount);
        HbmJeiFluidTooltips.addTo(slot);
        slot.addRichTooltipCallback((view, tooltip) ->
                tooltip.add(Component.translatable("info.reinhardtshbm.fluid.pressure", pressure)));
    }
}
