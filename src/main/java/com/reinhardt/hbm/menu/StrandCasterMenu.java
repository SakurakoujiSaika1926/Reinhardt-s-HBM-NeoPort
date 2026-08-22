package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.StrandCasterBlockEntity;
import com.reinhardt.hbm.foundry.FoundryMaterial;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.item.FoundryMoldItem;
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

public class StrandCasterMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOT_COUNT = StrandCasterBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Container container;
    private final ContainerData data;

    public StrandCasterMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, getContainer(playerInventory, buffer.readBlockPos()), new SimpleContainerData(StrandCasterBlockEntity.DATA_COUNT));
    }

    public StrandCasterMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(HbmMenus.STRAND_CASTER.get(), containerId);
        checkContainerSize(container, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, StrandCasterBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;

        this.addSlot(new ValidatedSlot(container, StrandCasterBlockEntity.MOLD_SLOT, 57, 62));
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 2; column++) {
                this.addSlot(new OutputSlot(container, column + row * 2 + StrandCasterBlockEntity.OUTPUT_START, 125 + column * 18, 26 + row * 18));
            }
        }

        addPlayerInventory(playerInventory, 8, 132);
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
            slot.onTake(player, stack);
        } else if (stack.getItem() instanceof FoundryMoldItem) {
            if (!moveItemStackTo(stack, StrandCasterBlockEntity.MOLD_SLOT, StrandCasterBlockEntity.MOLD_SLOT + 1, false)) {
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

    public FoundryMaterial material() {
        return FoundryMaterial.byId(this.data.get(0)).orElse(null);
    }

    public int amount() {
        return this.data.get(1);
    }

    public int capacity() {
        return Math.max(1, this.data.get(2));
    }

    public HbmFluidDefinition waterFluid() {
        return HbmFluids.byOldId(this.data.get(3)).orElse(HbmFluids.none());
    }

    public int waterAmount() {
        return this.data.get(4);
    }

    public int waterCapacity() {
        return Math.max(1, this.data.get(5));
    }

    public HbmFluidDefinition steamFluid() {
        return HbmFluids.byOldId(this.data.get(6)).orElse(HbmFluids.none());
    }

    public int steamAmount() {
        return this.data.get(7);
    }

    public int steamCapacity() {
        return Math.max(1, this.data.get(8));
    }

    public int materialScaled(int pixels) {
        return Math.min(pixels, this.amount() * pixels / this.capacity());
    }

    public int waterScaled(int pixels) {
        return Math.min(pixels, this.waterAmount() * pixels / this.waterCapacity());
    }

    public int steamScaled(int pixels) {
        return Math.min(pixels, this.steamAmount() * pixels / this.steamCapacity());
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
        if (blockEntity instanceof StrandCasterBlockEntity caster) {
            return caster;
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

    private static final class OutputSlot extends Slot {
        private OutputSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
