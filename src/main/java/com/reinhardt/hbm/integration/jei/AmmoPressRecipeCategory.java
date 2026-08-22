package com.reinhardt.hbm.integration.jei;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.recipe.AmmoPressRecipe;
import com.reinhardt.hbm.registry.HbmBlocks;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.Arrays;
import java.util.List;

public class AmmoPressRecipeCategory implements IRecipeCategory<RecipeHolder<AmmoPressRecipe>> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/processing/gui_ammo_press.png");

    private final IDrawable background;
    private final IDrawable icon;

    public AmmoPressRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 104, 8, 64, 84);
        this.icon = helper.createDrawableItemStack(new ItemStack(HbmBlocks.MACHINE_AMMO_PRESS.get()));
    }

    @Override
    public RecipeType<RecipeHolder<AmmoPressRecipe>> getRecipeType() {
        return HbmJeiPlugin.AMMO_PRESS;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.reinhardtshbm.ammo_press");
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<AmmoPressRecipe> holder, IFocusGroup focuses) {
        AmmoPressRecipe recipe = holder.value();
        for (int slot = 0; slot < 9; slot++) {
            AmmoPressRecipe.SlotIngredient required = recipe.input().get(slot);
            if (required.isEmpty()) {
                continue;
            }
            List<ItemStack> candidates = Arrays.stream(required.ingredient().orElseThrow().getItems())
                    .map(ItemStack::copy)
                    .peek(stack -> stack.setCount(required.count()))
                    .toList();
            builder.addSlot(RecipeIngredientRole.INPUT, 12 + slot % 3 * 18, 10 + slot / 3 * 18)
                    .addItemStacks(candidates);
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, 30, 64).addItemStack(recipe.result().copy());
    }
}
