package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidNetworks;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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

import java.util.ArrayList;
import java.util.List;

public class AirCompressorBlockEntity extends BlockEntity implements PowerEndpoint {
    public static final int AIR_CAPACITY = 1_000;
    public static final long MAX_POWER = 2_000L;
    public static final long POWER_PER_TICK = MAX_POWER / 20L;
    private static final int PUSH_PER_PORT = 1_000;

    private final HbmFluidTank airTank = new HbmFluidTank(air(), AIR_CAPACITY);
    private long power;
    private long lastInput;
    private boolean running;
    private float fan;
    private float lastFan;

    public AirCompressorBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.AIR_COMPRESSOR.get(), pos, blockState);
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos pos, BlockState state, T blockEntity) {
        if (blockEntity instanceof AirCompressorBlockEntity compressor) {
            if (level.isClientSide) {
                compressor.tickClient();
            } else {
                compressor.tickServer(level);
            }
        }
    }

    public HbmFluidTank airTank() {
        return this.airTank;
    }

    public long power() {
        return this.power;
    }

    public long lastInput() {
        return this.lastInput;
    }

    public boolean running() {
        return this.running;
    }

    public float fan(float partialTick) {
        return this.lastFan + (this.fan - this.lastFan) * partialTick;
    }

    @Nullable
    public IFluidHandler fluidHandler(BlockPos queriedPos, @Nullable Direction side) {
        if (side == null || !isAirPort(queriedPos, side)) {
            return null;
        }
        return new AirOutputHandler();
    }

    @Override
    public BlockPos getPowerPos() {
        return this.worldPosition;
    }

    @Override
    public List<BlockPos> getPowerConnectorPositions(LevelAccessor level) {
        ArrayList<BlockPos> connectors = new ArrayList<>();
        for (Port port : ports()) {
            connectors.add(port.pos().relative(port.face()).immutable());
        }
        return List.copyOf(connectors);
    }

    @Override
    public boolean canConnectPower(LevelAccessor level, BlockPos connectorPos, Direction machineSide) {
        return machineSide.getAxis().isHorizontal() && getPowerConnectorPositions(level).contains(connectorPos);
    }

    @Override
    public long getAvailableOutput() {
        return 0L;
    }

    @Override
    public long getRequestedInput() {
        return Math.max(0L, MAX_POWER - this.power);
    }

    @Override
    public void applyPower(long usedOutput, long receivedInput) {
        this.power = Math.min(MAX_POWER, this.power + receivedInput);
        this.lastInput = receivedInput;
        setChanged();
    }

    @Override
    public Component getPowerStatus() {
        return Component.literal(String.format("%,d / %,d HE", this.power, MAX_POWER));
    }

    public List<Port> ports() {
        Direction facing = this.getBlockState().hasProperty(LargeMachineBlock.FACING)
                ? this.getBlockState().getValue(LargeMachineBlock.FACING)
                : Direction.NORTH;
        Direction right = facing.getClockWise();
        BlockPos frontLeft = this.worldPosition;
        BlockPos frontRight = this.worldPosition.relative(right);
        BlockPos backLeft = this.worldPosition.relative(facing.getOpposite());
        BlockPos backRight = backLeft.relative(right);
        return List.of(
                new Port(frontLeft, facing),
                new Port(frontRight, facing),
                new Port(backLeft, facing.getOpposite()),
                new Port(backRight, facing.getOpposite()),
                new Port(frontRight, right),
                new Port(backRight, right),
                new Port(frontLeft, right.getOpposite()),
                new Port(backLeft, right.getOpposite())
        );
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("Power", this.power);
        tag.putLong("LastInput", this.lastInput);
        tag.putBoolean("Running", this.running);
        tag.put("AirTank", this.airTank.save());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.power = tag.getLong("Power");
        this.lastInput = tag.getLong("LastInput");
        this.running = tag.getBoolean("Running");
        this.airTank.load(tag.getCompound("AirTank"));
        ensureAirTank();
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

    private void tickServer(Level level) {
        ensureAirTank();
        PowerNetworkManager.tickFromEndpoint(level, this);

        boolean wasRunning = this.running;
        this.running = false;
        if (this.power >= POWER_PER_TICK) {
            this.airTank.setAmount(this.airTank.capacity());
            this.power -= POWER_PER_TICK;
            this.running = true;
        }

        pushAir(level);
        setChanged();
        if (wasRunning != this.running || level.getGameTime() % 10L == 0L) {
            sync(level);
        }
    }

    private void tickClient() {
        this.lastFan = this.fan;
        if (!this.running) {
            return;
        }
        this.fan += 45.0F;
        if (this.fan >= 360.0F) {
            this.fan -= 360.0F;
            this.lastFan -= 360.0F;
        }
    }

    private void pushAir(Level level) {
        if (this.airTank.amount() <= 0) {
            return;
        }
        for (Port port : ports()) {
            if (this.airTank.amount() <= 0) {
                break;
            }
            FluidStack stack = HbmFluids.toNeoStack(air(), Math.min(PUSH_PER_PORT, this.airTank.amount()));
            int accepted = HbmFluidNetworks.fillInto(
                    level,
                    port.pos().relative(port.face()),
                    port.face().getOpposite(),
                    stack,
                    this.worldPosition,
                    true
            );
            if (accepted > 0) {
                this.airTank.drain(air(), accepted, false);
            }
        }
    }

    private boolean isAirPort(BlockPos queriedPos, @Nullable Direction side) {
        if (side == null || !side.getAxis().isHorizontal()) {
            return false;
        }
        for (Port port : ports()) {
            if (port.pos().equals(queriedPos) && port.face() == side) {
                return true;
            }
        }
        return false;
    }

    private void ensureAirTank() {
        this.airTank.setCapacity(AIR_CAPACITY);
        if (this.airTank.type().isNone()) {
            this.airTank.setType(air());
        }
    }

    private void sync(Level level) {
        level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    private static HbmFluidDefinition air() {
        return HbmFluids.byName("air").orElse(HbmFluids.none());
    }

    public record Port(BlockPos pos, Direction face) {
    }

    private final class AirOutputHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return airTank.getFluidInTank(tank);
        }

        @Override
        public int getTankCapacity(int tank) {
            return airTank.getTankCapacity(tank);
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return false;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return FluidStack.EMPTY;
            }
            HbmFluidDefinition requested = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            if (requested != air()) {
                return FluidStack.EMPTY;
            }
            return drain(resource.getAmount(), action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            var drained = airTank.drain(air(), maxDrain, action.simulate());
            if (!action.simulate() && !drained.isEmpty()) {
                setChanged();
            }
            return drained.isEmpty() ? FluidStack.EMPTY : HbmFluids.toNeoStack(drained.type(), drained.amount());
        }
    }
}
