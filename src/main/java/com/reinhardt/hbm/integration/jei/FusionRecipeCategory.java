package com.reinhardt.hbm.integration.jei;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.recipe.FusionRecipe;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.util.HbmFluidTooltip;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public class FusionRecipeCategory implements IRecipeCategory<RecipeHolder<FusionRecipe>> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/nei/gui_nei_fusion.png");
    private static final int TANK_CAPACITY = 1_000;

    private final IDrawable background;
    private final IDrawable icon;

    public FusionRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 5, 11, 166, 65);
        this.icon = helper.createDrawableItemStack(new ItemStack(HbmBlocks.FUSION_TORUS.get()));
    }

    @Override
    public RecipeType<RecipeHolder<FusionRecipe>> getRecipeType() {
        return HbmJeiPlugin.FUSION;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.reinhardtshbm.fusion_torus");
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<FusionRecipe> holder, IFocusGroup focuses) {
        FusionRecipe recipe = holder.value();
        int[][] inputPos = inputPositions(recipe.inputFluids().size());
        for (int i = 0; i < recipe.inputFluids().size(); i++) {
            FusionRecipe.FusionFluidStack stack = recipe.inputFluids().get(i);
            addFluid(builder.addInputSlot(inputPos[i][0], inputPos[i][1]), stack.fluid(), stack.amount());
        }

        int outputCount = recipe.outputFluids().size() + (recipe.outputItem().isPresent() ? 1 : 0);
        int[][] outputPos = outputPositions(outputCount);
        int index = 0;
        if (recipe.outputItem().isPresent()) {
            builder.addOutputSlot(outputPos[index][0], outputPos[index][1]).addItemStack(recipe.outputItem().get().copy());
            index++;
        }
        for (FusionRecipe.FusionFluidStack stack : recipe.outputFluids()) {
            addFluid(builder.addOutputSlot(outputPos[index][0], outputPos[index][1]), stack.fluid(), stack.amount());
            index++;
        }
    }

    @Override
    public void draw(RecipeHolder<FusionRecipe> holder, mezz.jei.api.gui.ingredient.IRecipeSlotsView recipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
        FusionRecipe recipe = holder.value();
        var font = Minecraft.getInstance().font;
        ItemStack torus = new ItemStack(HbmBlocks.FUSION_TORUS.get());
        graphics.renderItem(torus, 75, 31);
        graphics.drawString(font, "KyU " + HbmFluidTooltip.shortNumber(recipe.ignitionTemp()), 72, 8, 0x404040, false);
        graphics.drawString(font, "TU " + HbmFluidTooltip.shortNumber(recipe.outputTemp()), 72, 51, 0x404040, false);
        graphics.drawString(font, recipe.power() + " HE/t", 71, 20, 0x404040, false);
    }

    private static void addFluid(IRecipeSlotBuilder slot, HbmFluidDefinition definition, int amount) {
        Fluid fluid = HbmFluids.toNeoFluid(definition);
        if (fluid == Fluids.EMPTY || amount <= 0) {
            return;
        }
        slot.setFluidRenderer(TANK_CAPACITY, false, 16, 16)
                .addFluidStack(fluid, amount);
        HbmJeiFluidTooltips.addTo(slot);
    }

    private static int[][] inputPositions(int count) {
        return switch (count) {
            case 1 -> new int[][]{{48, 24}};
            case 2 -> new int[][]{{30, 24}, {48, 24}};
            case 3 -> new int[][]{{12, 24}, {30, 24}, {48, 24}};
            default -> new int[0][0];
        };
    }

    private static int[][] outputPositions(int count) {
        return switch (count) {
            case 1 -> new int[][]{{102, 24}};
            case 2 -> new int[][]{{102, 24}, {120, 24}};
            case 3 -> new int[][]{{102, 24}, {120, 24}, {138, 24}};
            default -> new int[0][0];
        };
    }
}
