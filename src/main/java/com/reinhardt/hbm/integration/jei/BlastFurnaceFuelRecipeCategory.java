package com.reinhardt.hbm.integration.jei;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.recipe.BlastFurnaceFuelRecipe;
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

public class BlastFurnaceFuelRecipeCategory implements IRecipeCategory<RecipeHolder<BlastFurnaceFuelRecipe>> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/guidifurnace.png");

    private final IDrawable background;
    private final IDrawable icon;

    public BlastFurnaceFuelRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 0, 17, 70, 54);
        this.icon = helper.createDrawableItemStack(new ItemStack(HbmBlocks.MACHINE_DIFURNACE_OFF.get()));
    }

    @Override
    public RecipeType<RecipeHolder<BlastFurnaceFuelRecipe>> getRecipeType() {
        return HbmJeiPlugin.BLAST_FURNACE_FUEL;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.reinhardtshbm.blast_furnace_fuel");
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<BlastFurnaceFuelRecipe> holder, IFocusGroup focuses) {
        builder.addInputSlot(8, 19)
                .addIngredients(holder.value().ingredient());
    }

    @Override
    public void draw(RecipeHolder<BlastFurnaceFuelRecipe> recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        guiGraphics.drawString(
                net.minecraft.client.Minecraft.getInstance().font,
                Component.translatable("jei.reinhardtshbm.blast_furnace_fuel.power", recipe.value().power()),
                29,
                24,
                0x404040,
                false
        );
    }
}
