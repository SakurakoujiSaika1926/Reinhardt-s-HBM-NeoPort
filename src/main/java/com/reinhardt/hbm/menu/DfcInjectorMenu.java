package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.DfcInjectorBlockEntity;
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

public class DfcInjectorMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOT_COUNT = DfcInjectorBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Container container;
    private final ContainerData data;
    private final BlockPos blockPos;

    public DfcInjectorMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, getContainer(playerInventory, buffer.readBlockPos()), new SimpleContainerData(DfcInjectorBlockEntity.DATA_COUNT));
    }

    public DfcInjectorMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(HbmMenus.DFC_INJECTOR.get(), containerId);
        checkContainerSize(container, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, DfcInjectorBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;
        this.blockPos = container instanceof DfcInjectorBlockEntity injector ? injector.getBlockPos() : BlockPos.ZERO;

        this.addSlot(new ValidatedSlot(container, DfcInjectorBlockEntity.INPUT_0, 26, 17));
        this.addSlot(new OutputSlot(container, DfcInjectorBlockEntity.OUTPUT_0, 26, 53));
        this.addSlot(new ValidatedSlot(container, DfcInjectorBlockEntity.INPUT_1, 134, 17));
        this.addSlot(new OutputSlot(container, DfcInjectorBlockEntity.OUTPUT_1, 134, 53));
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
        } else if (this.container.canPlaceItem(DfcInjectorBlockEntity.INPUT_0, stack)) {
            if (!moveItemStackTo(stack, DfcInjectorBlockEntity.INPUT_0, DfcInjectorBlockEntity.INPUT_0 + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (this.container.canPlaceItem(DfcInjectorBlockEntity.INPUT_1, stack)) {
            if (!moveItemStackTo(stack, DfcInjectorBlockEntity.INPUT_1, DfcInjectorBlockEntity.INPUT_1 + 1, false)) {
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

    public int beam() {
        return this.data.get(6);
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
        return blockEntity instanceof DfcInjectorBlockEntity injector ? injector : new SimpleContainer(MACHINE_SLOT_COUNT);
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

    private static class OutputSlot extends Slot {
        private OutputSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
