package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.GasFlareBlockEntity;
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

public class GasFlareMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOT_COUNT = GasFlareBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Container container;
    private final ContainerData data;

    public GasFlareMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(
                containerId,
                playerInventory,
                getContainer(playerInventory, buffer.readBlockPos()),
                new SimpleContainerData(GasFlareBlockEntity.DATA_COUNT)
        );
    }

    public GasFlareMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(HbmMenus.GAS_FLARE.get(), containerId);
        checkContainerSize(container, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, GasFlareBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;

        addSlot(new ValidatedSlot(container, GasFlareBlockEntity.SLOT_BATTERY, 143, 71));
        addSlot(new ValidatedSlot(container, GasFlareBlockEntity.SLOT_FLUID_INPUT, 17, 17));
        addSlot(new TakeOnlySlot(container, GasFlareBlockEntity.SLOT_FLUID_OUTPUT, 17, 53));
        addSlot(new ValidatedSlot(container, GasFlareBlockEntity.SLOT_IDENTIFIER, 35, 71));
        addSlot(new ValidatedSlot(container, GasFlareBlockEntity.SLOT_SPEED_UPGRADE, 80, 71));
        addSlot(new ValidatedSlot(container, GasFlareBlockEntity.SLOT_EFFECT_UPGRADE, 98, 71));
        addPlayerInventory(playerInventory, 8, 121);
        addDataSlots(data);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(this.container instanceof GasFlareBlockEntity flare) || !stillValid(player)) {
            return false;
        }
        if (id == 0) {
            flare.toggleValve();
            return true;
        }
        if (id == 1) {
            flare.toggleIgnition();
            return true;
        }
        return false;
    }

    public int power() {
        return this.data.get(0);
    }

    public int powerCapacity() {
        return Math.max(1, this.data.get(1));
    }

    public boolean valveOpen() {
        return this.data.get(2) != 0;
    }

    public boolean ignitionEnabled() {
        return this.data.get(3) != 0;
    }

    public boolean active() {
        return this.data.get(4) != 0;
    }

    public HbmFluidDefinition fluid() {
        return HbmFluids.byOldId(this.data.get(5)).orElse(HbmFluids.none());
    }

    public int fluidAmount() {
        return this.data.get(6);
    }

    public int fluidCapacity() {
        return Math.max(1, this.data.get(7));
    }

    public int fluidUsed() {
        return this.data.get(8);
    }

    public int output() {
        return this.data.get(9);
    }

    public int powerScaled(int pixels) {
        return Math.min(pixels, (int) (this.power() * (long) pixels / this.powerCapacity()));
    }

    public int fluidScaled(int pixels) {
        return Math.min(pixels, this.fluidAmount() * pixels / this.fluidCapacity());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= this.slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack moved = stack.copy();

        if (index < MACHINE_SLOT_COUNT) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (this.container.canPlaceItem(GasFlareBlockEntity.SLOT_IDENTIFIER, stack)) {
            if (!moveItemStackTo(stack, GasFlareBlockEntity.SLOT_IDENTIFIER, GasFlareBlockEntity.SLOT_IDENTIFIER + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (this.container.canPlaceItem(GasFlareBlockEntity.SLOT_BATTERY, stack)) {
            if (!moveItemStackTo(stack, GasFlareBlockEntity.SLOT_BATTERY, GasFlareBlockEntity.SLOT_BATTERY + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (this.container.canPlaceItem(GasFlareBlockEntity.SLOT_SPEED_UPGRADE, stack)) {
            if (!moveItemStackTo(stack, GasFlareBlockEntity.SLOT_SPEED_UPGRADE, GasFlareBlockEntity.SLOT_SPEED_UPGRADE + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (this.container.canPlaceItem(GasFlareBlockEntity.SLOT_EFFECT_UPGRADE, stack)) {
            if (!moveItemStackTo(stack, GasFlareBlockEntity.SLOT_EFFECT_UPGRADE, GasFlareBlockEntity.SLOT_EFFECT_UPGRADE + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (this.container.canPlaceItem(GasFlareBlockEntity.SLOT_FLUID_INPUT, stack)) {
            if (!moveItemStackTo(stack, GasFlareBlockEntity.SLOT_FLUID_INPUT, GasFlareBlockEntity.SLOT_FLUID_INPUT + 1, false)) {
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
                addSlot(new Slot(inventory, column + row * 9 + 9, left + column * 18, top + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, left + column * 18, top + 58));
        }
    }

    private static Container getContainer(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        return blockEntity instanceof GasFlareBlockEntity flare ? flare : new SimpleContainer(MACHINE_SLOT_COUNT);
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
