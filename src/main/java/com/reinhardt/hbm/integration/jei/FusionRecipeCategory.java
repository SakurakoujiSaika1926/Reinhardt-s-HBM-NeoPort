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
import mezz.jei.api.recipe.RecipeIngredientRole;
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
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/nei/gui_nei.png");
    private static final int TANK_CAPACITY = 1_000;

    private final IDrawable background;
    private final IDrawable icon;

    public FusionRecipeCategory(IGuiHelper helper) {
        // 1.7.10's FusionRecipeHandler inherits NEIGenericRecipeHandler and
        // therefore uses the generic NEI panel with dynamic slot and machine
        // frames.  The standalone gui_nei_fusion.png only has one baked input
        // and output slot, so using it for multi-fluid recipes visibly offsets
        // JEI slots.
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
        builder.addSlot(RecipeIngredientRole.CATALYST, 75, 31)
                .addItemStack(new ItemStack(HbmBlocks.FUSION_TORUS.get()));
    }

    @Override
    public void draw(RecipeHolder<FusionRecipe> holder, mezz.jei.api.gui.ingredient.IRecipeSlotsView recipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
        FusionRecipe recipe = holder.value();
        var font = Minecraft.getInstance().font;
        int[][] inputPos = inputPositions(recipe.inputFluids().size());
        for (int[] pos : inputPos) {
            drawSlotFrame(graphics, pos[0], pos[1]);
        }

        int outputCount = recipe.outputFluids().size() + (recipe.outputItem().isPresent() ? 1 : 0);
        int[][] outputPos = outputPositions(outputCount);
        for (int[] pos : outputPos) {
            drawSlotFrame(graphics, pos[0], pos[1]);
        }
        graphics.blit(TEXTURE, 74, 14, 59, 87, 18, 36, 256, 256);

        drawRightAligned(graphics, font, HbmFluidTooltip.shortNumber(recipe.duration()) + " ticks", 164, 45, 0x404040);
        String status = System.currentTimeMillis() % 2_000L < 1_000L
                ? recipe.power() + " HE/t"
                : HbmFluidTooltip.shortNumber(recipe.ignitionTemp()) + "Ky/t";
        drawRightAligned(graphics, font, status, 164, 57, System.currentTimeMillis() % 2_000L < 1_000L ? 0x404040 : 0xA000A0);
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
            case 4 -> new int[][]{{30, 15}, {48, 15}, {30, 33}, {48, 33}};
            case 5 -> new int[][]{{12, 15}, {30, 15}, {48, 15}, {12, 33}, {30, 33}};
            case 6 -> new int[][]{{12, 15}, {30, 15}, {48, 15}, {12, 33}, {30, 33}, {48, 33}};
            default -> new int[0][0];
        };
    }

    private static int[][] outputPositions(int count) {
        return switch (count) {
            case 1 -> new int[][]{{102, 24}};
            case 2 -> new int[][]{{102, 24}, {120, 24}};
            case 3 -> new int[][]{{102, 24}, {120, 24}, {138, 24}};
            case 4 -> new int[][]{{102, 15}, {120, 15}, {102, 33}, {120, 33}};
            case 5 -> new int[][]{{102, 15}, {120, 15}, {102, 33}, {120, 33}, {138, 24}};
            case 6 -> new int[][]{{102, 6}, {120, 6}, {102, 24}, {120, 24}, {102, 42}, {120, 42}};
            case 7 -> new int[][]{{102, 6}, {120, 6}, {102, 24}, {120, 24}, {102, 42}, {120, 42}, {138, 24}};
            case 8 -> new int[][]{{102, 6}, {120, 6}, {102, 24}, {120, 24}, {102, 42}, {120, 42}, {138, 24}, {138, 42}};
            default -> new int[0][0];
        };
    }

    private static void drawSlotFrame(GuiGraphics graphics, int x, int y) {
        graphics.blit(TEXTURE, x - 1, y - 1, 5, 87, 18, 18, 256, 256);
    }

    private static void drawRightAligned(GuiGraphics graphics, net.minecraft.client.gui.Font font, String text, int right, int y, int color) {
        graphics.drawString(font, text, right - font.width(text), y, color, false);
    }
}
