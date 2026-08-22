package com.reinhardt.hbm.integration.jei;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.recipe.ParticleAcceleratorRecipe;
import com.reinhardt.hbm.registry.HbmBlocks;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
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

import java.util.Arrays;
import java.util.List;

public class ParticleAcceleratorRecipeCategory implements IRecipeCategory<RecipeHolder<ParticleAcceleratorRecipe>> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/particleaccelerator/gui_detector.png");

    private final IDrawable background;
    private final IDrawable icon;

    public ParticleAcceleratorRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 0, 0, 176, 104);
        this.icon = helper.createDrawableItemStack(new ItemStack(HbmBlocks.PA_DETECTOR.get()));
    }

    @Override
    public RecipeType<RecipeHolder<ParticleAcceleratorRecipe>> getRecipeType() {
        return HbmJeiPlugin.PARTICLE_ACCELERATOR;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.reinhardtshbm.particle_accelerator");
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<ParticleAcceleratorRecipe> holder, IFocusGroup focuses) {
        ParticleAcceleratorRecipe recipe = holder.value();
        builder.addInputSlot(62, 18).addItemStacks(stacks(recipe.input1().getItems()));
        builder.addInputSlot(80, 18).addItemStacks(stacks(recipe.input2().getItems()));
        builder.addOutputSlot(62, 45).addItemStack(recipe.output1().copy());
        if (!recipe.output2().isEmpty()) {
            builder.addOutputSlot(80, 45).addItemStack(recipe.output2().copy());
        }
    }

    @Override
    public void draw(RecipeHolder<ParticleAcceleratorRecipe> holder, mezz.jei.api.gui.ingredient.IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        guiGraphics.drawString(
                net.minecraft.client.Minecraft.getInstance().font,
                Component.translatable("jei.reinhardtshbm.particle_accelerator.momentum", holder.value().momentum()),
                61,
                74,
                0x404040,
                false
        );
    }

    private static List<ItemStack> stacks(ItemStack[] stacks) {
        return Arrays.stream(stacks).map(ItemStack::copy).toList();
    }
}
