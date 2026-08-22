package com.reinhardt.hbm.integration.jei;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.recipe.PlasmaForgeRecipe;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.util.HbmFluidTooltip;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import java.util.Arrays;
import java.util.List;

public class PlasmaForgeRecipeCategory implements IRecipeCategory<RecipeHolder<PlasmaForgeRecipe>> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/reactors/gui_fusion_plasmaforge.png");
    private static final int TANK_CAPACITY = 16_000;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawableAnimated powerBar;
    private final IDrawableAnimated progressBar;

    public PlasmaForgeRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 0, 0, 176, 145);
        this.icon = helper.createDrawableItemStack(new ItemStack(HbmBlocks.FUSION_PLASMA_FORGE.get()));

        IDrawableStatic power = helper.createDrawable(TEXTURE, 176, 0, 16, 62);
        this.powerBar = helper.createAnimatedDrawable(power, 100, IDrawableAnimated.StartDirection.TOP, true);

        IDrawableStatic progress = helper.createDrawable(TEXTURE, 176, 62, 70, 16);
        this.progressBar = helper.createAnimatedDrawable(progress, 100, IDrawableAnimated.StartDirection.LEFT, false);
    }

    @Override
    public RecipeType<RecipeHolder<PlasmaForgeRecipe>> getRecipeType() {
        return HbmJeiPlugin.PLASMA_FORGE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.reinhardtshbm.fusion_plasma_forge");
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<PlasmaForgeRecipe> holder, IFocusGroup focuses) {
        PlasmaForgeRecipe recipe = holder.value();
        int slot = 0;
        for (PlasmaForgeRecipe.CountedIngredient ingredient : recipe.inputItems()) {
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
            addFluid(builder.addInputSlot(80, 18), recipe.inputFluids().getFirst());
        }

        builder.addOutputSlot(116, 36).addItemStack(recipe.result().copy());
    }

    @Override
    public void draw(RecipeHolder<PlasmaForgeRecipe> holder, mezz.jei.api.gui.ingredient.IRecipeSlotsView recipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
        PlasmaForgeRecipe recipe = holder.value();
        this.powerBar.draw(graphics, 152, 18);
        this.progressBar.draw(graphics, 62, 81);

        var font = Minecraft.getInstance().font;
        String duration = HbmFluidTooltip.shortNumber(recipe.duration()) + " ticks";
        graphics.drawString(font, duration, 164 - font.width(duration), 105, 0x404040, false);
        String power = HbmFluidTooltip.shortNumber(recipe.power()) + " HE/t";
        graphics.drawString(font, power, 164 - font.width(power), 117, 0x404040, false);
        String plasma = HbmFluidTooltip.shortNumber(recipe.ignitionTemp()) + " TU/t";
        graphics.drawString(font, plasma, 164 - font.width(plasma), 129, 0xA000A0, false);
    }

    private static void addFluid(mezz.jei.api.gui.builder.IRecipeSlotBuilder slot, PlasmaForgeRecipe.PlasmaFluidStack stack) {
        Fluid fluid = HbmFluids.toNeoFluid(stack.type());
        if (fluid == Fluids.EMPTY || stack.amount() <= 0) {
            return;
        }
        slot.setFluidRenderer(TANK_CAPACITY, false, 16, 52)
                .addFluidStack(fluid, stack.amount());
        HbmJeiFluidTooltips.addTo(slot);
    }
}
