package com.reinhardt.hbm.integration.jei;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.item.ScrapsItem;
import com.reinhardt.hbm.recipe.RotaryFurnaceRecipe;
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
import java.util.Locale;

public class RotaryFurnaceRecipeCategory implements IRecipeCategory<RecipeHolder<RotaryFurnaceRecipe>> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/processing/gui_rotary_furnace.png");
    private static final int ADDITIVE_TANK_CAPACITY = 16_000;

    private final IDrawable background;
    private final IDrawable icon;

    public RotaryFurnaceRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 0, 0, 176, 86);
        this.icon = helper.createDrawableItemStack(new ItemStack(HbmBlocks.MACHINE_ROTARY_FURNACE.get()));
    }

    @Override
    public RecipeType<RecipeHolder<RotaryFurnaceRecipe>> getRecipeType() {
        return HbmJeiPlugin.ROTARY_FURNACE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.reinhardtshbm.machine_rotary_furnace");
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<RotaryFurnaceRecipe> recipe, IFocusGroup focuses) {
        RotaryFurnaceRecipe value = recipe.value();
        for (int index = 0; index < Math.min(3, value.inputs().size()); index++) {
            RotaryFurnaceRecipe.CountedIngredient input = value.inputs().get(index);
            List<ItemStack> stacks = Arrays.stream(input.ingredient().getItems())
                    .map(ItemStack::copy)
                    .peek(stack -> stack.setCount(input.count()))
                    .toList();
            builder.addInputSlot(8 + index * 18, 18)
                    .addItemStacks(stacks);
        }
        if (value.hasFluid()) {
            addFluid(builder.addInputSlot(8, 54), value.fluid().fluid(), value.fluid().amount());
        }
        builder.addSlot(RecipeIngredientRole.CATALYST, 44, 54)
                .addItemStack(new ItemStack(HbmBlocks.MACHINE_ROTARY_FURNACE.get()));
        builder.addOutputSlot(98, 36)
                .addItemStack(ScrapsItem.create(value.output().stack(), true));
    }

    @Override
    public void draw(RecipeHolder<RotaryFurnaceRecipe> recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        RotaryFurnaceRecipe value = recipe.value();
        String duration = String.format(Locale.US, "%,d", value.duration()) + " ticks";
        String consumption = Component.translatable("fluid.reinhardtshbm.steam").getString() + ": "
                + String.format(Locale.US, "%,d", value.steam()) + " mB/t";
        int side = 160;
        var font = Minecraft.getInstance().font;
        guiGraphics.drawString(font, duration, side - font.width(duration), 43, 0x404040, false);
        guiGraphics.drawString(font, consumption, side - font.width(consumption), 55, 0x404040, false);
    }

    private static void addFluid(IRecipeSlotBuilder slot, com.reinhardt.hbm.fluid.HbmFluidDefinition definition, int amount) {
        Fluid fluid = HbmFluids.toNeoFluid(definition);
        if (fluid == Fluids.EMPTY || amount <= 0) {
            return;
        }
        slot.setFluidRenderer(ADDITIVE_TANK_CAPACITY, false, 16, 16)
                .addFluidStack(fluid, amount);
        HbmJeiFluidTooltips.addTo(slot);
    }
}
