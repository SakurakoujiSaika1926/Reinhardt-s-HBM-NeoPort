package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.IcfPressBlockEntity;
import com.reinhardt.hbm.item.FluidIdentifierItem;
import com.reinhardt.hbm.registry.HbmFluids;
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

public final class IcfPressMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOT_COUNT = IcfPressBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Container container;
    private final ContainerData data;

    public IcfPressMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(
                containerId,
                playerInventory,
                getContainer(playerInventory, buffer.readBlockPos()),
                new SimpleContainerData(IcfPressBlockEntity.DATA_COUNT)
        );
    }

    public IcfPressMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(HbmMenus.ICF_PRESS.get(), containerId);
        checkContainerSize(container, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, IcfPressBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;

        addSlot(new ValidatedSlot(container, IcfPressBlockEntity.EMPTY_PELLET_SLOT, 98, 18));
        addSlot(new TakeOnlySlot(container, IcfPressBlockEntity.FILLED_PELLET_SLOT, 98, 54));
        addSlot(new ValidatedSlot(container, IcfPressBlockEntity.MUON_INPUT_SLOT, 8, 18));
        addSlot(new TakeOnlySlot(container, IcfPressBlockEntity.MUON_OUTPUT_SLOT, 8, 54));
        addSlot(new ValidatedSlot(container, IcfPressBlockEntity.LEFT_SOLID_FUEL_SLOT, 62, 54));
        addSlot(new ValidatedSlot(container, IcfPressBlockEntity.RIGHT_SOLID_FUEL_SLOT, 134, 54));
        addSlot(new ValidatedSlot(container, IcfPressBlockEntity.LEFT_IDENTIFIER_SLOT, 62, 18));
        addSlot(new ValidatedSlot(container, IcfPressBlockEntity.RIGHT_IDENTIFIER_SLOT, 134, 18));

        addPlayerInventory(playerInventory, 8, 97);
        addDataSlots(data);
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
        } else if (stack.is(HbmItems.ICF_PELLET_EMPTY.get())) {
            if (!moveItemStackTo(stack, IcfPressBlockEntity.EMPTY_PELLET_SLOT,
                    IcfPressBlockEntity.EMPTY_PELLET_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof FluidIdentifierItem) {
            if (!moveItemStackTo(stack, IcfPressBlockEntity.LEFT_IDENTIFIER_SLOT,
                    IcfPressBlockEntity.RIGHT_IDENTIFIER_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.is(HbmItems.PARTICLE_MUON.get())) {
            if (!moveItemStackTo(stack, IcfPressBlockEntity.MUON_INPUT_SLOT,
                    IcfPressBlockEntity.MUON_INPUT_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!moveItemStackTo(stack, IcfPressBlockEntity.LEFT_SOLID_FUEL_SLOT,
                    IcfPressBlockEntity.RIGHT_SOLID_FUEL_SLOT + 1, false)) {
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

    public int muon() {
        return this.data.get(0);
    }

    public com.reinhardt.hbm.fluid.HbmFluidDefinition fluid(int tank) {
        return HbmFluids.byOldId(this.data.get(tank == 0 ? 1 : 4)).orElse(HbmFluids.none());
    }

    public int fluidAmount(int tank) {
        return this.data.get(tank == 0 ? 2 : 5);
    }

    public int fluidPressure(int tank) {
        return this.data.get(tank == 0 ? 3 : 6);
    }

    public int fluidScaled(int tank, int pixels) {
        return Math.min(pixels, fluidAmount(tank) * pixels / IcfPressBlockEntity.TANK_CAPACITY);
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
        return blockEntity instanceof IcfPressBlockEntity press ? press : new SimpleContainer(MACHINE_SLOT_COUNT);
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
