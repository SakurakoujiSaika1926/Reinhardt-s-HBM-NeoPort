package com.reinhardt.hbm.integration.jei;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.recipe.FusionBreederFluidRecipe;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmFluids;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public class FusionBreederFluidRecipeCategory implements IRecipeCategory<RecipeHolder<FusionBreederFluidRecipe>> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/reactors/gui_fusion_breeder.png");
    private static final int TANK_CAPACITY = 1_000;

    private final IDrawable background;
    private final IDrawable icon;

    public FusionBreederFluidRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 0, 0, 176, 84);
        this.icon = helper.createDrawableItemStack(new ItemStack(HbmBlocks.FUSION_BREEDER.get()));
    }

    @Override
    public RecipeType<RecipeHolder<FusionBreederFluidRecipe>> getRecipeType() {
        return HbmJeiPlugin.FUSION_BREEDER_FLUID;
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<FusionBreederFluidRecipe> holder, IFocusGroup focuses) {
        FusionBreederFluidRecipe recipe = holder.value();
        addFluid(builder.addInputSlot(26, 18), recipe.input().fluid(), recipe.input().amount());
        addFluid(builder.addOutputSlot(134, 18), recipe.output().fluid(), recipe.output().amount());
    }

    @Override
    public void draw(RecipeHolder<FusionBreederFluidRecipe> holder, mezz.jei.api.gui.ingredient.IRecipeSlotsView recipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
        graphics.renderItem(new ItemStack(HbmBlocks.FUSION_BREEDER.get()), 80, 34);
    }

    private static void addFluid(IRecipeSlotBuilder slot, HbmFluidDefinition definition, int amount) {
        Fluid fluid = HbmFluids.toNeoFluid(definition);
        if (fluid == Fluids.EMPTY || amount <= 0) {
            return;
        }
        slot.setFluidRenderer(TANK_CAPACITY, false, 16, 52)
                .addFluidStack(fluid, amount);
        HbmJeiFluidTooltips.addTo(slot);
    }
}
