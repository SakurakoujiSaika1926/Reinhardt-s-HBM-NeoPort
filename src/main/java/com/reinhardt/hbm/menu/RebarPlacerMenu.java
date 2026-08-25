package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.item.LegacyRebarPlacerItem;
import com.reinhardt.hbm.registry.HbmMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** One-slot persistent inventory used by the held rebar placer. */
public final class RebarPlacerMenu extends AbstractContainerMenu {
    private static final int PLAYER_START = 1;
    private static final int PLAYER_END = PLAYER_START + 36;
    private final InteractionHand hand;
    private final SelectedConcreteContainer selected;

    public RebarPlacerMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, buffer.readBoolean() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
    }

    public RebarPlacerMenu(int containerId, Inventory inventory, InteractionHand hand) {
        super(HbmMenus.REBAR_PLACER.get(), containerId);
        this.hand = hand;
        this.selected = new SelectedConcreteContainer(inventory.player, hand);
        addSlot(new SelectedSlot(this.selected, 0, 53, 36));
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 100 + row * 18));
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 158));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size() || !slots.get(index).hasItem()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        ItemStack stack = slot.getItem();
        ItemStack result = stack.copy();
        if (index == 0) {
            if (!moveItemStackTo(stack, PLAYER_START, PLAYER_END, true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, 1, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getItemInHand(this.hand).getItem() instanceof LegacyRebarPlacerItem;
    }

    private static final class SelectedSlot extends Slot {
        private SelectedSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return LegacyRebarPlacerItem.isValidConcrete(stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    private static final class SelectedConcreteContainer implements Container {
        private final Player player;
        private final InteractionHand hand;
        private ItemStack selected;

        private SelectedConcreteContainer(Player player, InteractionHand hand) {
            this.player = player;
            this.hand = hand;
            this.selected = LegacyRebarPlacerItem.selectedConcrete(player.getItemInHand(hand), player.registryAccess());
        }

        @Override public int getContainerSize() { return 1; }
        @Override public boolean isEmpty() { return selected.isEmpty(); }
        @Override public ItemStack getItem(int slot) { return slot == 0 ? selected : ItemStack.EMPTY; }
        @Override public ItemStack removeItem(int slot, int amount) { if (slot != 0) return ItemStack.EMPTY; ItemStack result = selected.split(amount); if (selected.isEmpty()) selected = ItemStack.EMPTY; setChanged(); return result; }
        @Override public ItemStack removeItemNoUpdate(int slot) { if (slot != 0) return ItemStack.EMPTY; ItemStack result = selected; selected = ItemStack.EMPTY; return result; }
        @Override public void setItem(int slot, ItemStack stack) { if (slot == 0) { selected = stack.copyWithCount(Math.min(1, stack.getCount())); setChanged(); } }
        @Override public void setChanged() { LegacyRebarPlacerItem.setSelectedConcrete(player.getItemInHand(hand), selected, player.registryAccess()); }
        @Override public boolean stillValid(Player player) { return player.getItemInHand(hand).getItem() instanceof LegacyRebarPlacerItem; }
        @Override public void clearContent() { selected = ItemStack.EMPTY; setChanged(); }
    }
}
