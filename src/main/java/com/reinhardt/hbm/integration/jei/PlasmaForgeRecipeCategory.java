package com.reinhardt.hbm.integration.jei;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.item.BlueprintItem;
import com.reinhardt.hbm.recipe.PlasmaForgeRecipe;
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

import java.util.Arrays;
import java.util.List;

public class PlasmaForgeRecipeCategory implements IRecipeCategory<RecipeHolder<PlasmaForgeRecipe>> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/nei/gui_nei.png");

    private final IDrawable background;
    private final IDrawable icon;

    public PlasmaForgeRecipeCategory(IGuiHelper helper) {
        // 1.7.10 PlasmaForgeRecipeHandler extends AssemblyMachineRecipeHandler,
        // so its recipe view is the compact generic NEI panel. The forge's
        // 176x145 machine GUI contains live gauges and inventory-only slots;
        // using it as a JEI background makes recipe text overlap those gauges.
        this.background = helper.createDrawable(TEXTURE, 5, 11, 166, 65);
        this.icon = helper.createDrawableItemStack(new ItemStack(HbmBlocks.FUSION_PLASMA_FORGE.get()));
    }

    @Override
    public RecipeType<RecipeHolder<PlasmaForgeRecipe>> getRecipeType() {
        return HbmJeiPlugin.PLASMA_FORGE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.reinhardtshbm.fusion_plasma_forge");
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<PlasmaForgeRecipe> holder, IFocusGroup focuses) {
        PlasmaForgeRecipe recipe = holder.value();
        int inputCount = recipe.inputItems().size() + recipe.inputFluids().size();
        int[][] inputPositions = inputPositions(inputCount);
        int inputOffset = inputOffset(inputCount);
        int inputIndex = 0;

        for (PlasmaForgeRecipe.CountedIngredient ingredient : recipe.inputItems()) {
            List<ItemStack> stacks = Arrays.stream(ingredient.ingredient().getItems())
                    .map(ItemStack::copy)
                    .peek(stack -> stack.setCount(ingredient.count()))
                    .toList();
            int[] position = inputPositions[inputIndex++];
            builder.addInputSlot(position[0] + inputOffset, position[1]).addItemStacks(stacks);
        }

        for (PlasmaForgeRecipe.PlasmaFluidStack fluid : recipe.inputFluids()) {
            int[] position = inputPositions[inputIndex++];
            addFluid(builder.addInputSlot(position[0] + inputOffset, position[1]), fluid);
        }

        int machineOffset = machineOffset(inputCount);
        List<String> blueprintPools = BlueprintItem.activeRecipePools(recipe.blueprintPools());
        if (!blueprintPools.isEmpty()) {
            builder.addSlot(RecipeIngredientRole.CATALYST, 75 + machineOffset, 10)
                    .addItemStacks(blueprintPools.stream().map(BlueprintItem::stackFor).toList());
        }
        builder.addSlot(RecipeIngredientRole.CATALYST, 75 + machineOffset, blueprintPools.isEmpty() ? 31 : 38)
                .addItemStack(new ItemStack(HbmBlocks.FUSION_PLASMA_FORGE.get()));

        builder.addOutputSlot(102 + machineOffset, 24).addItemStack(recipe.result().copy());
    }

    @Override
    public void draw(RecipeHolder<PlasmaForgeRecipe> holder,
                     mezz.jei.api.gui.ingredient.IRecipeSlotsView recipeSlotsView,
                     GuiGraphics graphics, double mouseX, double mouseY) {
        PlasmaForgeRecipe recipe = holder.value();
        int inputCount = recipe.inputItems().size() + recipe.inputFluids().size();
        int inputOffset = inputOffset(inputCount);
        for (int[] position : inputPositions(inputCount)) {
            drawSlotFrame(graphics, position[0] + inputOffset, position[1]);
        }

        int machineOffset = machineOffset(inputCount);
        drawSlotFrame(graphics, 102 + machineOffset, 24);
        if (BlueprintItem.activeRecipePools(recipe.blueprintPools()).isEmpty()) {
            graphics.blit(TEXTURE, 74 + machineOffset, 14, 59, 87, 18, 36, 256, 256);
        } else {
            graphics.blit(TEXTURE, 74 + machineOffset, 7, 77, 87, 18, 50, 256, 256);
        }

        var font = Minecraft.getInstance().font;
        String duration = HbmFluidTooltip.shortNumber(recipe.duration()) + " ticks";
        drawRightAligned(graphics, font, duration, 164, 45, 0x404040);

        boolean showPower = System.currentTimeMillis() % 2_000L < 1_000L;
        String status = showPower
                ? HbmFluidTooltip.shortNumber(recipe.power()) + " HE/t"
                : HbmFluidTooltip.shortNumber(recipe.ignitionTemp()) + " TU/t";
        drawRightAligned(graphics, font, status, 164, 57, showPower ? 0x404040 : 0xA000A0);
    }

    private static void addFluid(IRecipeSlotBuilder slot, PlasmaForgeRecipe.PlasmaFluidStack stack) {
        Fluid fluid = HbmFluids.toNeoFluid(stack.type());
        if (fluid == Fluids.EMPTY || stack.amount() <= 0) {
            return;
        }
        slot.setFluidRenderer(stack.amount(), false, 16, 16)
                .addFluidStack(fluid, stack.amount());
        HbmJeiFluidTooltips.addTo(slot);
    }

    private static int inputOffset(int inputCount) {
        if (inputCount > 12) {
            return -9;
        }
        if (inputCount > 9) {
            return 18;
        }
        return 0;
    }

    private static int machineOffset(int inputCount) {
        if (inputCount > 12) {
            return 27;
        }
        if (inputCount > 9) {
            return 18;
        }
        return 0;
    }

    private static int[][] inputPositions(int count) {
        return switch (count) {
            case 0 -> new int[0][0];
            case 1 -> new int[][]{{48, 24}};
            case 2 -> new int[][]{{30, 24}, {48, 24}};
            case 3 -> new int[][]{{12, 24}, {30, 24}, {48, 24}};
            case 4 -> new int[][]{{30, 15}, {48, 15}, {30, 33}, {48, 33}};
            case 5 -> new int[][]{{12, 15}, {30, 15}, {48, 15}, {12, 33}, {30, 33}};
            case 6 -> new int[][]{{12, 15}, {30, 15}, {48, 15}, {12, 33}, {30, 33}, {48, 33}};
            default -> {
                int columns = (count + 2) / 3;
                int[][] positions = new int[count][2];
                for (int index = 0; index < count; index++) {
                    positions[index][0] = 12 + (index % columns) * 18 - (columns == 4 ? 18 : 0);
                    positions[index][1] = 6 + (index / columns) * 18;
                }
                yield positions;
            }
        };
    }

    private static void drawSlotFrame(GuiGraphics graphics, int x, int y) {
        graphics.blit(TEXTURE, x - 1, y - 1, 5, 87, 18, 18, 256, 256);
    }

    private static void drawRightAligned(GuiGraphics graphics, net.minecraft.client.gui.Font font,
                                         String text, int right, int y, int color) {
        graphics.drawString(font, text, right - font.width(text), y, color, false);
    }
}
