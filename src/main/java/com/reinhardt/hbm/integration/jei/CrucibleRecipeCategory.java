package com.reinhardt.hbm.integration.jei;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.item.ScrapsItem;
import com.reinhardt.hbm.recipe.CrucibleRecipe;
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

public class CrucibleRecipeCategory implements IRecipeCategory<RecipeHolder<CrucibleRecipe>> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/jei/gui_nei_crucible.png");

    private final IDrawable background;
    private final IDrawable icon;

    public CrucibleRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 5, 11, 166, 65);
        this.icon = helper.createDrawableItemStack(new ItemStack(HbmBlocks.MACHINE_CRUCIBLE.get()));
    }

    @Override
    public RecipeType<RecipeHolder<CrucibleRecipe>> getRecipeType() {
        return HbmJeiPlugin.CRUCIBLE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.reinhardtshbm.machine_crucible");
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<CrucibleRecipe> recipe, IFocusGroup focuses) {
        CrucibleRecipe value = recipe.value();

        for (int index = 0; index < value.input().size(); index++) {
            int x = 11 + (index % 3) * 18;
            int y = 5 + (index / 3) * 18;
            builder.addInputSlot(x, y)
                    .addItemStack(ScrapsItem.create(value.input().get(index).stack(), true));
        }

        builder.addSlot(RecipeIngredientRole.CATALYST, 74, 41)
                .addItemStack(new ItemStack(HbmBlocks.MACHINE_CRUCIBLE.get()));

        for (int index = 0; index < value.output().size(); index++) {
            int x = 101 + (index % 3) * 18;
            int y = 5 + (index / 3) * 18;
            builder.addOutputSlot(x, y)
                    .addItemStack(ScrapsItem.create(value.output().get(index).stack(), true));
        }
    }
}
