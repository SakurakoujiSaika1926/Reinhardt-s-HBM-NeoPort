package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.DfcCoreBlockEntity;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.registry.HbmFluids;
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

public class DfcCoreMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOT_COUNT = DfcCoreBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Container container;
    private final ContainerData data;
    private final BlockPos blockPos;

    public DfcCoreMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, getContainer(playerInventory, buffer.readBlockPos()), new SimpleContainerData(DfcCoreBlockEntity.DATA_COUNT));
    }

    public DfcCoreMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(HbmMenus.DFC_CORE.get(), containerId);
        checkContainerSize(container, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, DfcCoreBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;
        this.blockPos = container instanceof DfcCoreBlockEntity core ? core.getBlockPos() : BlockPos.ZERO;

        this.addSlot(new ValidatedSlot(container, DfcCoreBlockEntity.CATALYST_LEFT_SLOT, 62, 53));
        this.addSlot(new ValidatedSlot(container, DfcCoreBlockEntity.CORE_SLOT, 80, 53));
        this.addSlot(new ValidatedSlot(container, DfcCoreBlockEntity.CATALYST_RIGHT_SLOT, 98, 53));
        addPlayerInventory(playerInventory, 8, 84);
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
        } else {
            boolean placed = false;
            for (int machineSlot = 0; machineSlot < MACHINE_SLOT_COUNT; machineSlot++) {
                if (this.container.canPlaceItem(machineSlot, stack) && moveItemStackTo(stack, machineSlot, machineSlot + 1, false)) {
                    placed = true;
                    break;
                }
            }
            if (!placed && index < PLAYER_INVENTORY_END) {
                if (!moveItemStackTo(stack, HOTBAR_START, HOTBAR_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!placed && !moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, false)) {
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

    public BlockPos blockPos() {
        return this.blockPos;
    }

    public HbmFluidDefinition tankFluid(int index) {
        return HbmFluids.byOldId(this.data.get(index == 0 ? 0 : 3)).orElse(HbmFluids.none());
    }

    public int tankAmount(int index) {
        return this.data.get(index == 0 ? 1 : 4);
    }

    public int tankCapacity(int index) {
        return this.data.get(index == 0 ? 2 : 5);
    }

    public int tankScaled(int index, int pixels) {
        return tankCapacity(index) <= 0 ? 0 : tankAmount(index) * pixels / tankCapacity(index);
    }

    public int field() {
        return this.data.get(6);
    }

    public int heat() {
        return this.data.get(7);
    }

    public int color() {
        return this.data.get(8);
    }

    public boolean meltdownTick() {
        return this.data.get(9) != 0;
    }

    public int fieldScaled(int pixels) {
        return Math.min(pixels, Math.max(0, field()) * pixels / 100);
    }

    public int heatScaled(int pixels) {
        return Math.min(pixels, Math.max(0, heat()) * pixels / 100);
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
        return blockEntity instanceof DfcCoreBlockEntity core ? core : new SimpleContainer(MACHINE_SLOT_COUNT);
    }

    private static class ValidatedSlot extends Slot {
        private ValidatedSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return this.container.canPlaceItem(this.index, stack);
        }
    }
}
