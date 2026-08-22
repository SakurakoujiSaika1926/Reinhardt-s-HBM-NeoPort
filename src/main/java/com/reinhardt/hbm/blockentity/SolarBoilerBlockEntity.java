package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidNetworks;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SolarBoilerBlockEntity extends BlockEntity {
    // TileEntitySolarBoiler in 1.7.10 stores only one bucket tenth of water
    // and ten buckets of steam. Mirror heat is an unbuffered tick value.
    public static final int INPUT_CAPACITY = 100;
    public static final int OUTPUT_CAPACITY = 10_000;

    private final HbmFluidTank inputTank = new HbmFluidTank(water(), INPUT_CAPACITY);
    private final HbmFluidTank outputTank = new HbmFluidTank(steam(), OUTPUT_CAPACITY);
    private int heat;
    private int heatDisplay;

    public SolarBoilerBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.SOLAR_BOILER.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SolarBoilerBlockEntity boiler) {
        if (level.isClientSide) {
            return;
        }
        boiler.tickServer(level);
    }

    public HbmFluidTank inputTank() {
        return this.inputTank;
    }

    public HbmFluidTank outputTank() {
        return this.outputTank;
    }

    public int heat() {
        return this.heatDisplay;
    }

    public int heatInput() {
        return 0;
    }

    public void addHeatInput(int amount) {
        if (amount > 0) {
            this.heat += amount;
        }
    }

    @Nullable
    public IFluidHandler fluidHandler(BlockPos queriedPos, @Nullable Direction side) {
        if (!allowsPort(queriedPos, side)) {
            return null;
        }
        return new SolarBoilerFluidHandler(queriedPos.immutable(), side);
    }

    @Nullable
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return fluidHandler(this.worldPosition, side);
    }

    public List<Port> ports(LevelAccessor level) {
        return portsFor(this.worldPosition);
    }

    public static List<Port> portsFor(BlockPos corePos) {
        return List.of(
                new Port(corePos, Direction.DOWN),
                new Port(corePos.above(2), Direction.UP)
        );
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("InputTank", this.inputTank.save());
        tag.put("OutputTank", this.outputTank.save());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.inputTank.load(tag.getCompound("InputTank"));
        this.outputTank.load(tag.getCompound("OutputTank"));
        // Legacy heat is transient and deliberately not persisted.
        this.heat = 0;
        this.heatDisplay = 0;
        ensureTankTypes();
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        tag.putInt("HeatDisplay", this.heatDisplay);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void tickServer(Level level) {
        ensureTankTypes();
        pullInput(level);
        tryConvert();
        pushOutput(level);

        setChanged();
        if (level.getGameTime() % 10L == 0L) {
            sync();
        }
    }

    private void tryConvert() {
        int process = this.heat / 50;
        this.heatDisplay = process;
        process = Math.min(process, this.inputTank.amount());
        process = Math.min(process, (this.outputTank.capacity() - this.outputTank.amount()) / 100);
        if (process > 0) {
            this.inputTank.drain(water(), process, false);
            this.outputTank.fill(steam(), process * 100, false);
        }
        this.heat = 0;
        ensureTankTypes();
    }

    private void pullInput(Level level) {
        int space = this.inputTank.capacity() - this.inputTank.amount();
        if (space <= 0) {
            return;
        }
        for (Port port : ports(level)) {
            space = this.inputTank.capacity() - this.inputTank.amount();
            if (space <= 0) {
                break;
            }
            FluidStack drained = HbmFluidNetworks.drainFrom(
                    level,
                    port.connectorPos(),
                    port.face().getOpposite(),
                    water(),
                    space,
                    this.worldPosition,
                    true
            );
            if (!drained.isEmpty()) {
                this.inputTank.fill(water(), drained.getAmount(), false);
            }
        }
    }

    private void pushOutput(Level level) {
        if (this.outputTank.amount() <= 0 || this.outputTank.type().isNone()) {
            return;
        }
        for (Port port : ports(level)) {
            if (this.outputTank.amount() <= 0) {
                break;
            }
            FluidStack stack = HbmFluids.toNeoStack(steam(), this.outputTank.amount());
            int accepted = HbmFluidNetworks.fillInto(
                    level,
                    port.connectorPos(),
                    port.face().getOpposite(),
                    stack,
                    this.worldPosition,
                    true
            );
            if (accepted > 0) {
                this.outputTank.drain(steam(), accepted, false);
            }
        }
    }

    private void ensureTankTypes() {
        if (this.inputTank.amount() == 0 && this.inputTank.type() != water()) {
            this.inputTank.setType(water());
        }
        if (this.outputTank.amount() == 0 && this.outputTank.type() != steam()) {
            this.outputTank.setType(steam());
        }
    }

    private boolean allowsPort(BlockPos queriedPos, @Nullable Direction side) {
        if (this.level == null) {
            return false;
        }
        for (Port port : ports(this.level)) {
            if (port.pos().equals(queriedPos) && (side == null || side == port.face())) {
                return true;
            }
        }
        return false;
    }

    private void sync() {
        setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.invalidateCapabilities(this.worldPosition);
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
            for (Port port : ports(this.level)) {
                this.level.invalidateCapabilities(port.pos());
            }
        }
    }

    private static HbmFluidDefinition water() {
        return HbmFluids.byName("water").orElse(HbmFluids.none());
    }

    private static HbmFluidDefinition steam() {
        return HbmFluids.byName("steam").orElse(HbmFluids.none());
    }

    public record Port(BlockPos pos, Direction face) {
        public BlockPos connectorPos() {
            return this.pos.relative(this.face);
        }
    }

    private final class SolarBoilerFluidHandler implements IFluidHandler {
        private final BlockPos queriedPos;
        @Nullable
        private final Direction side;

        private SolarBoilerFluidHandler(BlockPos queriedPos, @Nullable Direction side) {
            this.queriedPos = queriedPos;
            this.side = side;
        }

        @Override
        public int getTanks() {
            return 2;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            if (tank == 0) {
                return inputTank.getFluidInTank(0);
            }
            if (tank == 1) {
                return outputTank.getFluidInTank(0);
            }
            return FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return switch (tank) {
                case 0 -> inputTank.capacity();
                case 1 -> outputTank.capacity();
                default -> 0;
            };
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            if (tank != 0 || stack.isEmpty() || !allowsPort(this.queriedPos, this.side)) {
                return false;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(stack.getFluid()).orElse(HbmFluids.none());
            return fluid == water();
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !allowsPort(this.queriedPos, this.side)) {
                return 0;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            if (fluid != water()) {
                return 0;
            }
            int filled = inputTank.fill(water(), resource.getAmount(), action.simulate());
            if (filled > 0 && action.execute()) {
                sync();
            }
            return filled;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !allowsPort(this.queriedPos, this.side)) {
                return FluidStack.EMPTY;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            if (fluid != steam()) {
                return FluidStack.EMPTY;
            }
            return drain(resource.getAmount(), action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (maxDrain <= 0 || !allowsPort(this.queriedPos, this.side)) {
                return FluidStack.EMPTY;
            }
            FluidStack drained = outputTank.drain(maxDrain, action);
            if (!drained.isEmpty() && action.execute()) {
                sync();
            }
            return drained;
        }
    }
}
