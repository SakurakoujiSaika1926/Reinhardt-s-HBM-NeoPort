package com.reinhardt.hbm.recipe.anvil;

import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

public class AnvilMoldSmithingRecipe extends AnvilSmithingRecipe {
    private final Predicate<ItemStack> leftMatcher;

    public AnvilMoldSmithingRecipe(int tier, ItemStack output, AnvilIngredient reference, AnvilIngredient moldBase) {
        this(tier, output, reference, moldBase, stack -> stack.getCount() == reference.count() && reference.matchesItem(stack));
    }

    public AnvilMoldSmithingRecipe(int tier, ItemStack output, AnvilIngredient reference, AnvilIngredient moldBase, Predicate<ItemStack> leftMatcher) {
        super(tier, output, reference, moldBase);
        this.leftMatcher = leftMatcher;
    }

    @Override
    public Match match(ItemStack leftStack, ItemStack rightStack) {
        if (this.leftMatcher.test(leftStack) && right().matches(rightStack)) {
            return new Match(this, false);
        }
        return null;
    }

    @Override
    public int amountConsumed(int slot, boolean mirrored) {
        if (slot == 0) {
            return 0;
        }
        if (slot == 1) {
            return 1;
        }
        return 0;
    }
}
