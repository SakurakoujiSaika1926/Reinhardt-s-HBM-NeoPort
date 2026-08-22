package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.LegacyMachineBlockEntity;
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

/** Exact six-slot layout of the 1.7.10 pyro oven container. */
public final class PyroOvenMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOT_COUNT = 6;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;
    private static final long ENERGY_CAPACITY = 10_000_000L;

    private final Container container;
    private final ContainerData data;

    public PyroOvenMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, getContainer(playerInventory, buffer.readBlockPos()),
                new SimpleContainerData(LegacyMachineBlockEntity.DATA_COUNT));
    }

    public PyroOvenMenu(int containerId, Inventory playerInventory, LegacyMachineBlockEntity machine) {
        this(containerId, playerInventory, machine, machine.menuData());
    }

    private PyroOvenMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(HbmMenus.PYRO_OVEN.get(), containerId);
        checkContainerSize(container, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, LegacyMachineBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;

        this.addSlot(new ValidatedSlot(container, 0, 152, 72));
        this.addSlot(new ValidatedSlot(container, 1, 35, 45));
        this.addSlot(new OutputSlot(container, 2, 89, 45));
        this.addSlot(new ValidatedSlot(container, 3, 8, 72));
        this.addSlot(new ValidatedSlot(container, 4, 71, 72));
        this.addSlot(new ValidatedSlot(container, 5, 89, 72));

        addPlayerInventory(playerInventory, 8, 122);
        addDataSlots(data);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        moved = stack.copy();
        if (index < MACHINE_SLOT_COUNT) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveToMachine(stack)) {
            if (index < PLAYER_INVENTORY_END) {
                if (!moveItemStackTo(stack, HOTBAR_START, HOTBAR_END, false)) return ItemStack.EMPTY;
            } else if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, false)) {
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

    private boolean moveToMachine(ItemStack stack) {
        for (int slot : new int[]{0, 3, 4, 5, 1}) {
            if (this.container.canPlaceItem(slot, stack)
                    && moveItemStackTo(stack, slot, slot + 1, false)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    public int energy() {
        return this.data.get(0);
    }

    public int energyScaled(int pixels) {
        return Math.min(pixels, (int) (this.energy() * (long) pixels / ENERGY_CAPACITY));
    }

    public HbmFluidDefinition inputFluid() {
        return HbmFluids.byOldId(this.data.get(5)).orElse(HbmFluids.none());
    }

    public int inputAmount() {
        return this.data.get(6);
    }

    public HbmFluidDefinition outputFluid() {
        return HbmFluids.byOldId(this.data.get(7)).orElse(HbmFluids.none());
    }

    public int outputAmount() {
        return this.data.get(8);
    }

    public int inputScaled(int pixels) {
        return Math.min(pixels, this.inputAmount() * pixels / 24_000);
    }

    public int outputScaled(int pixels) {
        return Math.min(pixels, this.outputAmount() * pixels / 24_000);
    }

    public int progress() {
        return this.data.get(2);
    }

    public int duration() {
        return Math.max(1, this.data.get(11));
    }

    public int progressScaled(int pixels) {
        return Math.min(pixels, this.progress() * pixels / this.duration());
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

    private static Container getContainer(Inventory inventory, BlockPos pos) {
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        return blockEntity instanceof LegacyMachineBlockEntity machine ? machine : new SimpleContainer(MACHINE_SLOT_COUNT);
    }

    private static final class ValidatedSlot extends Slot {
        private ValidatedSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return this.container.canPlaceItem(this.index, stack);
        }
    }

    private static final class OutputSlot extends Slot {
        private OutputSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
