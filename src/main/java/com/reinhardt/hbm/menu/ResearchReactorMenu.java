package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.ResearchReactorBlockEntity;
import com.reinhardt.hbm.item.PlateFuelItem;
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

public class ResearchReactorMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOT_COUNT = ResearchReactorBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Inventory playerInventory;
    private final Container container;
    private final ContainerData data;
    private final BlockPos blockPos;

    public ResearchReactorMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, readContainer(playerInventory, buffer));
    }

    private ResearchReactorMenu(int containerId, Inventory playerInventory, MenuContext context) {
        this(containerId, playerInventory, context.container(), new SimpleContainerData(ResearchReactorBlockEntity.DATA_COUNT), context.blockPos());
    }

    public ResearchReactorMenu(int containerId, Inventory playerInventory, Container container, ContainerData data, BlockPos blockPos) {
        super(HbmMenus.RESEARCH_REACTOR.get(), containerId);
        checkContainerSize(container, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, ResearchReactorBlockEntity.DATA_COUNT);
        this.playerInventory = playerInventory;
        this.container = container;
        this.data = data;
        this.blockPos = blockPos.immutable();

        addFuelSlot(0, 95, 22);
        addFuelSlot(1, 131, 22);
        addFuelSlot(2, 77, 40);
        addFuelSlot(3, 113, 40);
        addFuelSlot(4, 149, 40);
        addFuelSlot(5, 95, 58);
        addFuelSlot(6, 131, 58);
        addFuelSlot(7, 77, 76);
        addFuelSlot(8, 113, 76);
        addFuelSlot(9, 149, 76);
        addFuelSlot(10, 95, 94);
        addFuelSlot(11, 131, 94);
        addPlayerInventory(playerInventory, 8, 140);
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
        } else if (stack.getItem() instanceof PlateFuelItem) {
            if (!moveItemStackTo(stack, 0, MACHINE_SLOT_COUNT, false)) {
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

    public int water() {
        return this.data.get(1);
    }

    public int flux() {
        return this.data.get(2);
    }

    public int controlPercent() {
        return Math.round(this.data.get(3) * 100.0F / ResearchReactorBlockEntity.LEVEL_SCALE);
    }

    public int targetPercent() {
        return Math.round(this.data.get(4) * 100.0F / ResearchReactorBlockEntity.LEVEL_SCALE);
    }

    public int temperature() {
        return this.data.get(5);
    }

    private void addFuelSlot(int slot, int x, int y) {
        this.addSlot(new FuelSlot(this.container, slot, x, y));
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
        if (blockEntity instanceof ResearchReactorBlockEntity reactor) {
            return reactor;
        }
        return new SimpleContainer(MACHINE_SLOT_COUNT);
    }

    private static MenuContext readContainer(Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        return new MenuContext(pos, getContainer(playerInventory, pos));
    }

    private record MenuContext(BlockPos blockPos, Container container) {
    }

    private static final class FuelSlot extends Slot {
        private FuelSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return this.container.canPlaceItem(this.index, stack);
        }
    }
}
