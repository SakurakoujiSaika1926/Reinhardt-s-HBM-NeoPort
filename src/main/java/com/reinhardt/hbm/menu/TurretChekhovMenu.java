package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.TurretChekhovBlockEntity;
import com.reinhardt.hbm.item.BatteryPackItem;
import com.reinhardt.hbm.item.StandardAmmoItem;
import com.reinhardt.hbm.registry.HbmItems;
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

public class TurretChekhovMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOT_COUNT = TurretChekhovBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Container container;
    private final ContainerData data;
    private final BlockPos position;

    public TurretChekhovMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, buffer.readBlockPos());
    }

    private TurretChekhovMenu(int containerId, Inventory playerInventory, BlockPos position) {
        this(containerId, playerInventory, getContainer(playerInventory, position), new SimpleContainerData(TurretChekhovBlockEntity.DATA_COUNT), position);
    }

    public TurretChekhovMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        this(containerId, playerInventory, container, data, container instanceof TurretChekhovBlockEntity turret ? turret.getBlockPos() : BlockPos.ZERO);
    }

    private TurretChekhovMenu(int containerId, Inventory playerInventory, Container container, ContainerData data, BlockPos position) {
        super(HbmMenus.TURRET_CHEKHOV.get(), containerId);
        checkContainerSize(container, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, TurretChekhovBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;
        this.position = position;
        this.container.startOpen(playerInventory.player);

        addSlot(new ValidatedSlot(container, TurretChekhovBlockEntity.CHIP_SLOT, 98, 27));
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                addSlot(new ValidatedSlot(container, TurretChekhovBlockEntity.AMMO_START + row * 3 + column, 80 + column * 18, 63 + row * 18));
            }
        }
        addSlot(new ValidatedSlot(container, TurretChekhovBlockEntity.BATTERY_SLOT, 152, 99));
        addPlayerInventory(playerInventory, 8, 140);
        addDataSlots(data);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (this.container instanceof TurretChekhovBlockEntity turret && stillValid(player) && id >= 0 && id <= 4) {
            turret.toggle(id);
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
        } else if (stack.is(HbmItems.TURRET_CHIP.get())) {
            if (!moveItemStackTo(stack, TurretChekhovBlockEntity.CHIP_SLOT, TurretChekhovBlockEntity.CHIP_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!moveItemStackTo(stack, TurretChekhovBlockEntity.AMMO_START, MACHINE_SLOT_COUNT, false)) {
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

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.container.stopOpen(player);
    }

    public int powerScaled(int pixels) {
        return Math.min(pixels, this.data.get(0) * pixels / (int) TurretChekhovBlockEntity.MAX_POWER);
    }

    public int power() {
        return this.data.get(0);
    }

    public int lastInput() {
        return this.data.get(1);
    }

    public boolean on() {
        return this.data.get(2) != 0;
    }

    public boolean targetPlayers() {
        return this.data.get(3) != 0;
    }

    public boolean targetAnimals() {
        return this.data.get(4) != 0;
    }

    public boolean targetMobs() {
        return this.data.get(5) != 0;
    }

    public boolean targetMachines() {
        return this.data.get(6) != 0;
    }

    public int stattrak() {
        return this.data.get(7);
    }

    public java.util.List<String> whitelist() {
        if (this.container instanceof TurretChekhovBlockEntity turret) {
            return turret.whitelist();
        }
        return java.util.List.of();
    }

    public Container container() {
        return this.container;
    }

    public BlockPos position() {
        return this.position;
    }

    private void addPlayerInventory(Inventory inventory, int left, int top) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, left + column * 18, top + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, left + column * 18, top + 58));
        }
    }

    private static Container getContainer(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof TurretChekhovBlockEntity turret) {
            return turret;
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
