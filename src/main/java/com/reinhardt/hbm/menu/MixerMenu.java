package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.MixerBlockEntity;
import com.reinhardt.hbm.blockentity.ShredderBlockEntity;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.item.FluidIdentifierItem;
import com.reinhardt.hbm.item.MachineUpgradeItem;
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

public class MixerMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOT_COUNT = MixerBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Container container;
    private final ContainerData data;
    private final BlockPos blockPos;

    public MixerMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, buffer.readBlockPos());
    }

    private MixerMenu(int containerId, Inventory playerInventory, BlockPos blockPos) {
        this(containerId, playerInventory, getContainer(playerInventory, blockPos), new SimpleContainerData(MixerBlockEntity.DATA_COUNT), blockPos);
    }

    public MixerMenu(int containerId, Inventory playerInventory, Container container, ContainerData data, BlockPos blockPos) {
        super(HbmMenus.MIXER.get(), containerId);
        checkContainerSize(container, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, MixerBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;
        this.blockPos = blockPos.immutable();

        this.addSlot(new ValidatedSlot(container, MixerBlockEntity.BATTERY_SLOT, 23, 77));
        this.addSlot(new ValidatedSlot(container, MixerBlockEntity.SOLID_INPUT_SLOT, 43, 77));
        this.addSlot(new ValidatedSlot(container, MixerBlockEntity.FLUID_IDENTIFIER_SLOT, 117, 77));
        this.addSlot(new ValidatedSlot(container, MixerBlockEntity.UPGRADE_START, 137, 24));
        this.addSlot(new ValidatedSlot(container, MixerBlockEntity.UPGRADE_START + 1, 137, 42));

        addPlayerInventory(playerInventory, 8, 122);
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
        } else if (ShredderBlockEntity.isBattery(stack)) {
            if (!moveItemStackTo(stack, MixerBlockEntity.BATTERY_SLOT, MixerBlockEntity.BATTERY_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof FluidIdentifierItem) {
            if (!moveItemStackTo(stack, MixerBlockEntity.FLUID_IDENTIFIER_SLOT, MixerBlockEntity.FLUID_IDENTIFIER_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (MachineUpgradeItem.isMachineUpgrade(stack)) {
            if (!moveItemStackTo(stack, MixerBlockEntity.UPGRADE_START, MixerBlockEntity.UPGRADE_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, MixerBlockEntity.SOLID_INPUT_SLOT, MixerBlockEntity.SOLID_INPUT_SLOT + 1, false)) {
            if (index < PLAYER_INVENTORY_END) {
                if (!moveItemStackTo(stack, HOTBAR_START, HOTBAR_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, false)) {
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

    public BlockPos blockPos() {
        return this.blockPos;
    }

    public int energy() {
        return this.data.get(0);
    }

    public int lastInput() {
        return this.data.get(1);
    }

    public int progress() {
        return this.data.get(2);
    }

    public int processTime() {
        return Math.max(1, this.data.get(3));
    }

    public int usage() {
        return Math.max(1, this.data.get(4));
    }

    public int recipeIndex() {
        return this.data.get(5);
    }

    public HbmFluidDefinition inputFluid1() {
        return HbmFluids.byOldId(this.data.get(6)).orElse(HbmFluids.none());
    }

    public int inputAmount1() {
        return this.data.get(7);
    }

    public int inputCapacity1() {
        return Math.max(1, this.data.get(8));
    }

    public HbmFluidDefinition inputFluid2() {
        return HbmFluids.byOldId(this.data.get(9)).orElse(HbmFluids.none());
    }

    public int inputAmount2() {
        return this.data.get(10);
    }

    public int inputCapacity2() {
        return Math.max(1, this.data.get(11));
    }

    public HbmFluidDefinition outputFluid() {
        return HbmFluids.byOldId(this.data.get(12)).orElse(HbmFluids.none());
    }

    public int outputAmount() {
        return this.data.get(13);
    }

    public int outputCapacity() {
        return Math.max(1, this.data.get(14));
    }

    public HbmFluidDefinition configuredOutput() {
        return HbmFluids.byOldId(this.data.get(15)).orElse(HbmFluids.none());
    }

    public int energyScaled(int pixels) {
        return Math.min(pixels, (int) (this.energy() * (long) pixels / MixerBlockEntity.MAX_POWER));
    }

    public int progressScaled(int pixels) {
        return Math.min(pixels, this.progress() * pixels / this.processTime());
    }

    public int input1Scaled(int pixels) {
        return Math.min(pixels, this.inputAmount1() * pixels / this.inputCapacity1());
    }

    public int input2Scaled(int pixels) {
        return Math.min(pixels, this.inputAmount2() * pixels / this.inputCapacity2());
    }

    public int outputScaled(int pixels) {
        return Math.min(pixels, this.outputAmount() * pixels / this.outputCapacity());
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
        if (blockEntity instanceof MixerBlockEntity mixer) {
            return mixer;
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
