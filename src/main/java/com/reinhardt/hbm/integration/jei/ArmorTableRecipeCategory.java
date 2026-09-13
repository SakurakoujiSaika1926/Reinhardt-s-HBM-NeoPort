package com.reinhardt.hbm.integration.jei;

import com.reinhardt.hbm.registry.HbmBlocks;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** JEI category for installing armor modifiers in the Armor Modification Table. */
public final class ArmorTableRecipeCategory implements IRecipeCategory<ArmorTableJeiRecipe> {
    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable arrow;
    private final IDrawable plus;

    public ArmorTableRecipeCategory(IGuiHelper helper) {
        this.background = helper.createBlankDrawable(166, 65);
        this.icon = helper.createDrawableItemStack(new ItemStack(HbmBlocks.MACHINE_ARMOR_TABLE.get()));
        this.arrow = helper.getRecipeArrow();
        this.plus = helper.getRecipePlusSign();
    }

    @Override
    public RecipeType<ArmorTableJeiRecipe> getRecipeType() {
        return HbmJeiPlugin.ARMOR_TABLE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.reinhardtshbm.armor_table");
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
    public void setRecipe(IRecipeLayoutBuilder builder, ArmorTableJeiRecipe recipe, IFocusGroup focuses) {
        builder.addInputSlot(18, 23)
                .setStandardSlotBackground()
                .addItemStacks(recipe.armorInputs())
                .addRichTooltipCallback((view, tooltip) ->
                        tooltip.add(Component.translatable("jei.reinhardtshbm.armor_table.armor_input")));

        builder.addInputSlot(57, 23)
                .setStandardSlotBackground()
                .addItemStack(recipe.modifier().copy());

        builder.addSlot(RecipeIngredientRole.CATALYST, 86, 23)
                .setStandardSlotBackground()
                .addItemStack(new ItemStack(HbmBlocks.MACHINE_ARMOR_TABLE.get()));

        builder.addOutputSlot(129, 23)
                .setOutputSlotBackground()
                .addItemStacks(recipe.modifiedArmorOutputs())
                .addRichTooltipCallback((view, tooltip) ->
                        tooltip.add(Component.translatable("jei.reinhardtshbm.armor_table.modified_output")));
    }

    @Override
    public void draw(ArmorTableJeiRecipe recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView recipeSlotsView,
                     GuiGraphics guiGraphics, double mouseX, double mouseY) {
        this.plus.draw(guiGraphics, 42, 27);
        this.arrow.draw(guiGraphics, 105, 24);
        guiGraphics.drawString(
                Minecraft.getInstance().font,
                Component.translatable(
                        "jei.reinhardtshbm.armor_table.slot",
                        Component.translatable(recipe.slotTranslationKey())
                ),
                7,
                49,
                0x404040,
                false
        );
    }
}
