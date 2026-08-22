package com.reinhardt.hbm.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ChemistrySetItem extends Item {
    public ChemistrySetItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        ItemStack remaining = stack.copy();
        remaining.setCount(1);

        if (remaining.isDamageableItem() && remaining.getMaxDamage() > 0) {
            remaining.setDamageValue(remaining.getDamageValue() + 1);
            if (remaining.getDamageValue() >= remaining.getMaxDamage()) {
                return ItemStack.EMPTY;
            }
        }

        return remaining;
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return false;
    }
}
