package com.reinhardt.hbm.integration.jei;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.item.ScrapsItem;
import com.reinhardt.hbm.recipe.ElectrolyzerMetalRecipe;
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

public class ElectrolyzerMetalRecipeCategory implements IRecipeCategory<RecipeHolder<ElectrolyzerMetalRecipe>> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/processing/gui_electrolyser_metal.png");
    private final IDrawable background;
    private final IDrawable icon;

    public ElectrolyzerMetalRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 0, 0, 210, 96);
        this.icon = helper.createDrawableItemStack(new ItemStack(HbmBlocks.MACHINE_ELECTROLYSER.get()));
    }

    @Override
    public RecipeType<RecipeHolder<ElectrolyzerMetalRecipe>> getRecipeType() {
        return HbmJeiPlugin.ELECTROLYZER_METAL;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.reinhardtshbm.electrolyzer_metal");
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<ElectrolyzerMetalRecipe> holder, IFocusGroup focuses) {
        ElectrolyzerMetalRecipe recipe = holder.value();
        builder.addSlot(RecipeIngredientRole.INPUT, 10, 22).addIngredients(recipe.ingredient());
        Fluid acid = HbmFluids.toNeoFluid(HbmFluids.byName("nitric_acid").orElse(HbmFluids.none()));
        if (acid != Fluids.EMPTY) {
            var slot = builder.addSlot(RecipeIngredientRole.INPUT, 36, 18).setFluidRenderer(16_000, false, 16, 52).addFluidStack(acid, 100);
            HbmJeiFluidTooltips.addTo(slot);
        }
        recipe.output1Stack().ifPresent(stack -> builder.addSlot(RecipeIngredientRole.OUTPUT, 136, 18).addItemStack(ScrapsItem.create(stack, true)));
        recipe.output2Stack().ifPresent(stack -> builder.addSlot(RecipeIngredientRole.OUTPUT, 154, 18).addItemStack(ScrapsItem.create(stack, true)));
        for (int i = 0; i < recipe.byproducts().size(); i++) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, 136 + (i % 2) * 18, 36 + (i / 2) * 18).addItemStack(recipe.byproducts().get(i).copy());
        }
    }
}
