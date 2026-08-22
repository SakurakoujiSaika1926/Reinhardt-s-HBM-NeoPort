package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.ZirnoxReactorBlockEntity;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.item.ZirnoxRodItem;
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

public class ZirnoxReactorMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOT_COUNT = ZirnoxReactorBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Container container;
    private final ContainerData data;
    private final BlockPos blockPos;

    public ZirnoxReactorMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, readContext(playerInventory, buffer));
    }

    private ZirnoxReactorMenu(int containerId, Inventory playerInventory, MenuContext context) {
        this(containerId, playerInventory, context.container(), new SimpleContainerData(ZirnoxReactorBlockEntity.DATA_COUNT), context.blockPos());
    }

    public ZirnoxReactorMenu(int containerId, Inventory playerInventory, Container container, ContainerData data, BlockPos blockPos) {
        super(HbmMenus.ZIRNOX_REACTOR.get(), containerId);
        checkContainerSize(container, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, ZirnoxReactorBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;
        this.blockPos = blockPos.immutable();

        int[][] rodSlots = {
                {26, 16}, {62, 16}, {98, 16}, {8, 34}, {44, 34}, {80, 34}, {116, 34},
                {26, 52}, {62, 52}, {98, 52}, {8, 70}, {44, 70}, {80, 70}, {116, 70},
                {26, 88}, {62, 88}, {98, 88}, {8, 106}, {44, 106}, {80, 106}, {116, 106},
                {26, 124}, {62, 124}, {98, 124}
        };
        for (int slot = 0; slot < rodSlots.length; slot++) {
            this.addSlot(new ValidatedSlot(container, slot, rodSlots[slot][0], rodSlots[slot][1]));
        }

        this.addSlot(new ValidatedSlot(container, ZirnoxReactorBlockEntity.CO2_INPUT_SLOT, 143, 124));
        this.addSlot(new ValidatedSlot(container, ZirnoxReactorBlockEntity.WATER_INPUT_SLOT, 179, 124));
        this.addSlot(new TakeOnlySlot(container, ZirnoxReactorBlockEntity.CO2_OUTPUT_SLOT, 143, 142));
        this.addSlot(new TakeOnlySlot(container, ZirnoxReactorBlockEntity.WATER_OUTPUT_SLOT, 179, 142));
        addPlayerInventory(playerInventory, 8, 174);
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
        } else if (stack.getItem() instanceof ZirnoxRodItem) {
            if (!moveItemStackTo(stack, 0, ZirnoxReactorBlockEntity.ROD_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else if (this.container.canPlaceItem(ZirnoxReactorBlockEntity.CO2_INPUT_SLOT, stack)) {
            if (!moveItemStackTo(stack, ZirnoxReactorBlockEntity.CO2_INPUT_SLOT, ZirnoxReactorBlockEntity.CO2_INPUT_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (this.container.canPlaceItem(ZirnoxReactorBlockEntity.WATER_INPUT_SLOT, stack)) {
            if (!moveItemStackTo(stack, ZirnoxReactorBlockEntity.WATER_INPUT_SLOT, ZirnoxReactorBlockEntity.WATER_INPUT_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (this.container.canPlaceItem(ZirnoxReactorBlockEntity.CO2_OUTPUT_SLOT, stack)) {
            if (!moveItemStackTo(stack, ZirnoxReactorBlockEntity.CO2_OUTPUT_SLOT, ZirnoxReactorBlockEntity.WATER_OUTPUT_SLOT + 1, false)) {
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

    public BlockPos blockPos() {
        return this.blockPos;
    }

    public int heat() {
        return this.data.get(0);
    }

    public int pressure() {
        return this.data.get(1);
    }

    public boolean active() {
        return this.data.get(2) != 0;
    }

    public boolean redstonePowered() {
        return this.data.get(3) != 0;
    }

    public int output() {
        return this.data.get(4);
    }

    public HbmFluidDefinition steamFluid() {
        return HbmFluids.byOldId(this.data.get(5)).orElse(HbmFluids.none());
    }

    public int steamAmount() {
        return this.data.get(6);
    }

    public int steamCapacity() {
        return Math.max(1, this.data.get(7));
    }

    public HbmFluidDefinition carbonDioxideFluid() {
        return HbmFluids.byOldId(this.data.get(8)).orElse(HbmFluids.none());
    }

    public int carbonDioxideAmount() {
        return this.data.get(9);
    }

    public int carbonDioxideCapacity() {
        return Math.max(1, this.data.get(10));
    }

    public HbmFluidDefinition waterFluid() {
        return HbmFluids.byName("water").orElse(HbmFluids.none());
    }

    public int waterAmount() {
        return this.data.get(11);
    }

    public int waterCapacity() {
        return Math.max(1, this.data.get(12));
    }

    public int gaugeScaled(int pixels, int type) {
        return switch (type) {
            case 0 -> Math.min(pixels, this.steamAmount() * pixels / steamCapacity());
            case 1 -> Math.min(pixels, this.carbonDioxideAmount() * pixels / carbonDioxideCapacity());
            case 2 -> Math.min(pixels, this.waterAmount() * pixels / waterCapacity());
            case 3 -> Math.min(pixels, this.heat() * pixels / ZirnoxReactorBlockEntity.MAX_HEAT);
            case 4 -> Math.min(pixels, this.pressure() * pixels / ZirnoxReactorBlockEntity.MAX_PRESSURE);
            default -> 0;
        };
    }

    public int temperatureCelsius() {
        return (int) Math.round(this.heat() * 0.00001D * 780.0D + 20.0D);
    }

    public int pressureBars() {
        return (int) Math.round(this.pressure() * 0.00001D * 30.0D);
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

    private static MenuContext readContext(Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof ZirnoxReactorBlockEntity reactor) {
            return new MenuContext(reactor, pos);
        }
        return new MenuContext(new SimpleContainer(MACHINE_SLOT_COUNT), pos);
    }

    private record MenuContext(Container container, BlockPos blockPos) {
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
