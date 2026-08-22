package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.MiningLaserBlockEntity;
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

public class MiningLaserMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOT_COUNT = MiningLaserBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Container container;
    private final ContainerData data;

    public MiningLaserMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, getContainer(playerInventory, buffer.readBlockPos()), new SimpleContainerData(MiningLaserBlockEntity.DATA_COUNT));
    }

    public MiningLaserMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(HbmMenus.MINING_LASER.get(), containerId);
        checkContainerSize(container, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, MiningLaserBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;

        this.addSlot(new ValidatedSlot(container, MiningLaserBlockEntity.BATTERY_SLOT, 8, 108));
        for (int row = 0; row < 2; row++) {
            for (int column = 0; column < 4; column++) {
                this.addSlot(new ValidatedSlot(container, MiningLaserBlockEntity.UPGRADE_START + row * 4 + column, 98 + column * 18, 18 + row * 18));
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 7; column++) {
                this.addSlot(new TakeOnlySlot(container, MiningLaserBlockEntity.OUTPUT_START + row * 7 + column, 44 + column * 18, 72 + row * 18));
            }
        }
        addPlayerInventory(playerInventory, 8, 140);
        addDataSlots(data);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0 && this.container instanceof MiningLaserBlockEntity laser && stillValid(player)) {
            laser.toggleEnabled();
            return true;
        }
        return false;
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
        } else if (this.container.canPlaceItem(MiningLaserBlockEntity.BATTERY_SLOT, stack)) {
            if (!moveItemStackTo(stack, MiningLaserBlockEntity.BATTERY_SLOT, MiningLaserBlockEntity.BATTERY_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (moveUpgrade(stack)) {
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

    public int power() {
        return this.data.get(0);
    }

    public int maxPower() {
        return (int) MiningLaserBlockEntity.MAX_POWER;
    }

    public boolean enabled() {
        return this.data.get(2) != 0;
    }

    public int progressScaled(int pixels) {
        return Math.min(pixels, this.data.get(4) * pixels / 1000);
    }

    public int range() {
        return this.data.get(5);
    }

    public int width() {
        return 1 + this.range() * 2;
    }

    public int currentConsumption() {
        return this.data.get(6);
    }

    public com.reinhardt.hbm.fluid.HbmFluidDefinition oilFluid() {
        return HbmFluids.byName("oil").orElse(HbmFluids.none());
    }

    public int oilAmount() {
        return this.data.get(7);
    }

    public int oilCapacity() {
        return MiningLaserBlockEntity.OIL_CAPACITY;
    }

    public int completedBlocks() {
        return this.data.get(8);
    }

    public int targetY() {
        return this.data.get(9);
    }

    public int powerScaled(int pixels) {
        return Math.min(pixels, (int) (this.power() * (long) pixels / Math.max(1, this.maxPower())));
    }

    public int oilScaled(int pixels) {
        return Math.min(pixels, this.oilAmount() * pixels / Math.max(1, this.oilCapacity()));
    }

    private boolean moveUpgrade(ItemStack stack) {
        for (int slot = MiningLaserBlockEntity.UPGRADE_START; slot < MiningLaserBlockEntity.UPGRADE_END; slot++) {
            if (!this.container.canPlaceItem(slot, stack)) {
                continue;
            }
            if (moveItemStackTo(stack, slot, slot + 1, false)) {
                return true;
            }
        }
        return false;
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
        if (blockEntity instanceof MiningLaserBlockEntity laser) {
            return laser;
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
