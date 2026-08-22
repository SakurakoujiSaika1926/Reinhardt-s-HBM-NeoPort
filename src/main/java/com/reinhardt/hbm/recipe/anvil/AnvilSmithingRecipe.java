package com.reinhardt.hbm.recipe.anvil;

import net.minecraft.world.item.ItemStack;

public class AnvilSmithingRecipe {
    private final int tier;
    private final ItemStack output;
    private final AnvilIngredient left;
    private final AnvilIngredient right;
    private final boolean shapeless;

    public AnvilSmithingRecipe(int tier, ItemStack output, AnvilIngredient left, AnvilIngredient right) {
        this(tier, output, left, right, false);
    }

    public AnvilSmithingRecipe(int tier, ItemStack output, AnvilIngredient left, AnvilIngredient right, boolean shapeless) {
        this.tier = tier;
        this.output = output.copy();
        this.left = left;
        this.right = right;
        this.shapeless = shapeless;
    }

    public int tier() {
        return this.tier;
    }

    public ItemStack displayOutput() {
        return this.output.copy();
    }

    public AnvilIngredient left() {
        return this.left;
    }

    public AnvilIngredient right() {
        return this.right;
    }

    public Match match(ItemStack leftStack, ItemStack rightStack) {
        if (this.left.matches(leftStack) && this.right.matches(rightStack)) {
            return new Match(this, false);
        }
        if (this.shapeless && this.left.matches(rightStack) && this.right.matches(leftStack)) {
            return new Match(this, true);
        }
        return null;
    }

    public ItemStack getOutput(ItemStack leftStack, ItemStack rightStack) {
        return this.output.copy();
    }

    public int amountConsumed(int slot, boolean mirrored) {
        if (slot == 0) {
            return mirrored ? this.right.count() : this.left.count();
        }
        if (slot == 1) {
            return mirrored ? this.left.count() : this.right.count();
        }
        return 0;
    }

    public record Match(AnvilSmithingRecipe recipe, boolean mirrored) {
    }
}
