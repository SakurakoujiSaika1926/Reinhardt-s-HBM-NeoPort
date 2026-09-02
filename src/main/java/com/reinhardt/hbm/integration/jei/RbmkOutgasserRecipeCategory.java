package com.reinhardt.hbm.integration.jei;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.item.FluidIconItem;
import com.reinhardt.hbm.recipe.RbmkOutgasserRecipe;
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
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.Arrays;
import java.util.List;

public class RbmkOutgasserRecipeCategory implements IRecipeCategory<RecipeHolder<RbmkOutgasserRecipe>> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/nei/gui_nei.png");

    private final IDrawable background;
    private final IDrawable icon;

    public RbmkOutgasserRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 5, 11, 166, 65);
        this.icon = helper.createDrawableItemStack(new ItemStack(HbmBlocks.RBMK_OUTGASSER.get()));
    }

    @Override
    public RecipeType<RecipeHolder<RbmkOutgasserRecipe>> getRecipeType() {
        return HbmJeiPlugin.RBMK_OUTGASSER;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.reinhardtshbm.rbmk_outgasser");
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<RbmkOutgasserRecipe> holder, IFocusGroup focuses) {
        RbmkOutgasserRecipe recipe = holder.value();
        List<ItemStack> inputs = Arrays.stream(recipe.ingredient().getItems())
                .map(ItemStack::copy)
                .toList();
        builder.addInputSlot(48, 24).addItemStacks(inputs);

        builder.addSlot(RecipeIngredientRole.CATALYST, 75, 31)
                .addItemStack(new ItemStack(HbmBlocks.RBMK_OUTGASSER.get()));

        int outputIndex = 0;
        if (recipe.hasItemOutput()) {
            builder.addOutputSlot(102 + outputIndex++ * 18, 24)
                    .addItemStack(recipe.itemOutput().copy());
        }
        if (recipe.hasFluidOutput()) {
            builder.addOutputSlot(102 + outputIndex * 18, 24)
                    .addItemStack(FluidIconItem.forFluid(
                            recipe.fluidOutput().fluid(), recipe.fluidOutput().amount(), 0));
        }
    }

    @Override
    public void draw(RecipeHolder<RbmkOutgasserRecipe> holder, mezz.jei.api.gui.ingredient.IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        RbmkOutgasserRecipe recipe = holder.value();
        drawSlotFrame(guiGraphics, 48, 24);
        drawSlotFrame(guiGraphics, 75, 31);

        int outputCount = (recipe.hasItemOutput() ? 1 : 0) + (recipe.hasFluidOutput() ? 1 : 0);
        for (int index = 0; index < outputCount; index++) {
            drawSlotFrame(guiGraphics, 102 + index * 18, 24);
        }
    }

    private static void drawSlotFrame(GuiGraphics guiGraphics, int x, int y) {
        if (x == 75 && y == 31) {
            guiGraphics.blit(TEXTURE, 74, 14, 59, 87, 18, 36, 256, 256);
        } else {
            guiGraphics.blit(TEXTURE, x - 1, y - 1, 5, 87, 18, 18, 256, 256);
        }
    }
}
