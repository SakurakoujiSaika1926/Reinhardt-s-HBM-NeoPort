package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.block.FluidBarrelBlock;
import com.reinhardt.hbm.blockentity.FluidTankBlockEntity;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
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

public class FluidTankMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOT_COUNT = FluidTankBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Container container;
    private final FluidTankBlockEntity tank;
    private final ContainerData data;
    private final boolean barrel;

    public FluidTankMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(
                containerId,
                playerInventory,
                getContainer(playerInventory, buffer.readBlockPos()),
                new SimpleContainerData(FluidTankBlockEntity.DATA_COUNT)
        );
    }

    public FluidTankMenu(int containerId, Inventory playerInventory, FluidTankBlockEntity tank, ContainerData data) {
        this(containerId, playerInventory, tank, data, isBarrel(tank));
    }

    private FluidTankMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        this(containerId, playerInventory, container, data, isBarrel(container));
    }

    private FluidTankMenu(int containerId, Inventory playerInventory, Container container, ContainerData data, boolean barrel) {
        super(HbmMenus.FLUID_TANK.get(), containerId);
        checkContainerSize(container, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, FluidTankBlockEntity.DATA_COUNT);
        this.container = container;
        this.tank = container instanceof FluidTankBlockEntity fluidTank ? fluidTank : null;
        this.data = data;
        this.barrel = barrel;

        this.addSlot(new ValidatedSlot(container, FluidTankBlockEntity.ID_SLOT, 8, 17));
        this.addSlot(new TakeOnlySlot(container, FluidTankBlockEntity.ID_RESULT_SLOT, 8, 53));
        this.addSlot(new ValidatedSlot(container, FluidTankBlockEntity.INPUT_CONTAINER_SLOT, 35, 17));
        this.addSlot(new TakeOnlySlot(container, FluidTankBlockEntity.INPUT_CONTAINER_RESULT_SLOT, 35, 53));
        this.addSlot(new ValidatedSlot(container, FluidTankBlockEntity.OUTPUT_CONTAINER_SLOT, 125, 17));
        this.addSlot(new TakeOnlySlot(container, FluidTankBlockEntity.OUTPUT_CONTAINER_RESULT_SLOT, 125, 53));
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
            if (!moveItemStackTo(stack, FluidTankBlockEntity.ID_SLOT, FluidTankBlockEntity.ID_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (this.container.canPlaceItem(FluidTankBlockEntity.INPUT_CONTAINER_SLOT, stack)) {
            if (!moveItemStackTo(stack, FluidTankBlockEntity.INPUT_CONTAINER_SLOT, FluidTankBlockEntity.INPUT_CONTAINER_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (this.container.canPlaceItem(FluidTankBlockEntity.OUTPUT_CONTAINER_SLOT, stack)) {
            if (!moveItemStackTo(stack, FluidTankBlockEntity.OUTPUT_CONTAINER_SLOT, FluidTankBlockEntity.OUTPUT_CONTAINER_SLOT + 1, false)) {
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
    public boolean clickMenuButton(Player player, int id) {
        if (id != 0 || this.tank == null || !stillValid(player)) {
            return false;
        }
        this.tank.cycleMode();
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    public HbmFluidDefinition fluid() {
        return HbmFluids.byOldId(this.data.get(0)).orElse(HbmFluids.none());
    }

    public int amount() {
        return this.data.get(1);
    }

    public int capacity() {
        return this.data.get(2);
    }

    public int pressure() {
        return this.data.get(3);
    }

    public FluidTankBlockEntity.Mode mode() {
        return FluidTankBlockEntity.Mode.byOrdinal(this.data.get(4));
    }

    public int fluidScaled(int pixels) {
        return this.capacity() <= 0 ? 0 : Math.min(pixels, this.amount() * pixels / Math.max(1, this.capacity()));
    }

    public boolean isBarrel() {
        return this.barrel;
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
        if (blockEntity instanceof FluidTankBlockEntity tank) {
            return tank;
        }
        return new SimpleContainer(MACHINE_SLOT_COUNT);
    }

    private static boolean isBarrel(Container container) {
        if (container instanceof FluidTankBlockEntity tank) {
            return isBarrel(tank);
        }
        return false;
    }

    private static boolean isBarrel(FluidTankBlockEntity tank) {
        return tank != null && tank.getBlockState().getBlock() instanceof FluidBarrelBlock;
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
