package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.LargeTurbineBlockEntity;
import com.reinhardt.hbm.item.FluidIdentifierItem;
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

public class LargeTurbineMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOT_COUNT = LargeTurbineBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Container container;
    private final ContainerData data;

    public LargeTurbineMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, getContainer(playerInventory, buffer.readBlockPos()), new SimpleContainerData(LargeTurbineBlockEntity.DATA_COUNT));
    }

    public LargeTurbineMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(HbmMenus.LARGE_TURBINE.get(), containerId);
        checkContainerSize(container, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, LargeTurbineBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;

        this.addSlot(new ValidatedSlot(container, LargeTurbineBlockEntity.ID_SLOT, 8, 17));
        this.addSlot(new TakeOnlySlot(container, LargeTurbineBlockEntity.ID_RESULT_SLOT, 8, 53));
        this.addSlot(new ValidatedSlot(container, LargeTurbineBlockEntity.INPUT_CONTAINER_SLOT, 44, 17));
        this.addSlot(new TakeOnlySlot(container, LargeTurbineBlockEntity.INPUT_CONTAINER_RESULT_SLOT, 44, 53));
        this.addSlot(new ValidatedSlot(container, LargeTurbineBlockEntity.BATTERY_SLOT, 98, 53));
        this.addSlot(new ValidatedSlot(container, LargeTurbineBlockEntity.OUTPUT_CONTAINER_SLOT, 152, 17));
        this.addSlot(new TakeOnlySlot(container, LargeTurbineBlockEntity.OUTPUT_CONTAINER_RESULT_SLOT, 152, 53));
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
        } else if (stack.getItem() instanceof FluidIdentifierItem) {
            if (!moveItemStackTo(stack, LargeTurbineBlockEntity.ID_SLOT, LargeTurbineBlockEntity.ID_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (this.container.canPlaceItem(LargeTurbineBlockEntity.INPUT_CONTAINER_SLOT, stack)) {
            if (!moveItemStackTo(stack, LargeTurbineBlockEntity.INPUT_CONTAINER_SLOT, LargeTurbineBlockEntity.INPUT_CONTAINER_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (this.container.canPlaceItem(LargeTurbineBlockEntity.OUTPUT_CONTAINER_SLOT, stack)) {
            if (!moveItemStackTo(stack, LargeTurbineBlockEntity.OUTPUT_CONTAINER_SLOT, LargeTurbineBlockEntity.OUTPUT_CONTAINER_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (this.container.canPlaceItem(LargeTurbineBlockEntity.BATTERY_SLOT, stack)) {
            if (!moveItemStackTo(stack, LargeTurbineBlockEntity.BATTERY_SLOT, LargeTurbineBlockEntity.BATTERY_SLOT + 1, false)) {
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

    public com.reinhardt.hbm.fluid.HbmFluidDefinition inputFluid() {
        return HbmFluids.byOldId(this.data.get(0)).orElse(HbmFluids.none());
    }

    public int inputAmount() {
        return this.data.get(1);
    }

    public int inputCapacity() {
        return LargeTurbineBlockEntity.INPUT_CAPACITY;
    }

    public com.reinhardt.hbm.fluid.HbmFluidDefinition outputFluid() {
        return HbmFluids.byOldId(this.data.get(2)).orElse(HbmFluids.none());
    }

    public int outputAmount() {
        return this.data.get(3);
    }

    public int outputCapacity() {
        return Math.max(1, this.data.get(4));
    }

    public int power() {
        return this.data.get(5);
    }

    public int lastOutput() {
        return this.data.get(6);
    }

    public int powerScaled(int pixels) {
        return Math.min(pixels, (int) (this.power() * (long) pixels / LargeTurbineBlockEntity.ENERGY_CAPACITY));
    }

    public int inputScaled(int pixels) {
        return Math.min(pixels, this.inputAmount() * pixels / Math.max(1, inputCapacity()));
    }

    public int outputScaled(int pixels) {
        return Math.min(pixels, this.outputAmount() * pixels / Math.max(1, outputCapacity()));
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
        if (blockEntity instanceof LargeTurbineBlockEntity turbine) {
            return turbine;
        }
        return new SimpleContainer(MACHINE_SLOT_COUNT);
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

    private static final class TakeOnlySlot extends Slot {
        private TakeOnlySlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
