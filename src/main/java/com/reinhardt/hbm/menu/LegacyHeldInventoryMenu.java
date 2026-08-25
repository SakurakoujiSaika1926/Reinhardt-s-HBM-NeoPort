package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.block.StorageCrateBlock;
import com.reinhardt.hbm.item.LegacyHeldInventoryItem;
import com.reinhardt.hbm.registry.HbmMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Exact held-item inventory sizes, slot positions, and stack limits from 1.7.10. */
public final class LegacyHeldInventoryMenu extends AbstractContainerMenu {
    private final Player player;
    private final InteractionHand hand;
    private final LegacyHeldInventoryItem.Kind kind;
    private final HeldItemContainer container;
    private final int carrierSlots;
    private final int playerInventoryStart;

    public LegacyHeldInventoryMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory,
                buffer.readBoolean() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND,
                LegacyHeldInventoryItem.Kind.values()[Byte.toUnsignedInt(buffer.readByte())]);
    }

    public LegacyHeldInventoryMenu(int containerId, Inventory inventory, InteractionHand hand, LegacyHeldInventoryItem.Kind kind) {
        super(HbmMenus.HELD_INVENTORY.get(), containerId);
        this.player = inventory.player;
        this.hand = hand;
        this.kind = kind;
        this.container = new HeldItemContainer(this.player, hand, kind);
        this.carrierSlots = kind.slots();

        if (kind == LegacyHeldInventoryItem.Kind.PLASTIC_BAG) {
            addSlot(new HeldItemSlot(container, 0, 80, 65, kind));
            addPlayerInventory(inventory, 134, 192);
        } else {
            for (int row = 0; row < 4; row++) {
                for (int column = 0; column < 5; column++) {
                    addSlot(new HeldItemSlot(container, column + row * 5, 43 + column * 18, 18 + row * 18, kind));
                }
            }
            addPlayerInventory(inventory, 104, 162);
        }
        this.playerInventoryStart = carrierSlots;
    }

    public LegacyHeldInventoryItem.Kind kind() {
        return kind;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size() || !slots.get(index).hasItem()) {
            return ItemStack.EMPTY;
        }
        Slot source = slots.get(index);
        ItemStack stack = source.getItem();
        ItemStack result = stack.copy();
        if (index < carrierSlots) {
            if (!moveItemStackTo(stack, playerInventoryStart, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, carrierSlots, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            source.setByPlayer(ItemStack.EMPTY);
        } else {
            source.setChanged();
        }
        return result;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        int heldSlot = hand == InteractionHand.MAIN_HAND ? player.getInventory().selected : -1;
        int hotbarStart = slots.size() - 9;
        if ((clickType == ClickType.SWAP && button == heldSlot) || (heldSlot >= 0 && slotId == hotbarStart + heldSlot)) {
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public boolean stillValid(Player player) {
        ItemStack held = player.getItemInHand(hand);
        return held.getItem() instanceof LegacyHeldInventoryItem item && item.kind() == kind;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container.save();
    }

    private void addPlayerInventory(Inventory inventory, int inventoryY, int hotbarY) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, inventoryY + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, hotbarY));
        }
    }

    private static final class HeldItemSlot extends Slot {
        private final LegacyHeldInventoryItem.Kind kind;

        private HeldItemSlot(Container container, int index, int x, int y, LegacyHeldInventoryItem.Kind kind) {
            super(container, index, x, y);
            this.kind = kind;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return kind != LegacyHeldInventoryItem.Kind.CONTAINMENT_BOX
                    || !(stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof StorageCrateBlock);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    private static final class HeldItemContainer implements Container {
        private final Player player;
        private final InteractionHand hand;
        private final LegacyHeldInventoryItem.Kind kind;
        private final List<ItemStack> items;

        private HeldItemContainer(Player player, InteractionHand hand, LegacyHeldInventoryItem.Kind kind) {
            this.player = player;
            this.hand = hand;
            this.kind = kind;
            this.items = new ArrayList<>(LegacyHeldInventoryItem.loadContents(player.getItemInHand(hand), player.registryAccess(), kind));
        }

        @Override public int getContainerSize() { return kind.slots(); }
        @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
        @Override public ItemStack getItem(int slot) { return items.get(slot); }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            ItemStack stack = items.get(slot);
            if (stack.isEmpty()) return ItemStack.EMPTY;
            ItemStack removed = stack.split(amount);
            if (stack.isEmpty()) items.set(slot, ItemStack.EMPTY);
            setChanged();
            return removed;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            ItemStack removed = items.get(slot);
            items.set(slot, ItemStack.EMPTY);
            return removed;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            items.set(slot, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
            setChanged();
        }

        @Override
        public void setChanged() { save(); }

        @Override
        public boolean stillValid(Player player) {
            ItemStack held = player.getItemInHand(hand);
            return held.getItem() instanceof LegacyHeldInventoryItem item && item.kind() == kind;
        }

        @Override
        public void clearContent() {
            for (int slot = 0; slot < items.size(); slot++) items.set(slot, ItemStack.EMPTY);
            save();
        }

        private void save() {
            ItemStack held = player.getItemInHand(hand);
            if (held.getItem() instanceof LegacyHeldInventoryItem item && item.kind() == kind) {
                LegacyHeldInventoryItem.saveContents(held, items, player.registryAccess(), kind);
            }
        }
    }
}
