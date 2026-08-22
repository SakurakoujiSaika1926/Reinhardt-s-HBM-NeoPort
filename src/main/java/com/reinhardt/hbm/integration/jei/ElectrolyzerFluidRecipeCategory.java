package com.reinhardt.hbm.integration.jei;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.recipe.ElectrolyzerFluidRecipe;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmFluids;
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
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public class ElectrolyzerFluidRecipeCategory implements IRecipeCategory<RecipeHolder<ElectrolyzerFluidRecipe>> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/processing/gui_electrolyser_fluid.png");
    private final IDrawable background;
    private final IDrawable icon;

    public ElectrolyzerFluidRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 0, 0, 210, 96);
        this.icon = helper.createDrawableItemStack(new ItemStack(HbmBlocks.MACHINE_ELECTROLYSER.get()));
    }

    @Override
    public RecipeType<RecipeHolder<ElectrolyzerFluidRecipe>> getRecipeType() {
        return HbmJeiPlugin.ELECTROLYZER_FLUID;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.reinhardtshbm.electrolyzer_fluid");
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<ElectrolyzerFluidRecipe> holder, IFocusGroup focuses) {
        ElectrolyzerFluidRecipe recipe = holder.value();
        addFluid(builder, RecipeIngredientRole.INPUT, 42, 18, recipe.input().fluid(), recipe.input().amount());
        addFluid(builder, RecipeIngredientRole.OUTPUT, 96, 18, recipe.output1().fluid(), recipe.output1().amount());
        addFluid(builder, RecipeIngredientRole.OUTPUT, 116, 18, recipe.output2().fluid(), recipe.output2().amount());
        for (int i = 0; i < recipe.byproducts().size(); i++) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, 154, 18 + i * 18).addItemStack(recipe.byproducts().get(i).copy());
        }
    }

    private static void addFluid(IRecipeLayoutBuilder builder, RecipeIngredientRole role, int x, int y, com.reinhardt.hbm.fluid.HbmFluidDefinition fluid, int amount) {
        Fluid neoFluid = HbmFluids.toNeoFluid(fluid);
        if (neoFluid != Fluids.EMPTY && amount > 0) {
            var slot = builder.addSlot(role, x, y).setFluidRenderer(16_000, false, 16, 52).addFluidStack(neoFluid, amount);
            HbmJeiFluidTooltips.addTo(slot);
        }
    }
}
