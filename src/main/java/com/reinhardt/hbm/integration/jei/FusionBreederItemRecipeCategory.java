package com.reinhardt.hbm.integration.jei;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.recipe.FusionBreederItemRecipe;
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
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.Arrays;
import java.util.List;

public class FusionBreederItemRecipeCategory implements IRecipeCategory<RecipeHolder<FusionBreederItemRecipe>> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/reactors/gui_fusion_breeder.png");

    private final IDrawable background;
    private final IDrawable icon;

    public FusionBreederItemRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 0, 0, 176, 84);
        this.icon = helper.createDrawableItemStack(new ItemStack(HbmBlocks.FUSION_BREEDER.get()));
    }

    @Override
    public RecipeType<RecipeHolder<FusionBreederItemRecipe>> getRecipeType() {
        return HbmJeiPlugin.FUSION_BREEDER_ITEM;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.reinhardtshbm.fusion_breeder");
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<FusionBreederItemRecipe> holder, IFocusGroup focuses) {
        FusionBreederItemRecipe recipe = holder.value();
        List<ItemStack> inputs = Arrays.stream(recipe.ingredient().getItems())
                .map(ItemStack::copy)
                .toList();
        builder.addInputSlot(35, 35).addItemStacks(inputs);
        builder.addOutputSlot(125, 35).addItemStack(recipe.result().copy());
    }

    @Override
    public void draw(RecipeHolder<FusionBreederItemRecipe> holder, mezz.jei.api.gui.ingredient.IRecipeSlotsView recipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
        graphics.renderItem(new ItemStack(HbmBlocks.FUSION_BREEDER.get()), 80, 34);
        String flux = Integer.toString(holder.value().flux());
        graphics.drawString(Minecraft.getInstance().font, flux, 88 - Minecraft.getInstance().font.width(flux) / 2, 21, 0x08FF00, false);
    }
}
