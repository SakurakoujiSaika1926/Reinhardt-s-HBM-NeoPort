package com.reinhardt.hbm.recipe.anvil;

import com.reinhardt.hbm.item.LegacyHotItem;
import net.minecraft.world.item.ItemStack;

/**
 * Exact 1.7.10 hot-anvil contract. Cold hot-material inputs are rejected and
 * an all-hot operation carries the average remaining heat into its output.
 */
public final class AnvilSmithingHotRecipe extends AnvilSmithingRecipe {
    public AnvilSmithingHotRecipe(int tier, ItemStack output, AnvilIngredient left, AnvilIngredient right) {
        super(tier, output, left, right);
    }

    @Override
    public Match match(ItemStack leftStack, ItemStack rightStack) {
        Match match = super.match(leftStack, rightStack);
        if (match == null || !isUsable(leftStack) || !isUsable(rightStack)) {
            return null;
        }
        return match;
    }

    @Override
    public ItemStack getOutput(ItemStack leftStack, ItemStack rightStack) {
        ItemStack output = super.getOutput(leftStack, rightStack);
        if (leftStack.getItem() instanceof LegacyHotItem left
                && rightStack.getItem() instanceof LegacyHotItem right
                && output.getItem() instanceof LegacyHotItem) {
            LegacyHotItem.heatUp(output, (left.heatFraction(leftStack) + right.heatFraction(rightStack)) / 2.0D);
        }
        return output;
    }

    @Override
    public ItemStack displayOutput() {
        return LegacyHotItem.heatUp(super.displayOutput());
    }

    private static boolean isUsable(ItemStack stack) {
        return !(stack.getItem() instanceof LegacyHotItem hot) || hot.heatFraction(stack) >= 0.5D;
    }
}
