package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.CombustionEngineBlockEntity;
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

public class CombustionEngineMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOT_COUNT = CombustionEngineBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Container container;
    private final ContainerData data;
    private final BlockPos blockPos;

    public CombustionEngineMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, getContainer(playerInventory, buffer.readBlockPos()), new SimpleContainerData(CombustionEngineBlockEntity.DATA_COUNT));
    }

    public CombustionEngineMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(HbmMenus.COMBUSTION_ENGINE.get(), containerId);
        checkContainerSize(container, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, CombustionEngineBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;
        this.blockPos = container instanceof CombustionEngineBlockEntity engine ? engine.getBlockPos().immutable() : BlockPos.ZERO;

        this.addSlot(new ValidatedSlot(container, CombustionEngineBlockEntity.SLOT_INPUT, 17, 17));
        this.addSlot(new TakeOnlySlot(container, CombustionEngineBlockEntity.SLOT_OUTPUT, 17, 53));
        this.addSlot(new ValidatedSlot(container, CombustionEngineBlockEntity.SLOT_PISTON, 88, 71));
        this.addSlot(new ValidatedSlot(container, CombustionEngineBlockEntity.SLOT_BATTERY, 143, 71));
        this.addSlot(new ValidatedSlot(container, CombustionEngineBlockEntity.SLOT_IDENTIFIER, 35, 71));
        addPlayerInventory(playerInventory, 8, 121);
        addDataSlots(data);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(this.container instanceof CombustionEngineBlockEntity engine) || !stillValid(player)) {
            return false;
        }
        if (id == 0) {
            engine.toggleEnabled();
            return true;
        }
        if (id >= 1 && id <= CombustionEngineBlockEntity.MAX_THROTTLE + 1) {
            engine.setThrottle(id - 1);
            return true;
        }
        return false;
    }

    public int power() {
        return this.data.get(0);
    }

    public int powerCap() {
        return Math.max(1, this.data.get(1));
    }

    public boolean enabled() {
        return this.data.get(2) != 0;
    }

    public boolean running() {
        return this.data.get(3) != 0;
    }

    public int throttle() {
        return this.data.get(4);
    }

    public com.reinhardt.hbm.fluid.HbmFluidDefinition fuelFluid() {
        return HbmFluids.byOldId(this.data.get(5)).orElse(HbmFluids.none());
    }

    public int fuelAmount() {
        return this.data.get(6);
    }

    public int fuelCapacity() {
        return Math.max(1, this.data.get(7));
    }

    public int hePerTick() {
        return this.data.get(8);
    }

    public int efficiencyPercent() {
        return this.data.get(9);
    }

    public int powerScaled(int pixels) {
        return Math.min(pixels, (int) (this.power() * (long) pixels / this.powerCap()));
    }

    public int fuelScaled(int pixels) {
        return Math.min(pixels, this.fuelAmount() * pixels / this.fuelCapacity());
    }

    public int throttleSliderX() {
        return this.throttle() * 32 / CombustionEngineBlockEntity.MAX_THROTTLE;
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
        } else if (this.container.canPlaceItem(CombustionEngineBlockEntity.SLOT_BATTERY, stack)) {
            if (!moveItemStackTo(stack, CombustionEngineBlockEntity.SLOT_BATTERY, CombustionEngineBlockEntity.SLOT_BATTERY + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (this.container.canPlaceItem(CombustionEngineBlockEntity.SLOT_IDENTIFIER, stack)) {
            if (!moveItemStackTo(stack, CombustionEngineBlockEntity.SLOT_IDENTIFIER, CombustionEngineBlockEntity.SLOT_IDENTIFIER + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (this.container.canPlaceItem(CombustionEngineBlockEntity.SLOT_PISTON, stack)) {
            if (!moveItemStackTo(stack, CombustionEngineBlockEntity.SLOT_PISTON, CombustionEngineBlockEntity.SLOT_PISTON + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (this.container.canPlaceItem(CombustionEngineBlockEntity.SLOT_INPUT, stack)) {
            if (!moveItemStackTo(stack, CombustionEngineBlockEntity.SLOT_INPUT, CombustionEngineBlockEntity.SLOT_INPUT + 1, false)) {
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
                this.addSlot(new Slot(inventory, column + row * 9 + 9, left + column * 18, top + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(inventory, column, left + column * 18, top + 58));
        }
    }

    private static Container getContainer(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof CombustionEngineBlockEntity engine) {
            return engine;
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
