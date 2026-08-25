package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.item.ToolboxItem;
import com.reinhardt.hbm.registry.HbmItems;
import com.reinhardt.hbm.registry.HbmMenus;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.List;

/** Server-authoritative item inventory used by the held toolbox. */
public final class ToolboxMenu extends AbstractContainerMenu {
    private static final int PLAYER_INVENTORY_START = ToolboxItem.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Player player;
    private final net.minecraft.world.InteractionHand hand;
    private final ToolboxContainer toolbox;

    public ToolboxMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, buffer.readBoolean()
                ? net.minecraft.world.InteractionHand.OFF_HAND
                : net.minecraft.world.InteractionHand.MAIN_HAND);
    }

    public ToolboxMenu(int containerId, Inventory inventory, net.minecraft.world.InteractionHand hand) {
        super(HbmMenus.TOOLBOX.get(), containerId);
        this.player = inventory.player;
        this.hand = hand;
        this.toolbox = new ToolboxContainer(this.player, hand);

        for (int row = 0; row < ToolboxItem.ROWS; row++) {
            for (int column = 0; column < ToolboxItem.COLUMNS; column++) {
                addSlot(new ToolboxSlot(this.toolbox, column + row * ToolboxItem.COLUMNS, 17 + column * 18, 49 + row * 18));
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 129 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 187));
        }
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
        if (index < ToolboxItem.SLOT_COUNT) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, ToolboxItem.SLOT_COUNT, false)) {
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
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        int heldSlot = this.hand == net.minecraft.world.InteractionHand.MAIN_HAND ? player.getInventory().selected : -1;
        if ((clickType == ClickType.SWAP && button == heldSlot) || slotId == HOTBAR_START + heldSlot) {
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getItemInHand(this.hand).is(HbmItems.TOOLBOX.get());
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.toolbox.close();
    }

    private static final class ToolboxSlot extends Slot {
        private ToolboxSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return !stack.is(HbmItems.TOOLBOX.get());
        }
    }

    private static final class ToolboxContainer implements Container {
        private final Player player;
        private final net.minecraft.world.InteractionHand hand;
        private final List<ItemStack> items;

        private ToolboxContainer(Player player, net.minecraft.world.InteractionHand hand) {
            this.player = player;
            this.hand = hand;
            this.items = new ArrayList<>(ToolboxItem.loadContents(player.getItemInHand(hand), player.registryAccess()));
        }

        @Override
        public int getContainerSize() {
            return ToolboxItem.SLOT_COUNT;
        }

        @Override
        public boolean isEmpty() {
            return this.items.stream().allMatch(ItemStack::isEmpty);
        }

        @Override
        public ItemStack getItem(int slot) {
            return this.items.get(slot);
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            ItemStack item = this.items.get(slot);
            if (item.isEmpty()) {
                return ItemStack.EMPTY;
            }
            ItemStack removed = item.split(amount);
            if (item.isEmpty()) {
                this.items.set(slot, ItemStack.EMPTY);
            }
            setChanged();
            return removed;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            ItemStack removed = this.items.get(slot);
            this.items.set(slot, ItemStack.EMPTY);
            return removed;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            this.items.set(slot, stack);
            setChanged();
        }

        @Override
        public void setChanged() {
            ItemStack box = this.player.getItemInHand(this.hand);
            if (box.is(HbmItems.TOOLBOX.get())) {
                ToolboxItem.saveContents(box, this.items, this.player.registryAccess());
            }
        }

        @Override
        public boolean stillValid(Player player) {
            return player.getItemInHand(this.hand).is(HbmItems.TOOLBOX.get());
        }

        @Override
        public void clearContent() {
            for (int slot = 0; slot < ToolboxItem.SLOT_COUNT; slot++) {
                this.items.set(slot, ItemStack.EMPTY);
            }
            setChanged();
        }

        private void close() {
            ItemStack box = this.player.getItemInHand(this.hand);
            if (!box.is(HbmItems.TOOLBOX.get())) {
                return;
            }
            CompoundTag data = box.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            data.remove("toolbox_open");
            box.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        }
    }
}
