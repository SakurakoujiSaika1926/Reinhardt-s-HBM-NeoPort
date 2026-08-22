package com.reinhardt.hbm.integration.jei;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.recipe.FractionTowerRecipe;
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

public class FractionTowerRecipeCategory implements IRecipeCategory<RecipeHolder<FractionTowerRecipe>> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/jei/gui_nei_fraction_tower.png");
    private static final int TANK_CAPACITY = 100;

    private final IDrawable background;
    private final IDrawable icon;

    public FractionTowerRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 5, 11, 166, 65);
        this.icon = helper.createDrawableItemStack(new ItemStack(HbmBlocks.MACHINE_FRACTION_TOWER.get()));
    }

    @Override
    public RecipeType<RecipeHolder<FractionTowerRecipe>> getRecipeType() {
        return HbmJeiPlugin.FRACTION_TOWER;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.reinhardtshbm.machine_fraction_tower");
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<FractionTowerRecipe> holder, IFocusGroup focuses) {
        FractionTowerRecipe recipe = holder.value();
        addFluid(builder.addInputSlot(48, 24), recipe.input().fluid(), recipe.input().amount());
        addFluid(builder.addOutputSlot(102, 24), recipe.output1().fluid(), recipe.output1().amount());
        addFluid(builder.addOutputSlot(120, 24), recipe.output2().fluid(), recipe.output2().amount());
        builder.addSlot(RecipeIngredientRole.CATALYST, 74, 31)
                .addItemStack(new ItemStack(HbmBlocks.MACHINE_FRACTION_TOWER.get()));
    }

    private static void addFluid(mezz.jei.api.gui.builder.IRecipeSlotBuilder slot, com.reinhardt.hbm.fluid.HbmFluidDefinition definition, int amount) {
        Fluid fluid = HbmFluids.toNeoFluid(definition);
        if (fluid == Fluids.EMPTY) {
            return;
        }
        slot.setFluidRenderer(TANK_CAPACITY, false, 16, 16)
                .addFluidStack(fluid, amount);
        HbmJeiFluidTooltips.addTo(slot);
    }
}
