package com.reinhardt.hbm.integration.jei;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.recipe.anvil.AnvilSmithingRecipe;
import com.reinhardt.hbm.registry.HbmBlocks;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
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

public class AnvilSmithingRecipeCategory implements IRecipeCategory<AnvilSmithingRecipe> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/jei/gui_nei_smithing.png");

    private final IDrawable background;
    private final IDrawable icon;

    public AnvilSmithingRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 5, 11, 166, 65);
        this.icon = helper.createDrawableItemStack(new ItemStack(HbmBlocks.ANVIL_STEEL.get()));
    }

    @Override
    public RecipeType<AnvilSmithingRecipe> getRecipeType() {
        return HbmJeiPlugin.ANVIL_SMITHING;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.reinhardtshbm.anvil.smithing");
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
    public void setRecipe(IRecipeLayoutBuilder builder, AnvilSmithingRecipe recipe, IFocusGroup focuses) {
        builder.addInputSlot(38, 23).addItemStacks(recipe.left().displayStacks());
        builder.addInputSlot(74, 23).addItemStacks(recipe.right().displayStacks());
        builder.addOutputSlot(110, 23).addItemStack(recipe.displayOutput());
    }

    @Override
    public void draw(AnvilSmithingRecipe recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        guiGraphics.drawString(
                Minecraft.getInstance().font,
                Component.translatable("tooltip.reinhardtshbm.anvil.tier", recipe.tier()),
                52,
                43,
                0x404040,
                false
        );
    }
}
