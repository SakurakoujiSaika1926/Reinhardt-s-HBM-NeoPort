package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.DfcReceiverBlockEntity;
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

public class DfcReceiverMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerData data;
    private final BlockPos blockPos;

    public DfcReceiverMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, getContainer(playerInventory, buffer.readBlockPos()), new SimpleContainerData(DfcReceiverBlockEntity.DATA_COUNT));
    }

    public DfcReceiverMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(HbmMenus.DFC_RECEIVER.get(), containerId);
        checkContainerSize(container, 0);
        checkContainerDataCount(data, DfcReceiverBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;
        this.blockPos = container instanceof DfcReceiverBlockEntity receiver ? receiver.getBlockPos() : BlockPos.ZERO;
        addPlayerInventory(playerInventory, 8, 84);
        addDataSlots(data);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    public BlockPos blockPos() {
        return this.blockPos;
    }

    public HbmFluidDefinition fluid() {
        return HbmFluids.byOldId(this.data.get(0)).orElse(HbmFluids.none());
    }

    public int fluidAmount() {
        return this.data.get(1);
    }

    public int fluidCapacity() {
        return this.data.get(2);
    }

    public int fluidScaled(int pixels) {
        return fluidCapacity() <= 0 ? 0 : fluidAmount() * pixels / fluidCapacity();
    }

    public long joules() {
        return Integer.toUnsignedLong(this.data.get(3));
    }

    public long power() {
        return Integer.toUnsignedLong(this.data.get(4));
    }

    public long lastOutput() {
        return Integer.toUnsignedLong(this.data.get(5));
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
        return blockEntity instanceof DfcReceiverBlockEntity receiver ? receiver : new SimpleContainer(0);
    }
}
