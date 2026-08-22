package com.reinhardt.hbm.integration.jei;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.recipe.RbmkOutgasserRecipe;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmFluids;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.drawable.IDrawableStatic;
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
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import java.util.Arrays;
import java.util.List;

public class RbmkOutgasserRecipeCategory implements IRecipeCategory<RecipeHolder<RbmkOutgasserRecipe>> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/reactors/gui_rbmk_outgasser.png");
    private static final int TANK_CAPACITY = 100;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawableAnimated progress;

    public RbmkOutgasserRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 0, 0, 176, 84);
        this.icon = helper.createDrawableItemStack(new ItemStack(HbmBlocks.RBMK_OUTGASSER.get()));
        IDrawableStatic progressStatic = helper.createDrawable(TEXTURE, 176, 0, 13, 13);
        this.progress = helper.createAnimatedDrawable(progressStatic, 80, IDrawableAnimated.StartDirection.LEFT, false);
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
        builder.addInputSlot(44, 34).addItemStacks(inputs);

        if (recipe.hasItemOutput()) {
            builder.addOutputSlot(116, 34).addItemStack(recipe.itemOutput().copy());
        }
        if (recipe.hasFluidOutput()) {
            Fluid fluid = HbmFluids.toNeoFluid(recipe.fluidOutput().fluid());
            if (fluid != Fluids.EMPTY) {
                builder.addSlot(RecipeIngredientRole.OUTPUT, 145, 17)
                        .setFluidRenderer(TANK_CAPACITY, false, 16, 48)
                        .addFluidStack(fluid, recipe.fluidOutput().amount());
            } else if (!recipe.fluidIcon().isEmpty()) {
                builder.addOutputSlot(145, 34).addItemStack(recipe.fluidIcon());
            }
        }
    }

    @Override
    public void draw(RecipeHolder<RbmkOutgasserRecipe> holder, mezz.jei.api.gui.ingredient.IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        this.progress.draw(guiGraphics, 77, 36);
    }
}
