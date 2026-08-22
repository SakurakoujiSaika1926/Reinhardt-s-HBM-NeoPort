package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.CokerBlockEntity;
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

public class CokerMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOT_COUNT = CokerBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Container container;
    private final ContainerData data;

    public CokerMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, getContainer(playerInventory, buffer.readBlockPos()), new SimpleContainerData(CokerBlockEntity.DATA_COUNT));
    }

    public CokerMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(HbmMenus.COKER.get(), containerId);
        checkContainerSize(container, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, CokerBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;

        this.addSlot(new ValidatedSlot(container, CokerBlockEntity.FLUID_IDENTIFIER_SLOT, 35, 72));
        this.addSlot(new TakeOnlySlot(container, CokerBlockEntity.ITEM_OUTPUT_SLOT, 97, 27));

        addPlayerInventory(playerInventory, 8, 122);
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
        } else if (this.container.canPlaceItem(CokerBlockEntity.FLUID_IDENTIFIER_SLOT, stack)
                && moveItemStackTo(stack, CokerBlockEntity.FLUID_IDENTIFIER_SLOT, CokerBlockEntity.FLUID_IDENTIFIER_SLOT + 1, false)) {
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

    public HbmFluidDefinition inputFluid() {
        return HbmFluids.byOldId(this.data.get(0)).orElse(HbmFluids.none());
    }

    public int inputAmount() {
        return this.data.get(1);
    }

    public int inputCapacity() {
        return Math.max(1, this.data.get(2));
    }

    public HbmFluidDefinition outputFluid() {
        return HbmFluids.byOldId(this.data.get(3)).orElse(HbmFluids.none());
    }

    public int outputAmount() {
        return this.data.get(4);
    }

    public int outputCapacity() {
        return Math.max(1, this.data.get(5));
    }

    public int progress() {
        return this.data.get(6);
    }

    public int processTime() {
        return Math.max(1, this.data.get(7));
    }

    public int heat() {
        return this.data.get(8);
    }

    public int maxHeat() {
        return Math.max(1, this.data.get(9));
    }

    public boolean working() {
        return this.data.get(10) != 0;
    }

    public int inputScaled(int pixels) {
        return Math.min(pixels, this.inputAmount() * pixels / inputCapacity());
    }

    public int outputScaled(int pixels) {
        return Math.min(pixels, this.outputAmount() * pixels / outputCapacity());
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
        if (blockEntity instanceof CokerBlockEntity coker) {
            return coker;
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
