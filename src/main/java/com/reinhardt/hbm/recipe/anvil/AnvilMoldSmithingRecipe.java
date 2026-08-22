package com.reinhardt.hbm.recipe.anvil;

import net.minecraft.world.item.ItemStack;

public class AnvilMoldSmithingRecipe extends AnvilSmithingRecipe {
    public AnvilMoldSmithingRecipe(int tier, ItemStack output, AnvilIngredient reference, AnvilIngredient moldBase) {
        super(tier, output, reference, moldBase);
    }

    @Override
    public Match match(ItemStack leftStack, ItemStack rightStack) {
        if (leftStack.getCount() == left().count() && left().matchesItem(leftStack) && right().matches(rightStack)) {
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
