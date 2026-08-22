package com.reinhardt.hbm.integration.jei;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.item.FluidIconItem;
import com.reinhardt.hbm.recipe.SilexRecipe;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.util.Wavelength;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
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

import java.util.Arrays;

public class SilexRecipeCategory implements IRecipeCategory<RecipeHolder<SilexRecipe>> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/nei/gui_nei_silex.png");

    private final IDrawable background;
    private final IDrawable icon;
    private final RecipeType<RecipeHolder<SilexRecipe>> recipeType;
    private final String titleKey;

    public SilexRecipeCategory(IGuiHelper helper, RecipeType<RecipeHolder<SilexRecipe>> recipeType, String titleKey) {
        this.background = helper.createDrawable(TEXTURE, 3, 3, 170, 80);
        this.icon = helper.createDrawableItemStack(new ItemStack(HbmBlocks.MACHINE_SILEX.get()));
        this.recipeType = recipeType;
        this.titleKey = titleKey;
    }

    @Override
    public RecipeType<RecipeHolder<SilexRecipe>> getRecipeType() {
        return this.recipeType;
    }

    @Override
    public Component getTitle() {
        return Component.translatable(this.titleKey);
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<SilexRecipe> holder, IFocusGroup focuses) {
        SilexRecipe recipe = holder.value();
        if (recipe.ingredient().isPresent()) {
            builder.addInputSlot(13, 31).addItemStacks(Arrays.stream(recipe.ingredient().get().getItems()).map(ItemStack::copy).toList());
        } else if (recipe.fluidInput().isPresent()) {
            HbmFluids.byName(recipe.fluidInput().get()).ifPresent(fluid -> builder.addInputSlot(13, 31).addItemStack(FluidIconItem.forFluid(fluid)));
        }

        int outputSize = recipe.outputs().size();
        int split = columnSplit(outputSize);
        for (int index = 0; index < outputSize; index++) {
            int x = index < split ? 72 : 120;
            int y = slotY(index, outputSize, split);
            builder.addOutputSlot(x, y).addItemStack(recipe.outputs().get(index).stack().copy());
        }
    }

    @Override
    public void draw(RecipeHolder<SilexRecipe> holder, mezz.jei.api.gui.ingredient.IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        SilexRecipe recipe = holder.value();
        var font = Minecraft.getInstance().font;
        int outputSize = recipe.outputs().size();
        int split = columnSplit(outputSize);
        int totalWeight = recipe.totalWeight();

        for (int index = 0; index < outputSize; index++) {
            int x = index < split ? 90 : 138;
            int y = chanceY(index, outputSize, split);
            guiGraphics.drawString(font, chanceText(recipe.outputs().get(index).weight(), totalWeight), x, y, 0x404040, false);
        }

        String produced = producedText(recipe);
        guiGraphics.drawString(font, produced, 52 - font.width(produced) / 2, 51, 0x404040, false);

        String wavelength = wavelengthText(recipe.wavelength());
        guiGraphics.drawString(font, wavelength, 35 - font.width(wavelength) / 2, 17, wavelengthColor(recipe.wavelength()), false);
    }

    private static int columnSplit(int outputSize) {
        return outputSize > 6 ? 4 : outputSize > 4 ? 3 : 2;
    }

    private static int slotY(int index, int outputSize, int split) {
        if (index < split) {
            return 28 + index * 18 - 9 * ((Math.min(outputSize, split) + 1) / 2);
        }
        return 28 + (index - split) * 18 - 9 * ((Math.min(outputSize - split, split) + 1) / 2);
    }

    private static int chanceY(int index, int outputSize, int split) {
        if (index < split) {
            return 33 + index * 18 - 9 * ((Math.min(outputSize, split) + 1) / 2);
        }
        return 33 + (index - split) * 18 - 9 * ((Math.min(outputSize - split, split) + 1) / 2);
    }

    private static String chanceText(int weight, int totalWeight) {
        if (totalWeight <= 0) {
            return "0.0%";
        }
        double percentage = 100.0D * Math.max(0, weight) / totalWeight;
        return truncate(percentage, 100.0D) + "%";
    }

    private static String producedText(SilexRecipe recipe) {
        if (recipe.fluidConsumed() <= 0) {
            return "0.0x";
        }
        double ratio = (double) recipe.fluidProduced() / recipe.fluidConsumed();
        return truncate(ratio, 10.0D) + "x";
    }

    private static String wavelengthText(Wavelength wavelength) {
        if (wavelength == Wavelength.NULL) {
            return "N/A";
        }
        return Component.translatable(wavelength.translationKey()).getString();
    }

    private static int wavelengthColor(Wavelength wavelength) {
        if (wavelength == Wavelength.NULL) {
            return 0xFFFFFF;
        }
        Integer color = wavelength.textColor().getColor();
        return color == null ? 0x404040 : color;
    }

    private static String truncate(double value, double scale) {
        return String.valueOf(Math.floor(value * scale) / scale);
    }
}
