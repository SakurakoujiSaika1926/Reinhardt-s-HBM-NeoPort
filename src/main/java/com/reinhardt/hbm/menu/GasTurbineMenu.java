package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.GasTurbineBlockEntity;
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

public class GasTurbineMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOT_COUNT = GasTurbineBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Container container;
    private final ContainerData data;
    private final BlockPos blockPos;

    public GasTurbineMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, getContainer(playerInventory, buffer.readBlockPos()), new SimpleContainerData(GasTurbineBlockEntity.DATA_COUNT));
    }

    public GasTurbineMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(HbmMenus.GAS_TURBINE.get(), containerId);
        checkContainerSize(container, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, GasTurbineBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;
        this.blockPos = container instanceof GasTurbineBlockEntity turbine ? turbine.getBlockPos().immutable() : BlockPos.ZERO;

        this.addSlot(new ValidatedSlot(container, GasTurbineBlockEntity.BATTERY_SLOT, 8, 109));
        this.addSlot(new ValidatedSlot(container, GasTurbineBlockEntity.ID_SLOT, 36, 17));
        addPlayerInventory(playerInventory, 8, 141);
        addDataSlots(data);
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
        } else if (this.container.canPlaceItem(GasTurbineBlockEntity.BATTERY_SLOT, stack)) {
            if (!moveItemStackTo(stack, GasTurbineBlockEntity.BATTERY_SLOT, GasTurbineBlockEntity.BATTERY_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof FluidIdentifierItem) {
            if (!moveItemStackTo(stack, GasTurbineBlockEntity.ID_SLOT, GasTurbineBlockEntity.ID_SLOT + 1, false)) {
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

    public int power() {
        return this.data.get(0);
    }

    public int rpm() {
        return this.data.get(1);
    }

    public int temp() {
        return this.data.get(2);
    }

    public int state() {
        return this.data.get(3);
    }

    public boolean autoMode() {
        return this.data.get(4) != 0;
    }

    public int throttle() {
        return this.data.get(5);
    }

    public int powerSliderPos() {
        return this.data.get(6);
    }

    public int counterOrOutput() {
        return this.data.get(7);
    }

    public com.reinhardt.hbm.fluid.HbmFluidDefinition fuelFluid() {
        return HbmFluids.byOldId(this.data.get(8)).orElse(HbmFluids.none());
    }

    public int fuelAmount() {
        return this.data.get(9);
    }

    public com.reinhardt.hbm.fluid.HbmFluidDefinition lubricantFluid() {
        return HbmFluids.byOldId(this.data.get(10)).orElse(HbmFluids.none());
    }

    public int lubricantAmount() {
        return this.data.get(11);
    }

    public com.reinhardt.hbm.fluid.HbmFluidDefinition waterFluid() {
        return HbmFluids.byOldId(this.data.get(12)).orElse(HbmFluids.none());
    }

    public int waterAmount() {
        return this.data.get(13);
    }

    public com.reinhardt.hbm.fluid.HbmFluidDefinition steamFluid() {
        return HbmFluids.byOldId(this.data.get(14)).orElse(HbmFluids.none());
    }

    public int steamAmount() {
        return this.data.get(15);
    }

    public int instantPowerOutput() {
        return this.data.get(16);
    }

    public int powerScaled(int pixels) {
        return Math.min(pixels, (int) (this.power() * (long) pixels / GasTurbineBlockEntity.ENERGY_CAPACITY));
    }

    public int fuelScaled(int pixels) {
        return Math.min(pixels, this.fuelAmount() * pixels / GasTurbineBlockEntity.FUEL_CAPACITY);
    }

    public int lubricantScaled(int pixels) {
        return Math.min(pixels, this.lubricantAmount() * pixels / GasTurbineBlockEntity.LUBRICANT_CAPACITY);
    }

    public int waterScaled(int pixels) {
        return Math.min(pixels, this.waterAmount() * pixels / GasTurbineBlockEntity.WATER_CAPACITY);
    }

    public int steamScaled(int pixels) {
        return Math.min(pixels, this.steamAmount() * pixels / GasTurbineBlockEntity.STEAM_CAPACITY);
    }

    public double fuelConsumptionPerSecond() {
        double max = GasTurbineBlockEntity.fuelMaxConsumption(this.fuelFluid());
        return 20.0D * (max * 0.05D + max * (this.powerSliderPos() / 60.0D));
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
        if (blockEntity instanceof GasTurbineBlockEntity turbine) {
            return turbine;
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
}
