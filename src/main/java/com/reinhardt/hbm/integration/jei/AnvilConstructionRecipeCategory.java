package com.reinhardt.hbm.integration.jei;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.HbmAnvilBlock;
import com.reinhardt.hbm.recipe.anvil.AnvilConstructionRecipe;
import com.reinhardt.hbm.recipe.anvil.AnvilIngredient;
import com.reinhardt.hbm.recipe.anvil.AnvilOutput;
import com.reinhardt.hbm.registry.HbmBlocks;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
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

import java.util.ArrayList;
import java.util.List;

public class AnvilConstructionRecipeCategory implements IRecipeCategory<AnvilConstructionRecipe> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/jei/gui_nei_anvil.png");

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable areaSmall;
    private final IDrawable areaWide;
    private final IDrawable slot;
    private final IDrawable operationNone;
    private final IDrawable operationSmithing;
    private final IDrawable operationConstruction;
    private final IDrawable operationRecycling;

    public AnvilConstructionRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 5, 11, 166, 65);
        this.icon = helper.createDrawableItemStack(new ItemStack(HbmBlocks.ANVIL_STEEL.get()));
        this.areaSmall = helper.createDrawable(TEXTURE, 5, 87, 72, 54);
        this.areaWide = helper.createDrawable(TEXTURE, 5, 87, 108, 54);
        this.slot = helper.createDrawable(TEXTURE, 113, 105, 18, 18);
        this.operationNone = helper.createDrawable(TEXTURE, 131, 96, 18, 36);
        this.operationSmithing = helper.createDrawable(TEXTURE, 149, 96, 18, 36);
        this.operationConstruction = helper.createDrawable(TEXTURE, 167, 96, 18, 36);
        this.operationRecycling = helper.createDrawable(TEXTURE, 185, 96, 18, 36);
    }

    @Override
    public RecipeType<AnvilConstructionRecipe> getRecipeType() {
        return HbmJeiPlugin.ANVIL_CONSTRUCTION;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.reinhardtshbm.anvil.construction");
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
    public void setRecipe(IRecipeLayoutBuilder builder, AnvilConstructionRecipe recipe, IFocusGroup focuses) {
        OverlayLayout layout = layoutFor(recipe.overlay());
        int index = 0;
        for (AnvilIngredient input : recipe.inputs()) {
            builder.addInputSlot(slotX(layout.inputX(), index, layout.inputColumns()), slotY(layout.inputY(), index, layout.inputColumns()))
                    .addItemStacks(input.displayStacks());
            index++;
        }

        int outputIndex = 0;
        for (AnvilOutput output : recipe.outputs()) {
            ItemStack stack = output.stack().copy();
            builder.addOutputSlot(slotX(layout.outputX(), outputIndex, layout.outputColumns()), slotY(layout.outputY(), outputIndex, layout.outputColumns()))
                    .addItemStack(stack);
            outputIndex++;
        }

        builder.addSlot(RecipeIngredientRole.CATALYST, layout.anvilX() - 1, layout.anvilY() - 1)
                .addItemStacks(validAnvils(recipe));
    }

    @Override
    public void draw(AnvilConstructionRecipe recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        switch (recipe.overlay()) {
            case NONE -> {
                this.areaSmall.draw(guiGraphics, 2, 5);
                this.areaSmall.draw(guiGraphics, 92, 5);
                this.operationNone.draw(guiGraphics, 74, 14);
            }
            case SMITHING -> {
                this.slot.draw(guiGraphics, 47, 23);
                this.slot.draw(guiGraphics, 101, 23);
                this.operationSmithing.draw(guiGraphics, 74, 14);
            }
            case CONSTRUCTION -> {
                this.areaWide.draw(guiGraphics, 11, 5);
                this.slot.draw(guiGraphics, 137, 23);
                this.operationConstruction.draw(guiGraphics, 119, 14);
            }
            case RECYCLING -> {
                this.slot.draw(guiGraphics, 11, 23);
                this.areaWide.draw(guiGraphics, 47, 5);
                this.operationRecycling.draw(guiGraphics, 29, 14);
            }
        }
    }

    private static OverlayLayout layoutFor(AnvilConstructionRecipe.OverlayType overlay) {
        return switch (overlay) {
            case NONE -> new OverlayLayout(4, 3, 6, 4, 93, 6, 75, 31);
            case SMITHING -> new OverlayLayout(1, 48, 24, 1, 102, 24, 75, 31);
            case CONSTRUCTION -> new OverlayLayout(6, 12, 6, 1, 138, 24, 120, 31);
            case RECYCLING -> new OverlayLayout(1, 12, 24, 6, 48, 6, 30, 31);
        };
    }

    private static int slotX(int originX, int index, int columns) {
        return originX + 18 * (index % columns) - 1;
    }

    private static int slotY(int originY, int index, int columns) {
        return originY + 18 * (index / columns) - 1;
    }

    private static List<ItemStack> validAnvils(AnvilConstructionRecipe recipe) {
        List<ItemStack> stacks = new ArrayList<>();
        for (var anvil : HbmBlocks.ANVIL_BLOCKS) {
            if (anvil.get() instanceof HbmAnvilBlock hbmAnvil && recipe.isTierValid(hbmAnvil.tier())) {
                stacks.add(new ItemStack(anvil.get()));
            }
        }
        return stacks;
    }

    private record OverlayLayout(int inputColumns, int inputX, int inputY, int outputColumns, int outputX, int outputY, int anvilX, int anvilY) {
    }
}
