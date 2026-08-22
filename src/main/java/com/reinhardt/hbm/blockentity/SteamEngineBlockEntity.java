package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidNetworks;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.fluid.HbmThermalConversions;
import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import com.reinhardt.hbm.util.LegacyMachineGeometry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundSource;
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

public class SteamEngineBlockEntity extends BlockEntity implements PowerEndpoint {
    public static final int STEAM_CAPACITY = 2_000;
    public static final int SPENT_STEAM_CAPACITY = 20;
    private static final double EFFICIENCY = 0.85D;
    private static final int MAX_PULL_PER_PORT = 2_000;
    private static final int MAX_PUSH_PER_PORT = 2_000;

    private final HbmFluidTank inputTank = new HbmFluidTank(steam(), STEAM_CAPACITY);
    private final HbmFluidTank outputTank = new HbmFluidTank(spentSteam(), SPENT_STEAM_CAPACITY);
    private long powerBuffer;
    private long lastOutput;
    private boolean active;
    private float rotor;
    private float lastRotor;
    private float acceleration;

    public SteamEngineBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.STEAM_ENGINE.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SteamEngineBlockEntity engine) {
        if (level.isClientSide) {
            engine.tickClient();
            return;
        }
        engine.tickServer(level);
    }

    public HbmFluidTank inputTank() {
        return this.inputTank;
    }

    public HbmFluidTank outputTank() {
        return this.outputTank;
    }

    public long powerBuffer() {
        return this.powerBuffer;
    }

    public long lastOutput() {
        return this.lastOutput;
    }

    public boolean active() {
        return this.active;
    }

    public float rotor(float partialTick) {
        return this.lastRotor + (this.rotor - this.lastRotor) * partialTick;
    }

    @Nullable
    public IFluidHandler fluidHandler(BlockPos queriedPos, @Nullable Direction side) {
        if (!allowsPort(queriedPos, side)) {
            return null;
        }
        return new SteamEngineFluidHandler(queriedPos.immutable(), side);
    }

    @Nullable
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return fluidHandler(this.worldPosition, side);
    }

    public List<Port> ports(LevelAccessor level) {
        BlockState state = level.getBlockState(this.worldPosition);
        Direction facing = state.hasProperty(LargeMachineBlock.FACING) ? state.getValue(LargeMachineBlock.FACING) : Direction.SOUTH;
        return portsFor(this.worldPosition, facing);
    }

    public static List<Port> portsFor(BlockPos corePos, Direction facing) {
        Direction right = LegacyMachineGeometry.forgeRotateUp(facing);
        BlockPos base = corePos.relative(right, 2).above();
        return List.of(
                Port.fromConnector(base, right),
                Port.fromConnector(base.relative(facing), right),
                Port.fromConnector(base.relative(facing.getOpposite()), right)
        );
    }

    @Override
    public BlockPos getPowerPos() {
        return this.worldPosition;
    }

    @Override
    public List<BlockPos> getPowerConnectorPositions(LevelAccessor level) {
        List<BlockPos> connectors = new ArrayList<>(3);
        for (Port port : ports(level)) {
            connectors.add(port.connectorPos().immutable());
        }
        return List.copyOf(connectors);
    }

    @Override
    public boolean canConnectPower(LevelAccessor level, BlockPos connectorPos, Direction machineSide) {
        for (Port port : ports(level)) {
            if (port.connectorPos().equals(connectorPos) && port.face() == machineSide) {
                return true;
            }
        }
        return false;
    }

    @Override
    public long getAvailableOutput() {
        return this.powerBuffer;
    }

    @Override
    public long getRequestedInput() {
        return 0L;
    }

    @Override
    public void applyPower(long usedOutput, long receivedInput) {
        this.lastOutput = usedOutput;
        setChanged();
    }

    @Override
    public Component getPowerStatus() {
        return Component.literal(String.format("%,d HE/t", this.lastOutput));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("InputTank", this.inputTank.save());
        tag.put("OutputTank", this.outputTank.save());
        tag.putLong("PowerBuffer", this.powerBuffer);
        tag.putLong("LastOutput", this.lastOutput);
        tag.putBoolean("Active", this.active);
        tag.putFloat("Rotor", this.rotor);
        tag.putFloat("LastRotor", this.lastRotor);
        tag.putFloat("Acceleration", this.acceleration);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.inputTank.load(tag.getCompound("InputTank"));
        this.outputTank.load(tag.getCompound("OutputTank"));
        this.powerBuffer = tag.getLong("PowerBuffer");
        this.lastOutput = tag.getLong("LastOutput");
        this.active = tag.getBoolean("Active");
        this.rotor = tag.getFloat("Rotor");
        this.lastRotor = tag.getFloat("LastRotor");
        this.acceleration = tag.getFloat("Acceleration");
        ensureTankTypes();
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
        ensureTankTypes();
        boolean oldActive = this.active;

        tryConvert(level);
        pullInput(level);
        sendOutput(level);
        PowerNetworkManager.tickFromEndpoint(level, this);

        setChanged();
        if (oldActive != this.active || level.getGameTime() % 10L == 0L) {
            sync();
        }
    }

    private void tickClient() {
        this.lastRotor = this.rotor;
        if (this.acceleration <= 0.0F) {
            return;
        }
        this.rotor += this.acceleration;
        wrapRotor();
    }

    private void tryConvert(Level level) {
        this.powerBuffer = 0L;
        this.active = false;

        HbmThermalConversions.turbineStep(steam()).ifPresent(step -> {
            int inputOps = this.inputTank.amount() / step.amountReq();
            int outputOps = (this.outputTank.capacity() - this.outputTank.amount()) / step.amountProduced();
            int ops = Math.min(inputOps, outputOps);
            if (ops <= 0) {
                return;
            }

            this.inputTank.drain(step.input(), ops * step.amountReq(), false);
            this.outputTank.fill(step.output(), ops * step.amountProduced(), false);
            this.powerBuffer = (long) (ops * step.heatEnergy() * step.turbineEfficiency() * EFFICIENCY);
            this.active = true;
        });

        if (this.active) {
            this.acceleration += 0.1F;
        } else {
            this.acceleration -= 0.1F;
        }
        this.acceleration = Math.max(0.0F, Math.min(40.0F, this.acceleration));

        this.lastRotor = this.rotor;
        this.rotor += this.acceleration;
        if (this.rotor >= 360.0F) {
            this.rotor -= 360.0F;
            this.lastRotor -= 360.0F;
            level.playSound(
                    null,
                    this.worldPosition,
                    HbmSoundEvents.STEAM_ENGINE_OPERATE.get(),
                    SoundSource.BLOCKS,
                    1.0F,
                    0.5F + this.acceleration / 80.0F
            );
        }
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
                    steam(),
                    Math.min(MAX_PULL_PER_PORT, space),
                    this.worldPosition,
                    true
            );
            if (!drained.isEmpty()) {
                this.inputTank.fill(steam(), drained.getAmount(), false);
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
            int amount = Math.min(MAX_PUSH_PER_PORT, this.outputTank.amount());
            FluidStack stack = HbmFluids.toNeoStack(this.outputTank.type(), amount);
            int accepted = HbmFluidNetworks.fillInto(
                    level,
                    port.connectorPos(),
                    port.face().getOpposite(),
                    stack,
                    this.worldPosition,
                    true
            );
            if (accepted > 0) {
                this.outputTank.drain(this.outputTank.type(), accepted, false);
            }
        }
    }

    private void ensureTankTypes() {
        if (this.inputTank.amount() == 0 && this.inputTank.type() != steam()) {
            this.inputTank.setType(steam());
        }
        if (this.outputTank.amount() == 0 && this.outputTank.type() != spentSteam()) {
            this.outputTank.setType(spentSteam());
        }
    }

    private boolean allowsPort(BlockPos queriedPos, @Nullable Direction side) {
        if (this.level == null) {
            return false;
        }
        for (Port port : ports(this.level)) {
            if (port.proxyPos().equals(queriedPos) && (side == null || side == port.face())) {
                return true;
            }
        }
        return false;
    }

    private void wrapRotor() {
        if (this.rotor >= 360.0F) {
            this.rotor -= 360.0F;
            this.lastRotor -= 360.0F;
        }
    }

    private void sync() {
        setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.invalidateCapabilities(this.worldPosition);
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
            for (Port port : ports(this.level)) {
                this.level.invalidateCapabilities(port.proxyPos());
            }
        }
    }

    private static HbmFluidDefinition steam() {
        return HbmFluids.byName("steam").orElse(HbmFluids.none());
    }

    private static HbmFluidDefinition spentSteam() {
        return HbmFluids.byName("spentsteam").orElse(HbmFluids.none());
    }

    public record Port(BlockPos proxyPos, BlockPos connectorPos, Direction face) {
        private static Port fromConnector(BlockPos connectorPos, Direction face) {
            return new Port(connectorPos.relative(face.getOpposite()).immutable(), connectorPos.immutable(), face);
        }
    }

    private final class SteamEngineFluidHandler implements IFluidHandler {
        private final BlockPos queriedPos;
        @Nullable
        private final Direction side;

        private SteamEngineFluidHandler(BlockPos queriedPos, @Nullable Direction side) {
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
            return fluid == steam();
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !allowsPort(this.queriedPos, this.side)) {
                return 0;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            if (fluid != steam()) {
                return 0;
            }
            int filled = inputTank.fill(steam(), resource.getAmount(), action.simulate());
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
            if (fluid != spentSteam()) {
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
