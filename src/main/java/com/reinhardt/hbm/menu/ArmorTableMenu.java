package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.registry.HbmMenus;
import com.reinhardt.hbm.util.ArmorModHandler;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

public class ArmorTableMenu extends AbstractContainerMenu {
    private static final int ARMOR_SLOT = ArmorModHandler.MOD_SLOTS;
    private static final int PLAYER_ARMOR_START = ARMOR_SLOT + 1;
    private static final int PLAYER_INV_START = PLAYER_ARMOR_START + 4;
    private static final int PLAYER_INV_END = PLAYER_INV_START + 27;
    private static final int HOTBAR_START = PLAYER_INV_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Inventory playerInventory;
    private final SimpleContainer upgrades = new SimpleContainer(ArmorModHandler.MOD_SLOTS);
    private final SimpleContainer armor = new SimpleContainer(1);

    public ArmorTableMenu(int containerId, Inventory inventory, net.minecraft.network.RegistryFriendlyByteBuf ignored) {
        this(containerId, inventory);
    }

    public ArmorTableMenu(int containerId, Inventory inventory) {
        super(HbmMenus.ARMOR_TABLE.get(), containerId);
        this.playerInventory = inventory;

        addSlot(new UpgradeSlot(ArmorModHandler.HELMET_ONLY, 26 + 22, 27));
        addSlot(new UpgradeSlot(ArmorModHandler.PLATE_ONLY, 62 + 22, 27));
        addSlot(new UpgradeSlot(ArmorModHandler.LEGS_ONLY, 98 + 22, 27));
        addSlot(new UpgradeSlot(ArmorModHandler.BOOTS_ONLY, 134 + 22, 45));
        addSlot(new UpgradeSlot(ArmorModHandler.SERVOS, 134 + 22, 81));
        addSlot(new UpgradeSlot(ArmorModHandler.CLADDING, 98 + 22, 99));
        addSlot(new UpgradeSlot(ArmorModHandler.KEVLAR, 62 + 22, 99));
        addSlot(new UpgradeSlot(ArmorModHandler.EXTRA, 26 + 22, 99));
        addSlot(new UpgradeSlot(ArmorModHandler.BATTERY, 8 + 22, 63));
        addSlot(new ArmorSlot(44 + 22, 63));

        for (int slot = 0; slot < 4; slot++) {
            int equipmentIndex = 39 - slot;
            addSlot(new PlayerArmorSlot(inventory, equipmentIndex, -17 + 22, 36 + slot * 18));
        }
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

        if (index <= ARMOR_SLOT) {
            if (index == ARMOR_SLOT && !moveItemStackTo(stack, PLAYER_ARMOR_START, PLAYER_ARMOR_START + 4, false)) {
                if (!moveItemStackTo(stack, PLAYER_INV_START, HOTBAR_END, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (index < ARMOR_SLOT && !moveItemStackTo(stack, PLAYER_INV_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stack);
        } else if (stack.getItem() instanceof ArmorItem) {
            if (!moveItemStackTo(stack, ARMOR_SLOT, ARMOR_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (isArmorModForVisibleArmor(stack)) {
            int target = ((com.reinhardt.hbm.item.ArmorModItem) stack.getItem()).slotType();
            if (!moveItemStackTo(stack, target, target + 1, false)) {
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
            for (int slot = 0; slot < ArmorModHandler.MOD_SLOTS; slot++) {
                ItemStack stack = this.upgrades.removeItemNoUpdate(slot);
                if (!stack.isEmpty()) {
                    player.drop(stack, false);
                    ArmorModHandler.removeMod(this.armor.getItem(0), slot);
                }
            }
            ItemStack armorStack = this.armor.removeItemNoUpdate(0);
            if (!armorStack.isEmpty()) {
                player.drop(armorStack, false);
            }
        }
    }

    public ItemStack armorStack() {
        return this.armor.getItem(0);
    }

    public boolean modApplicable(int slot) {
        return ArmorModHandler.isApplicable(this.armor.getItem(0), this.upgrades.getItem(slot));
    }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18 + 22, 84 + row * 18 + 56));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18 + 22, 142 + 56));
        }
    }

    private boolean isArmorModForVisibleArmor(ItemStack stack) {
        return stack.getItem() instanceof com.reinhardt.hbm.item.ArmorModItem mod
                && this.slots.get(mod.slotType()).mayPlace(stack);
    }

    private final class ArmorSlot extends Slot {
        private ArmorSlot(int x, int y) {
            super(ArmorTableMenu.this.armor, 0, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getItem() instanceof ArmorItem;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public void set(ItemStack stack) {
            if (!stack.isEmpty()) {
                ItemStack[] mods = ArmorModHandler.pryMods(stack, ArmorTableMenu.this.playerInventory.player.registryAccess());
                for (int slot = 0; slot < ArmorModHandler.MOD_SLOTS; slot++) {
                    ArmorTableMenu.this.upgrades.setItem(slot, mods[slot] == null ? ItemStack.EMPTY : mods[slot]);
                }
            }
            super.set(stack);
        }

        @Override
        public void onTake(Player player, ItemStack stack) {
            super.onTake(player, stack);
            for (int slot = 0; slot < ArmorModHandler.MOD_SLOTS; slot++) {
                ItemStack mod = ArmorTableMenu.this.upgrades.getItem(slot);
                if (ArmorModHandler.isApplicable(stack, mod)) {
                    ArmorTableMenu.this.upgrades.setItem(slot, ItemStack.EMPTY);
                }
            }
        }
    }

    private final class UpgradeSlot extends Slot {
        private UpgradeSlot(int slot, int x, int y) {
            super(ArmorTableMenu.this.upgrades, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getItem() instanceof com.reinhardt.hbm.item.ArmorModItem mod
                    && mod.slotType() == this.index
                    && ArmorModHandler.isApplicable(ArmorTableMenu.this.armor.getItem(0), stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public void set(ItemStack stack) {
            super.set(stack);
            if (!stack.isEmpty()) {
                ArmorModHandler.applyMod(ArmorTableMenu.this.armor.getItem(0), stack, ArmorTableMenu.this.playerInventory.player.registryAccess());
            }
        }

        @Override
        public void onTake(Player player, ItemStack stack) {
            super.onTake(player, stack);
            ArmorModHandler.removeMod(ArmorTableMenu.this.armor.getItem(0), this.index);
        }
    }

    private static final class PlayerArmorSlot extends Slot {
        private PlayerArmorSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getItem() instanceof ArmorItem;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }
}
