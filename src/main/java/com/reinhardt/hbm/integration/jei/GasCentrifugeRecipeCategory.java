package com.reinhardt.hbm.integration.jei;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.recipe.GasCentrifugeRecipe;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmFluids;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Optional;

public class GasCentrifugeRecipeCategory implements IRecipeCategory<RecipeHolder<GasCentrifugeRecipe>> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/processing/gui_centrifuge_gas.png");
    private static final int TANK_CAPACITY = 8_000;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawableAnimated progress;

    public GasCentrifugeRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 0, 0, 176, 100);
        this.icon = helper.createDrawableItemStack(new ItemStack(HbmBlocks.MACHINE_GASCENT.get()));

        IDrawableStatic progressStatic = helper.createDrawable(TEXTURE, 206, 52, 36, 13);
        this.progress = helper.createAnimatedDrawable(progressStatic, 80, IDrawableAnimated.StartDirection.LEFT, false);
    }

    @Override
    public RecipeType<RecipeHolder<GasCentrifugeRecipe>> getRecipeType() {
        return HbmJeiPlugin.GAS_CENTRIFUGE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.reinhardtshbm.gas_centrifuge");
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<GasCentrifugeRecipe> holder, IFocusGroup focuses) {
        GasCentrifugeRecipe recipe = holder.value();
        addFluid(builder.addInputSlot(15, 16), recipe.input().fluid(), recipe.input().amount());
        if (recipe.hasFluidOutput()) {
            addFluid(builder.addOutputSlot(137, 16), recipe.output().fluid(), recipe.output().amount());
        }

        for (int index = 0; index < recipe.results().size(); index++) {
            int x = 71 + index % 2 * 18;
            int y = 53 + index / 2 * 18;
            builder.addOutputSlot(x, y)
                    .addItemStack(recipe.results().get(index).copy());
        }

        if (recipe.highSpeed()) {
            upgradeStack().ifPresent(stack -> builder.addSlot(RecipeIngredientRole.CATALYST, 69, 15).addItemStack(stack));
        }
    }

    @Override
    public void draw(RecipeHolder<GasCentrifugeRecipe> holder, mezz.jei.api.gui.ingredient.IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        GasCentrifugeRecipe recipe = holder.value();
        this.progress.draw(guiGraphics, 70, 35);
        guiGraphics.drawString(Minecraft.getInstance().font, fluidName(recipe.input().fluid()), 14, 75, 0x404040, false);
        if (recipe.hasFluidOutput()) {
            guiGraphics.drawString(Minecraft.getInstance().font, fluidName(recipe.output().fluid()), 136, 75, 0x404040, false);
        }
    }

    private static void addFluid(mezz.jei.api.gui.builder.IRecipeSlotBuilder slot, com.reinhardt.hbm.fluid.HbmFluidDefinition definition, int amount) {
        Fluid fluid = HbmFluids.toNeoFluid(definition);
        if (fluid == Fluids.EMPTY) {
            return;
        }
        slot.setFluidRenderer(TANK_CAPACITY, false, 16, 52)
                .addFluidStack(fluid, amount);
        HbmJeiFluidTooltips.addTo(slot);
    }

    private static String fluidName(com.reinhardt.hbm.fluid.HbmFluidDefinition definition) {
        return Component.translatable(definition.translationKey()).getString();
    }

    private static Optional<ItemStack> upgradeStack() {
        Optional<Item> item = BuiltInRegistries.ITEM.getOptional(ReinhardtsHBM.id("upgrade_gc_speed"));
        return item.map(ItemStack::new);
    }
}
