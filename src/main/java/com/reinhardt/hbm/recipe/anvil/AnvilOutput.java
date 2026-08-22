package com.reinhardt.hbm.recipe.anvil;

import net.minecraft.world.item.ItemStack;

public record AnvilOutput(ItemStack stack, float chance) {
    public AnvilOutput(ItemStack stack) {
        this(stack, 1.0F);
    }
}
