package com.reinhardt.hbm.integration.jei;

import com.reinhardt.hbm.recipe.SpaceAssemblerRecipe;
import com.reinhardt.hbm.registry.HbmItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.Arrays;
import java.util.List;

/** Recipe-only orbital assembler view; no fabricated machine GUI background. */
public final class SpaceAssemblerRecipeCategory
        implements IRecipeCategory<RecipeHolder<SpaceAssemblerRecipe>> {
    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable arrow;

    public SpaceAssemblerRecipeCategory(IGuiHelper helper) {
        this.background = helper.createBlankDrawable(142, 72);
        this.icon = helper.createDrawableItemStack(new ItemStack(HbmItems.SATELLITE_SCIENCE_ASSEMBLER.get()));
        this.arrow = helper.getRecipeArrow();
    }

    @Override
    public RecipeType<RecipeHolder<SpaceAssemblerRecipe>> getRecipeType() {
        return HbmJeiPlugin.SPACE_ASSEMBLER;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.reinhardtshbm.space_assembler");
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<SpaceAssemblerRecipe> holder,
                          IFocusGroup focuses) {
        SpaceAssemblerRecipe recipe = holder.value();
        List<ItemStack> inputs = Arrays.stream(recipe.ingredient().getItems())
                .map(ItemStack::copy)
                .peek(stack -> stack.setCount(recipe.inputCount()))
                .toList();
        builder.addInputSlot(9, 18)
                .setStandardSlotBackground()
                .addItemStacks(inputs);

        for (int index = 0; index < recipe.results().size(); index++) {
            int column = index % 3;
            int row = index / 3;
            builder.addOutputSlot(82 + column * 20, 8 + row * 20)
                    .setOutputSlotBackground()
                    .addItemStack(recipe.results().get(index).copy());
        }
    }

    @Override
    public void draw(RecipeHolder<SpaceAssemblerRecipe> holder,
                     mezz.jei.api.gui.ingredient.IRecipeSlotsView recipeSlots,
                     GuiGraphics graphics, double mouseX, double mouseY) {
        this.arrow.draw(graphics, 42, 19);
        graphics.drawString(Minecraft.getInstance().font,
                Component.translatable("jei.reinhardtshbm.space_assembler.duration", holder.value().duration()),
                8, 61, 0xFF404040, false);
    }
}
