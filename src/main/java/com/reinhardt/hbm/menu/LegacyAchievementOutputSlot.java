package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.advancement.HbmAdvancements;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 1.21 equivalent of HBM 1.7.10's SlotCraftingOutput achievement hook.
 */
class LegacyAchievementOutputSlot extends Slot {
    private ItemStack removedStack = ItemStack.EMPTY;

    LegacyAchievementOutputSlot(Container container, int slot, int x, int y) {
        super(container, slot, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }

    @Override
    public ItemStack remove(int amount) {
        ItemStack removed = super.remove(amount);
        if (!removed.isEmpty()) {
            this.removedStack = removed.copy();
        }
        return removed;
    }

    @Override
    public void onTake(Player player, ItemStack stack) {
        ItemStack crafted = stack.isEmpty() ? this.removedStack : stack;
        HbmAdvancements.awardForCraftedStack(player, crafted);
        this.removedStack = ItemStack.EMPTY;
        super.onTake(player, stack);
    }
}
