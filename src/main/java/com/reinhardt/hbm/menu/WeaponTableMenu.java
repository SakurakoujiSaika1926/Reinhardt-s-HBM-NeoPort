package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.registry.HbmMenus;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class WeaponTableMenu extends AbstractContainerMenu {
    private static final int MOD_SLOTS = 7;
    private static final int GUN_SLOT = 7;
    private static final int PLAYER_INV_START = 8;
    private static final int PLAYER_INV_END = PLAYER_INV_START + 27;
    private static final int HOTBAR_START = PLAYER_INV_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final SimpleContainer mods = new SimpleContainer(MOD_SLOTS);
    private final SimpleContainer gun = new SimpleContainer(1);
    private int configIndex;

    public WeaponTableMenu(int containerId, Inventory inventory, net.minecraft.network.RegistryFriendlyByteBuf ignored) {
        this(containerId, inventory);
    }

    public WeaponTableMenu(int containerId, Inventory inventory) {
        super(HbmMenus.WEAPON_TABLE.get(), containerId);
        for (int slot = 0; slot < MOD_SLOTS; slot++) {
            addSlot(new ModSlot(slot, 44 + 18 * slot, 108));
        }
        addSlot(new GunSlot(8, 108));
        addPlayerInventory(inventory);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return moved;
        }
        ItemStack stack = slot.getItem();
        moved = stack.copy();
        if (index < PLAYER_INV_START) {
            if (!moveItemStackTo(stack, PLAYER_INV_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stack);
        } else if (this.slots.get(GUN_SLOT).mayPlace(stack)) {
            if (!moveItemStackTo(stack, GUN_SLOT, GUN_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < PLAYER_INV_END) {
            if (!moveItemStackTo(stack, HOTBAR_START, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, PLAYER_INV_START, PLAYER_INV_END, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return moved;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide) {
            for (int slot = 0; slot < MOD_SLOTS; slot++) {
                ItemStack stack = this.mods.removeItemNoUpdate(slot);
                if (!stack.isEmpty()) {
                    player.drop(stack, false);
                }
            }
            ItemStack gunStack = this.gun.removeItemNoUpdate(0);
            if (!gunStack.isEmpty()) {
                player.drop(gunStack, false);
            }
        }
    }

    public int configIndex() {
        return this.configIndex;
    }

    public ItemStack gunStack() {
        return this.gun.getItem(0);
    }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 158 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 216));
        }
    }

    private final class GunSlot extends Slot {
        private GunSlot(int x, int y) {
            super(WeaponTableMenu.this.gun, 0, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public void set(ItemStack stack) {
            WeaponTableMenu.this.configIndex = 0;
            super.set(stack);
        }

        @Override
        public void onTake(Player player, ItemStack stack) {
            super.onTake(player, stack);
            WeaponTableMenu.this.configIndex = 0;
        }
    }

    private final class ModSlot extends Slot {
        private ModSlot(int slot, int x, int y) {
            super(WeaponTableMenu.this.mods, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

}
