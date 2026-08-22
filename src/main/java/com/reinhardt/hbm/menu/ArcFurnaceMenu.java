package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.ArcFurnaceBlockEntity;
import com.reinhardt.hbm.item.ArcElectrodeItem;
import com.reinhardt.hbm.item.BatteryPackItem;
import com.reinhardt.hbm.item.MachineUpgradeItem;
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

public final class ArcFurnaceMenu extends AbstractContainerMenu {
    private static final int PLAYER_INVENTORY_START = ArcFurnaceBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Container container;
    private final ContainerData data;

    public ArcFurnaceMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, getContainer(playerInventory, buffer.readBlockPos()),
                new SimpleContainerData(ArcFurnaceBlockEntity.DATA_COUNT));
    }

    public ArcFurnaceMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(HbmMenus.ARC_FURNACE.get(), containerId);
        checkContainerSize(container, ArcFurnaceBlockEntity.SLOT_COUNT);
        checkContainerDataCount(data, ArcFurnaceBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;

        for (int index = 0; index < 3; index++) {
            addSlot(new ValidatedSlot(container, index, 62 + index * 18, 22));
        }
        addSlot(new ValidatedSlot(container, ArcFurnaceBlockEntity.BATTERY_SLOT, 8, 108));
        addSlot(new ValidatedSlot(container, ArcFurnaceBlockEntity.UPGRADE_SLOT, 152, 108));

        for (int row = 0; row < 4; row++) {
            for (int column = 0; column < 5; column++) {
                int slot = ArcFurnaceBlockEntity.INPUT_START + column + row * 5;
                addSlot(new ArcInputSlot(container, slot, 44 + column * 18, 54 + row * 18));
            }
        }
        for (int index = 0; index < 5; index++) {
            addSlot(new ValidatedSlot(container, ArcFurnaceBlockEntity.QUEUE_START + index, 44 + index * 18, 129));
        }
        addPlayerInventory(playerInventory);
        addDataSlots(data);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != 0 || !(container instanceof ArcFurnaceBlockEntity furnace) || !stillValid(player)) {
            return false;
        }
        furnace.toggleLiquidMode();
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack moved = stack.copy();
        if (index < PLAYER_INVENTORY_START) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) return ItemStack.EMPTY;
        } else if (stack.getItem() instanceof ArcElectrodeItem) {
            if (!moveItemStackTo(stack, 0, 3, false)) return ItemStack.EMPTY;
        } else if (BatteryPackItem.isBattery(stack)) {
            if (!moveItemStackTo(stack, ArcFurnaceBlockEntity.BATTERY_SLOT, ArcFurnaceBlockEntity.BATTERY_SLOT + 1, false)) return ItemStack.EMPTY;
        } else if (MachineUpgradeItem.isMachineUpgrade(stack)
                && MachineUpgradeItem.upgradeType(stack) == MachineUpgradeItem.UpgradeType.SPEED) {
            if (!moveItemStackTo(stack, ArcFurnaceBlockEntity.UPGRADE_SLOT, ArcFurnaceBlockEntity.UPGRADE_SLOT + 1, false)) return ItemStack.EMPTY;
        } else if (container.canPlaceItem(ArcFurnaceBlockEntity.QUEUE_START, stack)) {
            if (!moveItemStackTo(stack, ArcFurnaceBlockEntity.QUEUE_START, ArcFurnaceBlockEntity.QUEUE_END, false)) return ItemStack.EMPTY;
        } else if (index < PLAYER_INVENTORY_END) {
            if (!moveItemStackTo(stack, HOTBAR_START, HOTBAR_END, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return moved;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    public int power() { return data.get(0); }
    public int progress() { return data.get(1); }
    public int processTime() { return Math.max(1, data.get(2)); }
    public boolean liquidMode() { return data.get(4) != 0; }
    public boolean working() { return data.get(5) != 0; }
    public int upgrade() { return data.get(7); }
    public int liquidAmount() { return data.get(8); }
    public int liquidCapacity() { return Math.max(1, data.get(9)); }
    public int powerScaled(int pixels) { return Math.min(pixels, power() * pixels / (int) ArcFurnaceBlockEntity.MAX_POWER); }
    public int progressScaled(int pixels) { return Math.min(pixels, progress() * pixels / processTime()); }
    public int liquidScaled(int pixels) { return Math.min(pixels, liquidAmount() * pixels / liquidCapacity()); }
    public int liquidMaterialId(int index) { return data.get(11 + index * 2); }
    public int liquidMaterialAmount(int index) { return data.get(12 + index * 2); }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 174 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 232));
        }
    }

    private static Container getContainer(Inventory inventory, BlockPos pos) {
        BlockEntity entity = inventory.player.level().getBlockEntity(pos);
        return entity instanceof ArcFurnaceBlockEntity furnace ? furnace : new SimpleContainer(ArcFurnaceBlockEntity.SLOT_COUNT);
    }

    private static final class ValidatedSlot extends Slot {
        private ValidatedSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return container.canPlaceItem(index, stack);
        }
    }

    private static final class ArcInputSlot extends Slot {
        private ArcInputSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return container.canPlaceItem(index, stack);
        }

        @Override
        public int getMaxStackSize() {
            return container instanceof ArcFurnaceBlockEntity furnace
                    ? furnace.maxInputSize(getItem())
                    : 1;
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return container instanceof ArcFurnaceBlockEntity furnace
                    ? furnace.maxInputSize(stack)
                    : 1;
        }
    }
}
