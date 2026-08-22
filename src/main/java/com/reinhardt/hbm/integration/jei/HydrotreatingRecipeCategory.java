package com.reinhardt.hbm.integration.jei;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.recipe.HydrotreatingRecipe;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
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

public final class HydrotreatingRecipeCategory implements IRecipeCategory<RecipeHolder<HydrotreatingRecipe>> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/processing/gui_hydrotreater.png");
    private static final int TANK_CAPACITY = 1_000;

    private final IDrawable background;
    private final IDrawable icon;

    public HydrotreatingRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 0, 0, 176, 126);
        this.icon = helper.createDrawableItemStack(new ItemStack(HbmBlocks.MACHINE_HYDROTREATER.get()));
    }

    @Override
    public RecipeType<RecipeHolder<HydrotreatingRecipe>> getRecipeType() {
        return HbmJeiPlugin.HYDROTREATING;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.reinhardtshbm.machine_hydrotreater");
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<HydrotreatingRecipe> holder, IFocusGroup focuses) {
        HydrotreatingRecipe recipe = holder.value();
        addFluid(builder.addInputSlot(35, 36), recipe.input().fluid(), recipe.input().amount());
        addFluid(builder.addInputSlot(53, 36), recipe.hydrogen().fluid(), recipe.hydrogen().amount());
        builder.addSlot(RecipeIngredientRole.CATALYST, 89, 36)
                .addItemStack(new ItemStack(HbmItems.CATALYTIC_CONVERTER.get()));
        addFluid(builder.addOutputSlot(125, 36), recipe.output1().fluid(), recipe.output1().amount());
        addFluid(builder.addOutputSlot(143, 36), recipe.output2().fluid(), recipe.output2().amount());
    }

    private static void addFluid(IRecipeSlotBuilder slot, com.reinhardt.hbm.fluid.HbmFluidDefinition definition, int amount) {
        Fluid fluid = HbmFluids.toNeoFluid(definition);
        if (fluid == Fluids.EMPTY || amount <= 0) {
            return;
        }
        slot.setFluidRenderer(TANK_CAPACITY, false, 16, 16)
                .addFluidStack(fluid, amount);
        HbmJeiFluidTooltips.addTo(slot);
    }
}
