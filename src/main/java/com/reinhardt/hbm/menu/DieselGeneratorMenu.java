package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.DieselGeneratorBlockEntity;
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

public class DieselGeneratorMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOT_COUNT = DieselGeneratorBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Container container;
    private final ContainerData data;
    private final BlockPos blockPos;

    public DieselGeneratorMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, getContainer(playerInventory, buffer.readBlockPos()), new SimpleContainerData(DieselGeneratorBlockEntity.DATA_COUNT));
    }

    public DieselGeneratorMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(HbmMenus.DIESEL_GENERATOR.get(), containerId);
        checkContainerSize(container, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, DieselGeneratorBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;
        this.blockPos = container instanceof DieselGeneratorBlockEntity diesel ? diesel.getBlockPos().immutable() : BlockPos.ZERO;

        this.addSlot(new ValidatedSlot(container, DieselGeneratorBlockEntity.SLOT_INPUT, 44, 17));
        this.addSlot(new TakeOnlySlot(container, DieselGeneratorBlockEntity.SLOT_OUTPUT, 44, 53));
        this.addSlot(new ValidatedSlot(container, DieselGeneratorBlockEntity.SLOT_BATTERY, 116, 53));
        this.addSlot(new ValidatedSlot(container, DieselGeneratorBlockEntity.SLOT_IDENTIFIER_INPUT, 8, 17));
        this.addSlot(new TakeOnlySlot(container, DieselGeneratorBlockEntity.SLOT_IDENTIFIER_OUTPUT, 8, 53));
        addPlayerInventory(playerInventory, 8, 84);
        addDataSlots(data);
    }

    public int power() {
        return this.data.get(0);
    }

    public int powerCap() {
        return Math.max(1, this.data.get(1));
    }

    public boolean running() {
        return this.data.get(2) != 0;
    }

    public com.reinhardt.hbm.fluid.HbmFluidDefinition fuelFluid() {
        return HbmFluids.byOldId(this.data.get(3)).orElse(HbmFluids.none());
    }

    public int fuelAmount() {
        return this.data.get(4);
    }

    public int fuelCapacity() {
        return Math.max(1, this.data.get(5));
    }

    public int hePerTick() {
        return this.data.get(6);
    }

    public int powerScaled(int pixels) {
        return Math.min(pixels, (int) (this.power() * (long) pixels / this.powerCap()));
    }

    public int fuelScaled(int pixels) {
        return Math.min(pixels, this.fuelAmount() * pixels / this.fuelCapacity());
    }

    public BlockPos blockPos() {
        return this.blockPos;
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
        } else if (this.container.canPlaceItem(DieselGeneratorBlockEntity.SLOT_IDENTIFIER_INPUT, stack)) {
            if (!moveItemStackTo(stack, DieselGeneratorBlockEntity.SLOT_IDENTIFIER_INPUT, DieselGeneratorBlockEntity.SLOT_IDENTIFIER_INPUT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (this.container.canPlaceItem(DieselGeneratorBlockEntity.SLOT_INPUT, stack)) {
            if (!moveItemStackTo(stack, DieselGeneratorBlockEntity.SLOT_INPUT, DieselGeneratorBlockEntity.SLOT_INPUT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (this.container.canPlaceItem(DieselGeneratorBlockEntity.SLOT_BATTERY, stack)) {
            if (!moveItemStackTo(stack, DieselGeneratorBlockEntity.SLOT_BATTERY, DieselGeneratorBlockEntity.SLOT_BATTERY + 1, false)) {
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
        if (blockEntity instanceof DieselGeneratorBlockEntity diesel) {
            return diesel;
        }
        return new SimpleContainer(MACHINE_SLOT_COUNT);
    }

    private static final class ValidatedSlot extends Slot {
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
