package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.AshpitBlockEntity;
import com.reinhardt.hbm.registry.HbmMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class AshpitMenu extends AbstractContainerMenu {
    private static final int ASHPIT_SLOTS = 5;
    private static final int PLAYER_INVENTORY_START = ASHPIT_SLOTS;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Container container;

    public AshpitMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, getContainer(playerInventory, buffer.readBlockPos()));
    }

    public AshpitMenu(int containerId, Inventory playerInventory, AshpitBlockEntity ashpit) {
        this(containerId, playerInventory, (Container) ashpit);
    }

    private AshpitMenu(int containerId, Inventory playerInventory, Container container) {
        super(HbmMenus.ASHPIT.get(), containerId);
        this.container = container;
        checkContainerSize(container, ASHPIT_SLOTS);

        for (int slot = 0; slot < ASHPIT_SLOTS; slot++) {
            this.addSlot(new OutputOnlySlot(container, slot, 44 + slot * 18, 27));
        }

        addPlayerInventory(playerInventory);
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

        if (index < ASHPIT_SLOTS) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else {
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
        return this.container.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide && this.container instanceof AshpitBlockEntity ashpit) {
            ashpit.stopOpen(player);
        }
    }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 86 + row * 18));
            }
        }

        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(inventory, column, 8 + column * 18, 144));
        }
    }

    private static Container getContainer(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof AshpitBlockEntity ashpit) {
            return ashpit;
        }
        return new SimpleContainer(ASHPIT_SLOTS);
    }

    private static final class OutputOnlySlot extends Slot {
        private OutputOnlySlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
