package com.reinhardt.hbm.integration.jei;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.recipe.AssemblyMachineRecipe;
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

public class AssemblyMachineRecipeCategory implements IRecipeCategory<RecipeHolder<AssemblyMachineRecipe>> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/processing/gui_assembler.png");
    private static final int TANK_CAPACITY = 4_000;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawableAnimated powerBar;
    private final IDrawableAnimated progressBar;

    public AssemblyMachineRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 0, 0, 176, 145);
        this.icon = helper.createDrawableItemStack(new ItemStack(HbmBlocks.MACHINE_ASSEMBLY_MACHINE.get()));

        IDrawableStatic power = helper.createDrawable(TEXTURE, 176, 0, 16, 61);
        this.powerBar = helper.createAnimatedDrawable(power, 100, IDrawableAnimated.StartDirection.TOP, true);

        IDrawableStatic progress = helper.createDrawable(TEXTURE, 176, 61, 70, 16);
        this.progressBar = helper.createAnimatedDrawable(progress, 100, IDrawableAnimated.StartDirection.LEFT, false);
    }

    @Override
    public RecipeType<RecipeHolder<AssemblyMachineRecipe>> getRecipeType() {
        return HbmJeiPlugin.ASSEMBLY_MACHINE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.reinhardtshbm.assembly_machine");
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<AssemblyMachineRecipe> holder, IFocusGroup focuses) {
        AssemblyMachineRecipe recipe = holder.value();
        int slot = 0;
        for (AssemblyMachineRecipe.CountedIngredient ingredient : recipe.ingredients()) {
            int x = 8 + slot % 3 * 18;
            int y = 18 + slot / 3 * 18;
            List<ItemStack> stacks = Arrays.stream(ingredient.ingredient().getItems())
                    .map(ItemStack::copy)
                    .peek(stack -> stack.setCount(ingredient.count()))
                    .toList();
            builder.addInputSlot(x, y).addItemStacks(stacks);
            slot++;
        }

        if (!recipe.inputFluids().isEmpty()) {
            addFluid(builder.addInputSlot(8, 99), recipe.inputFluids().getFirst());
        }
        if (!recipe.outputFluids().isEmpty()) {
            addFluid(builder.addOutputSlot(80, 99), recipe.outputFluids().getFirst());
        }

        builder.addOutputSlot(98, 45).addItemStack(recipe.result().copy());
    }

    @Override
    public void draw(RecipeHolder<AssemblyMachineRecipe> recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        this.powerBar.draw(guiGraphics, 152, 18);
        this.progressBar.draw(guiGraphics, 62, 126);
    }

    private static void addFluid(mezz.jei.api.gui.builder.IRecipeSlotBuilder slot, AssemblyMachineRecipe.AssemblyFluidStack stack) {
        Fluid fluid = HbmFluids.toNeoFluid(stack.type());
        if (fluid == Fluids.EMPTY || stack.amount() <= 0) {
            return;
        }
        slot.setFluidRenderer(TANK_CAPACITY, false, 52, 16)
                .addFluidStack(fluid, stack.amount());
        HbmJeiFluidTooltips.addTo(slot);
    }
}
