package com.reinhardt.hbm.integration.jei;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmFluids;
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
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/** JEI equivalent of the 1.7.10 BoilingHandler's generated fluid conversions. */
public final class BoilingRecipeCategory implements IRecipeCategory<BoilingJeiRecipe> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/nei/gui_nei_boiler.png");

    private final IDrawable background;
    private final IDrawable icon;

    public BoilingRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 0, 0, 176, 82);
        this.icon = helper.createDrawableItemStack(new ItemStack(HbmBlocks.HEAT_BOILER.get()));
    }

    @Override
    public RecipeType<BoilingJeiRecipe> getRecipeType() {
        return HbmJeiPlugin.BOILING;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.reinhardtshbm.boiling");
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
    public void setRecipe(IRecipeLayoutBuilder builder, BoilingJeiRecipe recipe, IFocusGroup focuses) {
        addFluid(builder.addInputSlot(48, 24), recipe.input(), recipe.inputAmount());
        builder.addSlot(RecipeIngredientRole.CATALYST, 75, 31)
                .addItemStacks(java.util.List.of(
                        new ItemStack(HbmBlocks.HEAT_BOILER.get()),
                        new ItemStack(HbmBlocks.MACHINE_INDUSTRIAL_BOILER.get())
                ));
        addFluid(builder.addOutputSlot(102, 24), recipe.output(), recipe.outputAmount());
    }

    private static void addFluid(IRecipeSlotBuilder slot, HbmFluidDefinition definition, int amount) {
        Fluid fluid = HbmFluids.toNeoFluid(definition);
        if (fluid == Fluids.EMPTY || amount <= 0) {
            return;
        }
        slot.setFluidRenderer(amount, false, 16, 16).addFluidStack(fluid, amount);
        HbmJeiFluidTooltips.addTo(slot);
    }
}
