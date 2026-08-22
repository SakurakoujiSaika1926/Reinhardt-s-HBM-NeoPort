package com.reinhardt.hbm.integration.jei;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.recipe.ReformingRecipe;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmItems;
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

public class ReformingRecipeCategory implements IRecipeCategory<RecipeHolder<ReformingRecipe>> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/jei/gui_nei_reforming.png");
    private static final int TANK_CAPACITY = 1_000;

    private final IDrawable background;
    private final IDrawable icon;

    public ReformingRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 5, 11, 166, 65);
        this.icon = helper.createDrawableItemStack(new ItemStack(HbmBlocks.MACHINE_CATALYTIC_REFORMER.get()));
    }

    @Override
    public RecipeType<RecipeHolder<ReformingRecipe>> getRecipeType() {
        return HbmJeiPlugin.REFORMING;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.reinhardtshbm.machine_catalytic_reformer");
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<ReformingRecipe> holder, IFocusGroup focuses) {
        ReformingRecipe recipe = holder.value();
        addFluid(builder.addInputSlot(38, 25), recipe.input().fluid(), recipe.input().amount());
        builder.addSlot(RecipeIngredientRole.CATALYST, 74, 25)
                .addItemStack(new ItemStack(HbmItems.CATALYTIC_CONVERTER.get()));
        addFluid(builder.addOutputSlot(108, 16), recipe.output1().fluid(), recipe.output1().amount());
        addFluid(builder.addOutputSlot(126, 25), recipe.output2().fluid(), recipe.output2().amount());
        addFluid(builder.addOutputSlot(108, 34), recipe.output3().fluid(), recipe.output3().amount());
    }

    private static void addFluid(mezz.jei.api.gui.builder.IRecipeSlotBuilder slot, com.reinhardt.hbm.fluid.HbmFluidDefinition definition, int amount) {
        Fluid fluid = HbmFluids.toNeoFluid(definition);
        if (fluid == Fluids.EMPTY || amount <= 0) {
            return;
        }
        slot.setFluidRenderer(TANK_CAPACITY, false, 16, 16)
                .addFluidStack(fluid, amount);
        HbmJeiFluidTooltips.addTo(slot);
    }
}
