package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.FluidPumpBlock;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidNetworks;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.menu.FluidPumpMenu;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.util.FluidCopiable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

public class FluidPumpBlockEntity extends BlockEntity implements MenuProvider, FluidCopiable {
    public static final int DATA_COUNT = 6;
    public static final int MAX_BUFFER_SIZE = 10_000;

    private final HbmFluidTank tank = new HbmFluidTank(100);
    private int bufferSize = 100;
    private Priority priority = Priority.NORMAL;
    private boolean powered;
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> tank.type().oldId();
                case 1 -> tank.amount();
                case 2 -> bufferSize;
                case 3 -> tank.pressure();
                case 4 -> priority.ordinal();
                case 5 -> powered ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> setType(HbmFluids.byOldId(value).orElse(HbmFluids.none()));
                case 2 -> setBufferSize(value);
                case 3 -> tank.setPressure(value);
                case 4 -> priority = Priority.byOrdinal(value);
                case 5 -> powered = value != 0;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public FluidPumpBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.FLUID_PUMP.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FluidPumpBlockEntity pump) {
        pump.tickServer(level, state);
    }

    public HbmFluidTank tank() {
        return tank;
    }

    public ContainerData menuData() {
        return menuData;
    }

    public int bufferSize() {
        return bufferSize;
    }

    public Priority priority() {
        return priority;
    }

    public boolean powered() {
        return powered;
    }

    public void setType(HbmFluidDefinition type) {
        tank.setType(type);
        sync();
    }

    @Override
    public int[] getFluidIdsToCopy() {
        return new int[]{tank.type().oldId()};
    }

    @Override
    public void pasteFluidSetting(HbmFluidDefinition fluid, Level level, Player player, BlockPos pos) {
        setType(fluid);
    }

    public void cyclePressure() {
        tank.setPressure((tank.pressure() + 1) % 6);
        sync();
    }

    public void cyclePriority() {
        priority = Priority.byOrdinal(priority.ordinal() + 1);
        sync();
    }

    public void adjustBufferSize(int delta) {
        setBufferSize(bufferSize + delta);
    }

    private void setBufferSize(int value) {
        bufferSize = Math.max(0, Math.min(MAX_BUFFER_SIZE, value));
        tank.setCapacity(Math.max(tank.amount(), bufferSize));
        sync();
    }

    private void tickServer(Level level, BlockState state) {
        tank.setCapacity(Math.max(tank.amount(), bufferSize));
        boolean nextPowered = level.hasNeighborSignal(this.worldPosition);
        if (this.powered != nextPowered) {
            this.powered = nextPowered;
            sync();
        }

        if (tank.type().isNone()) {
            return;
        }

        Direction input = inputSide(state);
        Direction output = input.getOpposite();
        if (tank.amount() < bufferSize) {
            FluidStack drained = HbmFluidNetworks.drainFrom(
                    level,
                    this.worldPosition.relative(input),
                    input.getOpposite(),
                    tank.type(),
                    bufferSize - tank.amount(),
                    this.worldPosition,
                    true
            );
            if (!drained.isEmpty()) {
                tank.fill(tank.type(), drained.getAmount(), false);
                sync();
            }
        }

        if (!powered && tank.amount() > 0) {
            FluidStack offered = HbmFluids.toNeoStack(tank.type(), Math.min(tank.amount(), bufferSize));
            int accepted = HbmFluidNetworks.fillInto(
                    level,
                    this.worldPosition.relative(output),
                    output.getOpposite(),
                    offered,
                    this.worldPosition,
                    true
            );
            if (accepted > 0) {
                tank.drain(tank.type(), accepted, false);
                sync();
            }
        }
    }

    private Direction inputSide(BlockState state) {
        Direction facing = state.hasProperty(FluidPumpBlock.FACING) ? state.getValue(FluidPumpBlock.FACING) : Direction.NORTH;
        return facing.getClockWise();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.reinhardtshbm.fluid_pump");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new FluidPumpMenu(containerId, playerInventory, this, this.menuData);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Tank", tank.save());
        tag.putInt("BufferSize", bufferSize);
        tag.putByte("Priority", (byte) priority.ordinal());
        tag.putBoolean("Powered", powered);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tank.load(tag.getCompound("Tank"));
        bufferSize = Math.max(0, Math.min(MAX_BUFFER_SIZE, tag.getInt("BufferSize")));
        if (bufferSize == 0 && !tag.contains("BufferSize")) {
            bufferSize = 100;
        }
        tank.setCapacity(Math.max(tank.amount(), bufferSize));
        priority = Priority.byOrdinal(tag.getByte("Priority"));
        powered = tag.getBoolean("Powered");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void sync() {
        setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public enum Priority {
        LOWEST,
        LOW,
        NORMAL,
        HIGH,
        HIGHEST;

        public static Priority byOrdinal(int ordinal) {
            Priority[] values = values();
            int index = Math.floorMod(ordinal, values.length);
            return values[index];
        }
    }
}
