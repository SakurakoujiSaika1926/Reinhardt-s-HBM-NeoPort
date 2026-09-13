package com.reinhardt.hbm.integration.jei;

import com.reinhardt.hbm.ReinhardtsHBM;
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
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/** JEI-only view of the 1.7.10 fixed barrel container conversions. */
public final class LegacyFluidContainerRecipeCategory implements IRecipeCategory<LegacyFluidContainerJeiRecipe> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/nei/gui_nei.png");

    private final IDrawable background;
    private final IDrawable icon;

    public LegacyFluidContainerRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 38, 21, 100, 45);
        this.icon = helper.createDrawableItemStack(new ItemStack(HbmItems.TANK_STEEL.get()));
    }

    @Override
    public RecipeType<LegacyFluidContainerJeiRecipe> getRecipeType() {
        return HbmJeiPlugin.LEGACY_FLUID_CONTAINER;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.reinhardtshbm.legacy_fluid_container");
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
    public void setRecipe(IRecipeLayoutBuilder builder, LegacyFluidContainerJeiRecipe recipe, IFocusGroup focuses) {
        if (recipe.mode() == LegacyFluidContainerJeiRecipe.Mode.FILL) {
            builder.addInputSlot(1, 14).addItemStack(recipe.inputItem().copy());
            addFluid(builder.addInputSlot(31, 14), recipe);
            builder.addOutputSlot(73, 14).addItemStack(recipe.outputItem().copy());
        } else {
            builder.addInputSlot(1, 14).addItemStack(recipe.inputItem().copy());
            addFluid(builder.addOutputSlot(31, 14), recipe);
            builder.addOutputSlot(73, 14).addItemStack(recipe.outputItem().copy());
        }
    }

    @Override
    public void draw(LegacyFluidContainerJeiRecipe recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView recipeSlotsView,
                     GuiGraphics guiGraphics, double mouseX, double mouseY) {
        guiGraphics.blit(TEXTURE, 0, 13, 5, 87, 18, 18, 256, 256);
        guiGraphics.blit(TEXTURE, 30, 13, 5, 87, 18, 18, 256, 256);
        guiGraphics.blit(TEXTURE, 72, 13, 5, 87, 18, 18, 256, 256);
        guiGraphics.blit(TEXTURE, 53, 14, 23, 87, 18, 18, 256, 256);
    }

    private static void addFluid(IRecipeSlotBuilder slot, LegacyFluidContainerJeiRecipe recipe) {
        Fluid fluid = HbmFluids.toNeoFluid(recipe.fluid());
        if (fluid == Fluids.EMPTY || recipe.amount() <= 0) {
            return;
        }
        slot.setFluidRenderer(recipe.amount(), false, 16, 16).addFluidStack(fluid, recipe.amount());
        HbmJeiFluidTooltips.addTo(slot);
    }
}
