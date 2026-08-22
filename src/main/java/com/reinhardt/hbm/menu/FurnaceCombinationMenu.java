package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.FurnaceCombinationBlockEntity;
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

public class FurnaceCombinationMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOT_COUNT = FurnaceCombinationBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Container container;
    private final ContainerData data;

    public FurnaceCombinationMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, getContainer(playerInventory, buffer.readBlockPos()), new SimpleContainerData(FurnaceCombinationBlockEntity.DATA_COUNT));
    }

    public FurnaceCombinationMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(HbmMenus.FURNACE_COMBINATION.get(), containerId);
        checkContainerSize(container, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, FurnaceCombinationBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;

        this.addSlot(new ValidatedSlot(container, FurnaceCombinationBlockEntity.INPUT_SLOT, 26, 36));
        this.addSlot(new TakeOnlySlot(container, FurnaceCombinationBlockEntity.ITEM_OUTPUT_SLOT, 89, 36));
        this.addSlot(new ValidatedSlot(container, FurnaceCombinationBlockEntity.CONTAINER_INPUT_SLOT, 136, 18));
        this.addSlot(new TakeOnlySlot(container, FurnaceCombinationBlockEntity.CONTAINER_OUTPUT_SLOT, 136, 54));

        addPlayerInventory(playerInventory, 8, 104);
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
        } else if (this.container.canPlaceItem(FurnaceCombinationBlockEntity.INPUT_SLOT, stack)
                && moveItemStackTo(stack, FurnaceCombinationBlockEntity.INPUT_SLOT, FurnaceCombinationBlockEntity.INPUT_SLOT + 1, false)) {
            return moved;
        } else if (this.container.canPlaceItem(FurnaceCombinationBlockEntity.CONTAINER_INPUT_SLOT, stack)
                && moveItemStackTo(stack, FurnaceCombinationBlockEntity.CONTAINER_INPUT_SLOT, FurnaceCombinationBlockEntity.CONTAINER_INPUT_SLOT + 1, false)) {
            return moved;
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

    public HbmFluidDefinition fluid() {
        return HbmFluids.byOldId(this.data.get(0)).orElse(HbmFluids.none());
    }

    public int fluidAmount() {
        return this.data.get(1);
    }

    public int fluidCapacity() {
        return Math.max(1, this.data.get(2));
    }

    public int progress() {
        return this.data.get(3);
    }

    public int processTime() {
        return Math.max(1, this.data.get(4));
    }

    public int heat() {
        return this.data.get(5);
    }

    public int maxHeat() {
        return Math.max(1, this.data.get(6));
    }

    public int fluidScaled(int pixels) {
        return Math.min(pixels, this.fluidAmount() * pixels / fluidCapacity());
    }

    public int progressScaled(int pixels) {
        return Math.min(pixels, this.progress() * pixels / processTime());
    }

    public int heatScaled(int pixels) {
        return Math.min(pixels, this.heat() * pixels / maxHeat());
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
        if (blockEntity instanceof FurnaceCombinationBlockEntity furnace) {
            return furnace;
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
