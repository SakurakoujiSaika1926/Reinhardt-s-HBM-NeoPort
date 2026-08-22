package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.WatzBlockEntity;
import com.reinhardt.hbm.item.WatzPelletItem;
import com.reinhardt.hbm.registry.HbmMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class WatzMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOT_COUNT = WatzBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Container container;
    private final ContainerData data;
    private final BlockPos blockPos;

    public WatzMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, context(playerInventory, buffer));
    }

    private WatzMenu(int containerId, Inventory playerInventory, MenuContext context) {
        this(containerId, playerInventory, context.container(), new SimpleContainerData(WatzBlockEntity.DATA_COUNT), context.blockPos());
    }

    public WatzMenu(int containerId, Inventory playerInventory, Container container, ContainerData data, BlockPos blockPos) {
        super(HbmMenus.WATZ.get(), containerId);
        checkContainerSize(container, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, WatzBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;
        this.blockPos = blockPos;

        addPelletSlots(container);
        addPlayerInventory(playerInventory, 8, 147);
        addDataSlots(data);
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

        if (index < MACHINE_SLOT_COUNT) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (WatzPelletItem.isActivePellet(stack)) {
            if (!moveItemStackTo(stack, 0, MACHINE_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < PLAYER_INVENTORY_END) {
            if (!moveItemStackTo(stack, HOTBAR_START, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, false)) {
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
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0 && this.container instanceof WatzBlockEntity watz && stillValid(player)) {
            watz.toggleLock();
            return true;
        }
        return false;
    }

    public BlockPos blockPos() {
        return this.blockPos;
    }

    public int heat() {
        return this.data.get(0);
    }

    public boolean on() {
        return this.data.get(1) != 0;
    }

    public boolean locked() {
        return this.data.get(2) != 0;
    }

    public int flux() {
        return this.data.get(3);
    }

    public int tankFluidOldId(int tank) {
        return this.data.get(4 + tank * 3);
    }

    public int tankAmount(int tank) {
        return this.data.get(5 + tank * 3);
    }

    public int tankCapacity(int tank) {
        return Math.max(1, this.data.get(6 + tank * 3));
    }

    public int tankScaled(int tank, int pixels) {
        return Math.min(pixels, this.tankAmount(tank) * pixels / this.tankCapacity(tank));
    }

    public int baseFlux() {
        return this.data.get(13);
    }

    public int reactionFlux() {
        return this.data.get(14);
    }

    private void addPelletSlots(Container container) {
        int index = 0;
        for (int row = 0; row < 6; row++) {
            for (int column = 0; column < 6; column++) {
                if (column + row > 1 && column + row < 9 && 5 - column + row > 1 && column + 5 - row > 1) {
                    this.addSlot(new PelletSlot(container, index, 17 + column * 18, 8 + row * 18));
                    index++;
                }
            }
        }
    }

    private void addPlayerInventory(Inventory inventory, int left, int top) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(inventory, column + row * 9 + 9, left + column * 18, top + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(inventory, column, left + column * 18, top + 58));
        }
    }

    private static Container getContainer(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof WatzBlockEntity watz) {
            return watz;
        }
        return new SimpleContainer(MACHINE_SLOT_COUNT);
    }

    private static MenuContext context(Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        return new MenuContext(getContainer(playerInventory, pos), pos);
    }

    private record MenuContext(Container container, BlockPos blockPos) {
    }

    private static final class PelletSlot extends Slot {
        private PelletSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return this.container.canPlaceItem(this.index, stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }
}
