package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.StorageCrateBlockEntity;
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

public class StorageCrateMenu extends AbstractContainerMenu {
    private final Container container;
    private final StorageCrateBlockEntity.Kind kind;
    private final int crateSlotCount;
    private final int playerInventoryStart;
    private final int playerInventoryEnd;
    private final int hotbarStart;
    private final int hotbarEnd;

    public StorageCrateMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, getContainer(playerInventory, buffer.readBlockPos()));
    }

    public StorageCrateMenu(int containerId, Inventory playerInventory, StorageCrateBlockEntity crate) {
        this(containerId, playerInventory, (Container) crate);
    }

    private StorageCrateMenu(int containerId, Inventory playerInventory, Container container) {
        super(HbmMenus.STORAGE_CRATE.get(), containerId);
        this.container = container;
        this.kind = container instanceof StorageCrateBlockEntity crate ? crate.kind() : StorageCrateBlockEntity.Kind.IRON;
        this.crateSlotCount = this.kind.slots();
        this.playerInventoryStart = this.crateSlotCount;
        this.playerInventoryEnd = this.playerInventoryStart + 27;
        this.hotbarStart = this.playerInventoryEnd;
        this.hotbarEnd = this.hotbarStart + 9;
        checkContainerSize(container, this.crateSlotCount);

        for (int row = 0; row < this.kind.rows(); row++) {
            for (int column = 0; column < this.kind.columns(); column++) {
                int slot = column + row * this.kind.columns();
                this.addSlot(new Slot(container, slot, this.kind.crateX() + column * 18, this.kind.crateY() + row * 18));
            }
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

        if (index < this.crateSlotCount) {
            if (!moveItemStackTo(stack, this.playerInventoryStart, this.hotbarEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, this.crateSlotCount, false)) {
            if (index < this.playerInventoryEnd) {
                if (!moveItemStackTo(stack, this.hotbarStart, this.hotbarEnd, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, this.playerInventoryStart, this.playerInventoryEnd, false)) {
                return ItemStack.EMPTY;
            }
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

    public StorageCrateBlockEntity.Kind kind() {
        return this.kind;
    }

    public boolean hot() {
        return this.container instanceof StorageCrateBlockEntity crate && crate.heatTimer() > 0;
    }

    public long joules() {
        return this.container instanceof StorageCrateBlockEntity crate ? crate.joules() : 0L;
    }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(inventory, column + row * 9 + 9, this.kind.playerInventoryX() + column * 18, this.kind.playerInventoryY() + row * 18));
            }
        }

        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(inventory, column, this.kind.playerInventoryX() + column * 18, this.kind.hotbarY()));
        }
    }

    private static Container getContainer(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof StorageCrateBlockEntity crate) {
            return crate;
        }
        return new SimpleContainer(StorageCrateBlockEntity.Kind.IRON.slots());
    }
}
