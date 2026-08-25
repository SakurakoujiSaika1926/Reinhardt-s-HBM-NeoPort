package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.item.LegacyCraftBookItem;
import com.reinhardt.hbm.recipe.LegacyCraftBookRecipes;
import com.reinhardt.hbm.registry.HbmMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Server-authoritative temporary inventories for ItemBook and ItemBookLemegeton. */
public final class LegacyCraftBookMenu extends AbstractContainerMenu {
    private static final int PLAYER_SLOT_COUNT = 36;

    private final Player player;
    private final InteractionHand hand;
    private final LegacyCraftBookItem.Kind kind;
    private final SimpleContainer input;
    private final SimpleContainer result = new SimpleContainer(1);
    private final int inputSlots;
    private final int playerStart;

    public LegacyCraftBookMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, buffer.readBoolean() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND,
                LegacyCraftBookItem.Kind.values()[Byte.toUnsignedInt(buffer.readByte())]);
    }

    public LegacyCraftBookMenu(int containerId, Inventory inventory, InteractionHand hand, LegacyCraftBookItem.Kind kind) {
        super(HbmMenus.CRAFT_BOOK.get(), containerId);
        this.player = inventory.player;
        this.hand = hand;
        this.kind = kind;
        this.inputSlots = kind == LegacyCraftBookItem.Kind.BOXCARS ? 4 : 1;
        this.input = new SimpleContainer(inputSlots) {
            @Override
            public void setChanged() {
                super.setChanged();
                LegacyCraftBookMenu.this.updateResult();
            }
        };

        if (kind == LegacyCraftBookItem.Kind.BOXCARS) {
            addSlot(new OutputSlot(124, 35));
            for (int row = 0; row < 2; row++) {
                for (int column = 0; column < 2; column++) {
                    addSlot(new Slot(this.input, column + row * 2, 30 + column * 36, 17 + row * 36));
                }
            }
        } else {
            addSlot(new OutputSlot(107, 35));
            addSlot(new Slot(this.input, 0, 49, 35));
        }

        this.playerStart = inputSlots + 1;
        addPlayerInventory(inventory);
        updateResult();
    }

    public LegacyCraftBookItem.Kind kind() {
        return kind;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size() || !slots.get(index).hasItem()) {
            return ItemStack.EMPTY;
        }
        Slot source = slots.get(index);
        ItemStack stack = source.getItem();
        ItemStack moved = stack.copy();

        if (index == 0) {
            if (!moveItemStackTo(stack, playerStart, playerStart + PLAYER_SLOT_COUNT, true)) {
                return ItemStack.EMPTY;
            }
            source.onTake(player, stack);
        } else if (index < playerStart) {
            if (!moveItemStackTo(stack, playerStart, playerStart + PLAYER_SLOT_COUNT, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 1, playerStart, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            source.setByPlayer(ItemStack.EMPTY);
        } else {
            source.setChanged();
        }
        return moved;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        int heldSlot = hand == InteractionHand.MAIN_HAND ? player.getInventory().selected : -1;
        int hotbarStart = playerStart + 27;
        if ((clickType == ClickType.SWAP && button == heldSlot) || (heldSlot >= 0 && slotId == hotbarStart + heldSlot)) {
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public boolean stillValid(Player player) {
        if (isBook(player.getItemInHand(hand))) {
            return true;
        }
        for (ItemStack stack : player.getInventory().items) {
            if (isBook(stack)) {
                return true;
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (isBook(stack)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (player.level().isClientSide) {
            return;
        }
        for (int slot = 0; slot < inputSlots; slot++) {
            ItemStack stack = input.removeItemNoUpdate(slot);
            if (!stack.isEmpty()) {
                player.drop(stack, false);
            }
        }
    }

    private boolean isBook(ItemStack stack) {
        return stack.getItem() instanceof LegacyCraftBookItem book && book.kind() == kind;
    }

    private void updateResult() {
        ItemStack crafted = kind == LegacyCraftBookItem.Kind.BOXCARS
                ? LegacyCraftBookRecipes.magicResult(input)
                : LegacyCraftBookRecipes.lemegetonResult(input.getItem(0));
        result.setItem(0, crafted);
    }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 142));
        }
    }

    private final class OutputSlot extends Slot {
        private OutputSlot(int x, int y) {
            super(LegacyCraftBookMenu.this.result, 0, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public void onTake(Player player, ItemStack stack) {
            for (int slot = 0; slot < inputSlots; slot++) {
                input.removeItem(slot, 1);
            }
            updateResult();
            super.onTake(player, stack);
        }
    }
}
