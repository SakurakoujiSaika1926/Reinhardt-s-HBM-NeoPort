package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.GasCentrifugeBlockEntity;
import com.reinhardt.hbm.blockentity.ShredderBlockEntity;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.item.FluidIdentifierItem;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
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

public class GasCentrifugeMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOT_COUNT = GasCentrifugeBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Container container;
    private final ContainerData data;

    public GasCentrifugeMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, getContainer(playerInventory, buffer.readBlockPos()), new SimpleContainerData(GasCentrifugeBlockEntity.DATA_COUNT));
    }

    public GasCentrifugeMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(HbmMenus.GAS_CENTRIFUGE.get(), containerId);
        checkContainerSize(container, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, GasCentrifugeBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;

        for (int row = 0; row < 2; row++) {
            for (int column = 0; column < 2; column++) {
                int slot = GasCentrifugeBlockEntity.OUTPUT_START + column + row * 2;
                this.addSlot(new OutputSlot(container, slot, 71 + column * 18, 53 + row * 18));
            }
        }
        this.addSlot(new ValidatedSlot(container, GasCentrifugeBlockEntity.BATTERY_SLOT, 182, 71));
        this.addSlot(new ValidatedSlot(container, GasCentrifugeBlockEntity.FLUID_ID_SLOT, 91, 15));
        this.addSlot(new ValidatedSlot(container, GasCentrifugeBlockEntity.UPGRADE_SLOT, 69, 15));
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
        } else if (ShredderBlockEntity.isBattery(stack)) {
            if (!moveItemStackTo(stack, GasCentrifugeBlockEntity.BATTERY_SLOT, GasCentrifugeBlockEntity.BATTERY_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof FluidIdentifierItem) {
            if (!moveItemStackTo(stack, GasCentrifugeBlockEntity.FLUID_ID_SLOT, GasCentrifugeBlockEntity.FLUID_ID_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (isGasSpeedUpgrade(stack)) {
            if (!moveItemStackTo(stack, GasCentrifugeBlockEntity.UPGRADE_SLOT, GasCentrifugeBlockEntity.UPGRADE_SLOT + 1, false)) {
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

    public int energy() {
        return this.data.get(0);
    }

    public int lastInput() {
        return this.data.get(1);
    }

    public int progress() {
        return this.data.get(2);
    }

    public int processTime() {
        return Math.max(1, this.data.get(3));
    }

    public int consumption() {
        return Math.max(1, this.data.get(4));
    }

    public int completedCycles() {
        return this.data.get(5);
    }

    public HbmFluidDefinition inputFluid() {
        return HbmFluids.byOldId(this.data.get(6)).orElse(HbmFluids.none());
    }

    public int inputAmount() {
        return this.data.get(7);
    }

    public HbmFluidDefinition outputFluid() {
        return HbmFluids.byOldId(this.data.get(8)).orElse(HbmFluids.none());
    }

    public int outputAmount() {
        return this.data.get(9);
    }

    public boolean isWorking() {
        return this.data.get(10) != 0;
    }

    public int energyScaled(int pixels) {
        return Math.min(pixels, (int) (this.energy() * (long) pixels / GasCentrifugeBlockEntity.MAX_POWER));
    }

    public int progressScaled(int pixels) {
        return Math.min(pixels, this.progress() * pixels / this.processTime());
    }

    public int inputScaled(int pixels) {
        return Math.min(pixels, this.inputAmount() * pixels / GasCentrifugeBlockEntity.TANK_CAPACITY);
    }

    public int outputScaled(int pixels) {
        return Math.min(pixels, this.outputAmount() * pixels / GasCentrifugeBlockEntity.TANK_CAPACITY);
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
        if (blockEntity instanceof GasCentrifugeBlockEntity gasCentrifuge) {
            return gasCentrifuge;
        }
        return new SimpleContainer(MACHINE_SLOT_COUNT);
    }

    private static boolean isGasSpeedUpgrade(ItemStack stack) {
        return !stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(ReinhardtsHBM.id("upgrade_gc_speed"));
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

    private static final class OutputSlot extends Slot {
        private OutputSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
