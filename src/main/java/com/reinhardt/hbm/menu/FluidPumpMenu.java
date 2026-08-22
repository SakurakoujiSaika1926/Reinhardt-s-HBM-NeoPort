package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.FluidPumpBlockEntity;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class FluidPumpMenu extends AbstractContainerMenu {
    private final FluidPumpBlockEntity pump;
    private final ContainerData data;

    public FluidPumpMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, getPump(playerInventory, buffer.readBlockPos()), new SimpleContainerData(FluidPumpBlockEntity.DATA_COUNT));
    }

    public FluidPumpMenu(int containerId, Inventory playerInventory, FluidPumpBlockEntity pump, ContainerData data) {
        super(HbmMenus.FLUID_PUMP.get(), containerId);
        checkContainerDataCount(data, FluidPumpBlockEntity.DATA_COUNT);
        this.pump = pump;
        this.data = data;
        addDataSlots(data);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (this.pump == null || !stillValid(player)) {
            return false;
        }
        switch (id) {
            case 0 -> this.pump.cyclePressure();
            case 1 -> this.pump.cyclePriority();
            case 2 -> this.pump.adjustBufferSize(-100);
            case 3 -> this.pump.adjustBufferSize(100);
            case 4 -> this.pump.adjustBufferSize(-1000);
            case 5 -> this.pump.adjustBufferSize(1000);
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        if (pump == null || pump.getLevel() == null) {
            return false;
        }
        return player.level().getBlockEntity(pump.getBlockPos()) == pump
                && player.distanceToSqr(
                pump.getBlockPos().getX() + 0.5D,
                pump.getBlockPos().getY() + 0.5D,
                pump.getBlockPos().getZ() + 0.5D
        ) <= 64.0D;
    }

    public HbmFluidDefinition fluid() {
        return HbmFluids.byOldId(this.data.get(0)).orElse(HbmFluids.none());
    }

    public int amount() {
        return this.data.get(1);
    }

    public int bufferSize() {
        return this.data.get(2);
    }

    public int pressure() {
        return this.data.get(3);
    }

    public FluidPumpBlockEntity.Priority priority() {
        return FluidPumpBlockEntity.Priority.byOrdinal(this.data.get(4));
    }

    public boolean powered() {
        return this.data.get(5) != 0;
    }

    private static FluidPumpBlockEntity getPump(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        return blockEntity instanceof FluidPumpBlockEntity pump ? pump : null;
    }
}
