package com.reinhardt.hbm.integration.jei;

import com.reinhardt.hbm.ReinhardtsHBM;
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

/** JEI view of the old SawmillHandler recipe list. */
public final class SawmillRecipeCategory implements IRecipeCategory<SawmillJeiRecipe> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/nei/gui_nei.png");

    private final IDrawable background;
    private final IDrawable icon;

    public SawmillRecipeCategory(IGuiHelper helper) {
        // NEIUniversalHandler renders the 166x65 panel at (5, 11) in gui_nei.png.
        this.background = helper.createDrawable(TEXTURE, 5, 11, 166, 65);
        this.icon = helper.createDrawableItemStack(new ItemStack(HbmBlocks.MACHINE_SAWMILL.get()));
    }

    @Override
    public RecipeType<SawmillJeiRecipe> getRecipeType() {
        return HbmJeiPlugin.SAWMILL;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.reinhardtshbm.machine_sawmill");
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
    public void setRecipe(IRecipeLayoutBuilder builder, SawmillJeiRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 48, 24)
                .addIngredients(recipe.input());
        builder.addOutputSlot(102, 24).addItemStack(recipe.result().copy());
        if (!recipe.byproduct().isEmpty()) {
            builder.addOutputSlot(120, 24).addItemStack(recipe.byproduct().copy());
        }
        builder.addSlot(RecipeIngredientRole.CATALYST, 75, 31)
                .addItemStack(new ItemStack(HbmBlocks.MACHINE_SAWMILL.get()));
    }

    @Override
    public void draw(SawmillJeiRecipe recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView recipeSlotsView,
                     GuiGraphics guiGraphics, double mouseX, double mouseY) {
        guiGraphics.blit(TEXTURE, 47, 23, 5, 87, 18, 18, 256, 256);
        guiGraphics.blit(TEXTURE, 101, 23, 5, 87, 18, 18, 256, 256);
        if (!recipe.byproduct().isEmpty()) {
            guiGraphics.blit(TEXTURE, 119, 23, 5, 87, 18, 18, 256, 256);
        }
        guiGraphics.blit(TEXTURE, 74, 14, 59, 87, 18, 36, 256, 256);
        if (recipe.byproductChance() > 0) {
            guiGraphics.drawString(
                    net.minecraft.client.Minecraft.getInstance().font,
                    Component.translatable("jei.reinhardtshbm.chance", recipe.byproductChance()),
                    101, 44, 0x404040, false
            );
        }
    }
}
