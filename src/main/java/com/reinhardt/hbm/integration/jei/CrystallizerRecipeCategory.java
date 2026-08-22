package com.reinhardt.hbm.integration.jei;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.CrystallizerBlockEntity;
import com.reinhardt.hbm.recipe.CrystallizerRecipe;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmFluids;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.drawable.IDrawableStatic;
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

import java.util.Arrays;
import java.util.List;

public class CrystallizerRecipeCategory implements IRecipeCategory<RecipeHolder<CrystallizerRecipe>> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/processing/gui_crystallizer_alt.png");

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawableAnimated powerBar;
    private final IDrawableAnimated progressBar;

    public CrystallizerRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 0, 0, 176, 104);
        this.icon = helper.createDrawableItemStack(new ItemStack(HbmBlocks.MACHINE_CRYSTALLIZER.get()));

        IDrawableStatic power = helper.createDrawable(TEXTURE, 176, 12, 16, 52);
        this.powerBar = helper.createAnimatedDrawable(power, 100, IDrawableAnimated.StartDirection.TOP, true);

        IDrawableStatic progress = helper.createDrawable(TEXTURE, 176, 0, 28, 12);
        this.progressBar = helper.createAnimatedDrawable(progress, 100, IDrawableAnimated.StartDirection.LEFT, false);
    }

    @Override
    public RecipeType<RecipeHolder<CrystallizerRecipe>> getRecipeType() {
        return HbmJeiPlugin.CRYSTALLIZER;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.reinhardtshbm.crystallizer");
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<CrystallizerRecipe> holder, IFocusGroup focuses) {
        CrystallizerRecipe recipe = holder.value();
        List<ItemStack> inputs = Arrays.stream(recipe.ingredient().getItems())
                .map(ItemStack::copy)
                .peek(stack -> stack.setCount(recipe.inputCount()))
                .toList();

        builder.addInputSlot(62, 45).addItemStacks(inputs);
        addFluid(builder.addInputSlot(35, 18), recipe.acid());
        builder.addOutputSlot(113, 45).addItemStack(recipe.result().copy());
    }

    @Override
    public void draw(RecipeHolder<CrystallizerRecipe> recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        this.powerBar.draw(guiGraphics, 152, 18);
        this.progressBar.draw(guiGraphics, 80, 47);
    }

    private static void addFluid(mezz.jei.api.gui.builder.IRecipeSlotBuilder slot, CrystallizerRecipe.AcidStack stack) {
        Fluid fluid = HbmFluids.toNeoFluid(stack.type());
        if (fluid == Fluids.EMPTY || stack.amount() <= 0) {
            return;
        }
        slot.setFluidRenderer(CrystallizerBlockEntity.TANK_CAPACITY, false, 16, 52)
                .addFluidStack(fluid, stack.amount());
        HbmJeiFluidTooltips.addTo(slot);
    }
}
