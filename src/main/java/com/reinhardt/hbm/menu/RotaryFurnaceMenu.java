package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.RotaryFurnaceBlockEntity;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.foundry.FoundryMaterial;
import com.reinhardt.hbm.item.FluidIdentifierItem;
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

public class RotaryFurnaceMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOT_COUNT = RotaryFurnaceBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Container container;
    private final ContainerData data;

    public RotaryFurnaceMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, getContainer(playerInventory, buffer.readBlockPos()), new SimpleContainerData(RotaryFurnaceBlockEntity.DATA_COUNT));
    }

    public RotaryFurnaceMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(HbmMenus.ROTARY_FURNACE.get(), containerId);
        checkContainerSize(container, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, RotaryFurnaceBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;

        this.addSlot(new ValidatedSlot(container, RotaryFurnaceBlockEntity.INPUT_A_SLOT, 8, 18));
        this.addSlot(new ValidatedSlot(container, RotaryFurnaceBlockEntity.INPUT_B_SLOT, 26, 18));
        this.addSlot(new ValidatedSlot(container, RotaryFurnaceBlockEntity.INPUT_C_SLOT, 44, 18));
        this.addSlot(new ValidatedSlot(container, RotaryFurnaceBlockEntity.FLUID_IDENTIFIER_SLOT, 8, 54));
        this.addSlot(new ValidatedSlot(container, RotaryFurnaceBlockEntity.FUEL_SLOT, 44, 54));

        addPlayerInventory(playerInventory, 8, 104);
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
        } else if (stack.getItem() instanceof FluidIdentifierItem
                && moveItemStackTo(stack, RotaryFurnaceBlockEntity.FLUID_IDENTIFIER_SLOT,
                RotaryFurnaceBlockEntity.FLUID_IDENTIFIER_SLOT + 1, false)) {
            return moved;
        } else if (this.container.canPlaceItem(RotaryFurnaceBlockEntity.FUEL_SLOT, stack)
                && moveItemStackTo(stack, RotaryFurnaceBlockEntity.FUEL_SLOT, RotaryFurnaceBlockEntity.FUEL_SLOT + 1, false)) {
            return moved;
        } else if (moveItemStackTo(stack, RotaryFurnaceBlockEntity.INPUT_A_SLOT, RotaryFurnaceBlockEntity.INPUT_C_SLOT + 1, false)) {
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

    public HbmFluidDefinition additiveFluid() {
        return HbmFluids.byOldId(this.data.get(0)).orElse(HbmFluids.none());
    }

    public int additiveAmount() {
        return this.data.get(1);
    }

    public int steamAmount() {
        return this.data.get(2);
    }

    public int spentSteamAmount() {
        return this.data.get(3);
    }

    public int progress() {
        return this.data.get(4);
    }

    public int burnTime() {
        return this.data.get(5);
    }

    public int maxBurnTime() {
        return Math.max(1, this.data.get(6));
    }

    public FoundryMaterial outputMaterial() {
        return FoundryMaterial.byId(this.data.get(7)).orElse(null);
    }

    public int outputAmount() {
        return this.data.get(8);
    }

    public int additiveCapacity() {
        return Math.max(1, this.data.get(9));
    }

    public int steamCapacity() {
        return Math.max(1, this.data.get(10));
    }

    public int spentSteamCapacity() {
        return Math.max(1, this.data.get(11));
    }

    public boolean working() {
        return this.data.get(12) != 0;
    }

    public int additiveScaled(int pixels) {
        return Math.min(pixels, this.additiveAmount() * pixels / additiveCapacity());
    }

    public int steamScaled(int pixels) {
        return Math.min(pixels, this.steamAmount() * pixels / steamCapacity());
    }

    public int spentSteamScaled(int pixels) {
        return Math.min(pixels, this.spentSteamAmount() * pixels / spentSteamCapacity());
    }

    public int burnScaled(int pixels) {
        return Math.min(pixels, this.burnTime() * pixels / maxBurnTime());
    }

    public int progressScaled(int pixels) {
        return Math.min(pixels, this.progress() * pixels / 10_000);
    }

    public int outputScaled(int pixels) {
        return Math.min(pixels, this.outputAmount() * pixels / RotaryFurnaceBlockEntity.MAX_OUTPUT);
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
        if (blockEntity instanceof RotaryFurnaceBlockEntity furnace) {
            return furnace;
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
}
