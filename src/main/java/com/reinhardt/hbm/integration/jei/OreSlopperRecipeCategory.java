package com.reinhardt.hbm.integration.jei;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmFluids;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/** Exact layout of the 1.7.10 OreSlopperHandler/NEIUniversalHandler page. */
public final class OreSlopperRecipeCategory implements IRecipeCategory<OreSlopperJeiRecipe> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/nei/gui_nei.png");
    private static final int[][] OUTPUT_POSITIONS = {
            {102, 6}, {120, 6}, {102, 24}, {120, 24}, {102, 42}, {120, 42}, {138, 24}
    };

    private final IDrawable background;
    private final IDrawable icon;

    public OreSlopperRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 5, 11, 166, 65);
        this.icon = helper.createDrawableItemStack(new ItemStack(HbmBlocks.MACHINE_ORE_SLOPPER.get()));
    }

    @Override
    public RecipeType<OreSlopperJeiRecipe> getRecipeType() {
        return HbmJeiPlugin.ORE_SLOPPER;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.reinhardtshbm.machine_ore_slopper");
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
    public void setRecipe(IRecipeLayoutBuilder builder, OreSlopperJeiRecipe recipe, IFocusGroup focuses) {
        addFluid(builder.addInputSlot(48, 24), recipe.water(), recipe.fluidAmount());
        builder.addInputSlot(30, 24).addItemStack(recipe.bedrockOreBase().copy());
        builder.addSlot(RecipeIngredientRole.CATALYST, 75, 31)
                .addItemStack(new ItemStack(HbmBlocks.MACHINE_ORE_SLOPPER.get()));

        for (int i = 0; i < recipe.oreOutputs().size(); i++) {
            int[] position = OUTPUT_POSITIONS[i];
            builder.addOutputSlot(position[0], position[1]).addItemStack(recipe.oreOutputs().get(i).copy());
        }
        int[] slopPosition = OUTPUT_POSITIONS[recipe.oreOutputs().size()];
        addFluid(builder.addOutputSlot(slopPosition[0], slopPosition[1]), recipe.slop(), recipe.fluidAmount());
    }

    @Override
    public void draw(OreSlopperJeiRecipe recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView recipeSlotsView,
                     GuiGraphics guiGraphics, double mouseX, double mouseY) {
        drawSlot(guiGraphics, 48, 24);
        drawSlot(guiGraphics, 30, 24);
        for (int[] position : OUTPUT_POSITIONS) {
            drawSlot(guiGraphics, position[0], position[1]);
        }
        guiGraphics.blit(TEXTURE, 74, 14, 59, 87, 18, 36, 256, 256);
    }

    private static void drawSlot(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.blit(TEXTURE, x - 1, y - 1, 5, 87, 18, 18, 256, 256);
    }

    private static void addFluid(IRecipeSlotBuilder slot, HbmFluidDefinition definition, int amount) {
        Fluid fluid = HbmFluids.toNeoFluid(definition);
        if (fluid == Fluids.EMPTY || amount <= 0) {
            return;
        }
        slot.setFluidRenderer(amount, false, 16, 16).addFluidStack(fluid, amount);
        HbmJeiFluidTooltips.addTo(slot);
    }
}
