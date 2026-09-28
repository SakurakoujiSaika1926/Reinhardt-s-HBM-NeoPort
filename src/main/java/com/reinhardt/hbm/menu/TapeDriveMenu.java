package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.TapeDriveBlockEntity;
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

/** Literal 2x6 slot layout from ContainerTapeDrive. */
public final class TapeDriveMenu extends AbstractContainerMenu {
    private static final int MACHINE_END = TapeDriveBlockEntity.SLOT_COUNT;
    private static final int PLAYER_START = MACHINE_END;
    private static final int PLAYER_END = PLAYER_START + 36;

    private final Container container;

    public TapeDriveMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, findDrive(inventory, buffer.readBlockPos()));
    }

    public TapeDriveMenu(int containerId, Inventory inventory, TapeDriveBlockEntity drive) {
        super(HbmMenus.TAPE_DRIVE.get(), containerId);
        this.container = drive == null ? new SimpleContainer(TapeDriveBlockEntity.SLOT_COUNT) : drive;
        checkContainerSize(this.container, TapeDriveBlockEntity.SLOT_COUNT);

        for (int row = 0; row < 2; row++) {
            for (int column = 0; column < 6; column++) {
                this.addSlot(new DriveSlot(this.container, column + row * 6,
                        35 + column * 18, 27 + row * 18));
            }
        }
        addPlayerInventory(inventory, 8, 104);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= this.slots.size()) return ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack result = stack.copy();
        if (index < MACHINE_END) {
            if (!moveItemStackTo(stack, PLAYER_START, PLAYER_END, true)) return ItemStack.EMPTY;
        } else {
            if (!TapeDriveBlockEntity.isDrive(stack)
                    || !moveItemStackTo(stack, 0, MACHINE_END, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    private static TapeDriveBlockEntity findDrive(Inventory inventory, BlockPos pos) {
        BlockEntity entity = inventory.player.level().getBlockEntity(pos);
        return entity instanceof TapeDriveBlockEntity drive ? drive : null;
    }

    private void addPlayerInventory(Inventory inventory, int left, int top) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(inventory, column + row * 9 + 9,
                        left + column * 18, top + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(inventory, column, left + column * 18, top + 58));
        }
    }

    private static final class DriveSlot extends Slot {
        private DriveSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override public boolean mayPlace(ItemStack stack) { return TapeDriveBlockEntity.isDrive(stack); }
        @Override public int getMaxStackSize() { return 1; }
    }
}
