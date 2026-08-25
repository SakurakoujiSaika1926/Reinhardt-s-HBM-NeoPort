package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.DroneRequesterBlockEntity;
import com.reinhardt.hbm.registry.HbmMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Exact 1.7.10 requester layout: ghost-like filters at right and storage at left. */
public final class DroneRequesterMenu extends AbstractContainerMenu {
    private static final int SLOTS = 18;
    private final Container container;
    private final DroneRequesterBlockEntity requester;

    public DroneRequesterMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, resolve(inventory, buffer.readBlockPos()));
    }

    public DroneRequesterMenu(int id, Inventory inventory, DroneRequesterBlockEntity requester) {
        this(id, inventory, new Resolved(requester, requester));
    }

    private DroneRequesterMenu(int id, Inventory inventory, Resolved resolved) {
        super(HbmMenus.DRONE_REQUESTER.get(), id);
        this.container = resolved.container();
        this.requester = resolved.requester();
        checkContainerSize(container, SLOTS);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                addSlot(new FilterSlot(container, column + row * 3, 98 + column * 18, 17 + row * 18));
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                addSlot(new Slot(container, column + row * 3 + DroneRequesterBlockEntity.STOCK_START, 26 + column * 18, 17 + row * 18));
            }
        }
        addPlayerInventory(inventory);
    }

    public String filterMode(int slot) {
        return requester == null ? null : requester.filterMode(slot);
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < 9 && clickType == ClickType.PICKUP && requester != null) {
            if (button == 1) {
                requester.cycleFilterMode(slotId);
            } else if (button == 0) {
                requester.setFilter(slotId, getCarried());
            }
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 9) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack result = stack.copy();
        if (index < SLOTS) {
            if (!moveItemStackTo(stack, SLOTS, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, DroneRequesterBlockEntity.STOCK_START, SLOTS, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 103 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 161));
        }
    }

    private static Resolved resolve(Inventory inventory, BlockPos pos) {
        BlockEntity entity = inventory.player.level().getBlockEntity(pos);
        return entity instanceof DroneRequesterBlockEntity requester
                ? new Resolved(requester, requester)
                : new Resolved(new SimpleContainer(SLOTS), null);
    }

    private record Resolved(Container container, DroneRequesterBlockEntity requester) {
    }

    private static final class FilterSlot extends Slot {
        private FilterSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return true;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }
    }
}
