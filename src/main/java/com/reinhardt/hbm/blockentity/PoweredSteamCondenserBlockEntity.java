package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.block.PoweredSteamCondenserBlock;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidNetworks;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.fluid.HbmThermalConversions;
import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.util.FluidCopiable;
import com.reinhardt.hbm.util.LegacyMachineGeometry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PoweredSteamCondenserBlockEntity extends BlockEntity implements PowerEndpoint, FluidCopiable {
    public static final long MAX_POWER = 10_000_000L;
    public static final int TANK_CAPACITY = 1_000_000;
    public static final int POWER_PER_MB = 10;

    private final HbmFluidTank inputTank = new HbmFluidTank(spentSteam(), TANK_CAPACITY);
    private final HbmFluidTank outputTank = new HbmFluidTank(water(), TANK_CAPACITY);
    private long power;
    private long lastInput;
    private int waterTimer;
    private int throughput;
    private float spin;
    private float lastSpin;

    public PoweredSteamCondenserBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.POWERED_STEAM_CONDENSER.get(), pos, blockState);
        setupTanks();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PoweredSteamCondenserBlockEntity condenser) {
        if (level.isClientSide) {
            condenser.tickClient(level, state);
            return;
        }
        condenser.tickServer(level);
    }

    public HbmFluidTank inputTank() {
        return this.inputTank;
    }

    public HbmFluidTank outputTank() {
        return this.outputTank;
    }

    public long power() {
        return this.power;
    }

    public long lastInput() {
        return this.lastInput;
    }

    public int throughput() {
        return this.throughput;
    }

    public float spin(float partialTick) {
        return this.lastSpin + (this.spin - this.lastSpin) * partialTick;
    }

    @Override
    public int[] getFluidIdsToCopy() {
        return new int[]{this.inputTank.type().oldId(), this.outputTank.type().oldId()};
    }

    @Override
    public void pasteFluidSetting(HbmFluidDefinition fluid, Level level, Player player, BlockPos pos) {
        if (HbmThermalConversions.condenserStep(fluid).isEmpty()) {
            return;
        }
        if (this.inputTank.amount() == 0) {
            this.inputTank.setType(fluid);
            sync();
        }
    }

    @Nullable
    public IFluidHandler fluidHandler(BlockPos queriedPos, @Nullable Direction side) {
        if (!allowsPort(queriedPos, side)) {
            return null;
        }
        return new CondenserFluidHandler(queriedPos.immutable(), side);
    }

    @Nullable
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return fluidHandler(this.worldPosition, side);
    }

    @Override
    public BlockPos getPowerPos() {
        return this.worldPosition;
    }

    @Override
    public List<BlockPos> getPowerConnectorPositions(LevelAccessor level) {
        return connectorPositions(level);
    }

    @Override
    public boolean canConnectPower(LevelAccessor level, BlockPos connectorPos, Direction machineSide) {
        for (Port port : ports(level)) {
            if (port.connectorPos().equals(connectorPos) && machineSide == port.face()) {
                return true;
            }
        }
        return false;
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
        this.lastInput = receivedInput;
        this.power = Math.min(MAX_POWER, this.power + receivedInput);
        setChanged();
    }

    @Override
    public Component getPowerStatus() {
        return Component.translatable(
                "message.reinhardtshbm.power.condenser_powered",
                this.lastInput,
                this.power,
                MAX_POWER,
                this.throughput
        );
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("InputTank", this.inputTank.save());
        tag.put("OutputTank", this.outputTank.save());
        tag.putLong("Power", this.power);
        tag.putLong("LastInput", this.lastInput);
        tag.putInt("WaterTimer", this.waterTimer);
        tag.putInt("Throughput", this.throughput);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.inputTank.load(tag.getCompound("InputTank"));
        this.outputTank.load(tag.getCompound("OutputTank"));
        this.power = tag.getLong("Power");
        this.lastInput = tag.getLong("LastInput");
        this.waterTimer = tag.getInt("WaterTimer");
        this.throughput = tag.getInt("Throughput");
        setupTanks();
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
        setupTanks();
        if (this.waterTimer > 0) {
            this.waterTimer--;
        }
        tryConvert();
        pullInput(level);
        sendOutput(level);
        PowerNetworkManager.tickFromEndpoint(level, this);
        setChanged();
        if (level.getGameTime() % 10L == 0L || this.waterTimer == 20) {
            sync();
        }
    }

    private void tickClient(Level level, BlockState state) {
        this.lastSpin = this.spin;
        if (this.waterTimer > 0) {
            this.spin += 30.0F;
            if (this.spin >= 360.0F) {
                this.spin -= 360.0F;
                this.lastSpin -= 360.0F;
            }
            if (level.getGameTime() % 4L == 0L) {
                Direction facing = facing(level);
                level.addParticle(ParticleTypes.CLOUD,
                        this.worldPosition.getX() + 0.5D + facing.getStepX() * 1.5D,
                        this.worldPosition.getY() + 1.5D,
                        this.worldPosition.getZ() + 0.5D + facing.getStepZ() * 1.5D,
                        facing.getStepX() * 0.1D, 0.0D, facing.getStepZ() * 0.1D);
                level.addParticle(ParticleTypes.CLOUD,
                        this.worldPosition.getX() + 0.5D - facing.getStepX() * 1.5D,
                        this.worldPosition.getY() + 1.5D,
                        this.worldPosition.getZ() + 0.5D - facing.getStepZ() * 1.5D,
                        -facing.getStepX() * 0.1D, 0.0D, -facing.getStepZ() * 0.1D);
            }
        }
    }

    private void tryConvert() {
        HbmThermalConversions.condenserStep(this.inputTank.type()).ifPresentOrElse(step -> {
            int convert = Math.min(this.inputTank.amount(), this.outputTank.capacity() - this.outputTank.amount());
            this.throughput = convert;
            if (convert <= 0 || this.power < (long) convert * POWER_PER_MB) {
                return;
            }
            this.inputTank.drain(step.input(), convert, false);
            this.outputTank.fill(step.output(), convert, false);
            this.power = Math.max(0L, this.power - (long) convert * POWER_PER_MB);
            this.waterTimer = 20;
        }, () -> this.throughput = 0);
    }

    private void pullInput(Level level) {
        int space = this.inputTank.capacity() - this.inputTank.amount();
        if (space <= 0 || this.inputTank.type().isNone()) {
            return;
        }
        for (Port port : ports(level)) {
            space = this.inputTank.capacity() - this.inputTank.amount();
            if (space <= 0) {
                break;
            }
            FluidStack drained = HbmFluidNetworks.drainFrom(level, port.connectorPos(), port.face().getOpposite(), this.inputTank.type(), space, this.worldPosition, true);
            if (!drained.isEmpty()) {
                this.inputTank.fill(this.inputTank.type(), drained.getAmount(), false);
            }
        }
    }

    private void sendOutput(Level level) {
        if (this.outputTank.amount() <= 0 || this.outputTank.type().isNone()) {
            return;
        }
        for (Port port : ports(level)) {
            if (this.outputTank.amount() <= 0) {
                break;
            }
            FluidStack stack = HbmFluids.toNeoStack(this.outputTank.type(), this.outputTank.amount());
            int accepted = HbmFluidNetworks.fillInto(level, port.connectorPos(), port.face().getOpposite(), stack, this.worldPosition, true);
            if (accepted > 0) {
                this.outputTank.drain(this.outputTank.type(), accepted, false);
            }
        }
    }

    private void setupTanks() {
        this.inputTank.setCapacity(TANK_CAPACITY);
        this.outputTank.setCapacity(TANK_CAPACITY);
        if (this.inputTank.amount() == 0) {
            this.inputTank.setType(spentSteam());
        }
        if (this.outputTank.amount() == 0) {
            this.outputTank.setType(water());
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

    private List<BlockPos> connectorPositions(LevelAccessor level) {
        return ports(level).stream().map(Port::connectorPos).toList();
    }

    private List<Port> ports(LevelAccessor level) {
        Direction facing = facing(level);
        return portsFor(this.worldPosition, facing);
    }

    public static List<Port> portsFor(BlockPos corePos, Direction facing) {
        Direction rot = LegacyMachineGeometry.forgeRotateUp(facing);
        return List.of(
                Port.fromConnector(corePos.relative(rot, 4).above(), rot),
                Port.fromConnector(corePos.relative(rot.getOpposite(), 4).above(), rot.getOpposite()),
                Port.fromConnector(corePos.relative(facing, 2).relative(rot.getOpposite()).above(), facing),
                Port.fromConnector(corePos.relative(facing, 2).relative(rot).above(), facing),
                Port.fromConnector(corePos.relative(facing.getOpposite(), 2).relative(rot.getOpposite()).above(), facing.getOpposite()),
                Port.fromConnector(corePos.relative(facing.getOpposite(), 2).relative(rot).above(), facing.getOpposite())
        );
    }

    private Direction facing(LevelAccessor level) {
        BlockState state = level.getBlockState(this.worldPosition);
        return state.hasProperty(LargeMachineBlock.FACING) ? state.getValue(LargeMachineBlock.FACING) : Direction.SOUTH;
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
    private static HbmFluidDefinition spentSteam() {
        return HbmFluids.byName("spentsteam").orElse(HbmFluids.none());
    }

    private static HbmFluidDefinition water() {
        return HbmFluids.byName("water").orElse(HbmFluids.none());
    }

    public record Port(BlockPos pos, Direction face) {
        private static Port fromConnector(BlockPos connectorPos, Direction face) {
            return new Port(connectorPos.relative(face.getOpposite()).immutable(), face);
        }

        public BlockPos connectorPos() {
            return this.pos.relative(this.face).immutable();
        }
    }

    private final class CondenserFluidHandler implements IFluidHandler {
        private final BlockPos queriedPos;
        @Nullable
        private final Direction side;

        private CondenserFluidHandler(BlockPos queriedPos, @Nullable Direction side) {
            this.queriedPos = queriedPos;
            this.side = side;
        }

        @Override
        public int getTanks() {
            return 2;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank == 0 ? inputTank.getFluidInTank(0) : tank == 1 ? outputTank.getFluidInTank(0) : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 || tank == 1 ? TANK_CAPACITY : 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            if (tank != 0 || stack.isEmpty() || !allowsPort(this.queriedPos, this.side)) {
                return false;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(stack.getFluid()).orElse(HbmFluids.none());
            return HbmThermalConversions.condenserStep(fluid).isPresent();
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !allowsPort(this.queriedPos, this.side)) {
                return 0;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            if (HbmThermalConversions.condenserStep(fluid).isEmpty()) {
                return 0;
            }
            int filled = inputTank.fill(fluid, resource.getAmount(), action.simulate());
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
            return fluid == outputTank.type() ? drain(resource.getAmount(), action) : FluidStack.EMPTY;
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
