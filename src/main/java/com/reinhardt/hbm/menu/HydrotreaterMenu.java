package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.HydrotreaterBlockEntity;
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

public final class HydrotreaterMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOT_COUNT = HydrotreaterBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Container container;
    private final ContainerData data;

    public HydrotreaterMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, getContainer(inventory, buffer.readBlockPos()),
                new SimpleContainerData(HydrotreaterBlockEntity.DATA_COUNT));
    }

    public HydrotreaterMenu(int id, Inventory inventory, Container container, ContainerData data) {
        super(HbmMenus.HYDROTREATER.get(), id);
        checkContainerSize(container, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, HydrotreaterBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;

        addSlot(new ValidatedSlot(container, 0, 17, 90));
        addSlot(new ValidatedSlot(container, 1, 35, 90));
        addSlot(new TakeOnlySlot(container, 2, 35, 108));
        addSlot(new DisabledSlot(container, 3, 53, 90));
        addSlot(new DisabledSlot(container, 4, 53, 108));
        addSlot(new ValidatedSlot(container, 5, 125, 90));
        addSlot(new TakeOnlySlot(container, 6, 125, 108));
        addSlot(new ValidatedSlot(container, 7, 143, 90));
        addSlot(new TakeOnlySlot(container, 8, 143, 108));
        addSlot(new ValidatedSlot(container, 9, 17, 108));
        addSlot(new ValidatedSlot(container, 10, 89, 36));

        addPlayerInventory(inventory, 8, 156);
        addDataSlots(data);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return moved;
        }
        ItemStack stack = slot.getItem();
        moved = stack.copy();
        if (index < MACHINE_SLOT_COUNT) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveIntoFirstValidMachineSlot(stack)) {
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
        return container.stillValid(player);
    }

    public HbmFluidDefinition tankFluid(int index) {
        return HbmFluids.byOldId(data.get(index * 3)).orElse(HbmFluids.none());
    }

    public int tankAmount(int index) {
        return Math.max(0, data.get(index * 3 + 1));
    }

    public int tankCapacity(int index) {
        return Math.max(1, data.get(index * 3 + 2));
    }

    public int tankScaled(int index, int pixels) {
        return Math.min(pixels, tankAmount(index) * pixels / tankCapacity(index));
    }

    public int power() {
        return Math.max(0, data.get(12));
    }

    public int maxPower() {
        return Math.max(1, data.get(14));
    }

    public int powerScaled(int pixels) {
        return Math.min(pixels, (int) (power() * (long) pixels / maxPower()));
    }

    private boolean moveIntoFirstValidMachineSlot(ItemStack stack) {
        for (int slot = 0; slot < MACHINE_SLOT_COUNT; slot++) {
            if (container.canPlaceItem(slot, stack) && moveItemStackTo(stack, slot, slot + 1, false)) {
                return true;
            }
        }
        return false;
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

    private static Container getContainer(Inventory inventory, BlockPos pos) {
        BlockEntity entity = inventory.player.level().getBlockEntity(pos);
        return entity instanceof HydrotreaterBlockEntity hydrotreater
                ? hydrotreater : new SimpleContainer(MACHINE_SLOT_COUNT);
    }

    private static class ValidatedSlot extends Slot {
        private ValidatedSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return container.canPlaceItem(index, stack);
        }
    }

    private static class TakeOnlySlot extends ValidatedSlot {
        private TakeOnlySlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }

    private static final class DisabledSlot extends TakeOnlySlot {
        private DisabledSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }
    }
}
