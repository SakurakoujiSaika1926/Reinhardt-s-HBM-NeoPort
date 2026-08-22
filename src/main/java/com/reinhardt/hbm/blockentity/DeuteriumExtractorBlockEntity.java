package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidNetworks;
import com.reinhardt.hbm.fluid.HbmFluidStack;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.util.LegacyMachineGeometry;
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
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class DeuteriumExtractorBlockEntity extends BlockEntity implements PowerEndpoint {
    private static final int EXTRACTOR_WATER_CAPACITY = 1_000;
    private static final int EXTRACTOR_HEAVY_WATER_CAPACITY = 100;
    private static final long EXTRACTOR_MAX_POWER = 10_000L;
    private static final int TOWER_WATER_CAPACITY = 50_000;
    private static final int TOWER_HEAVY_WATER_CAPACITY = 5_000;
    private static final long TOWER_MAX_POWER = 100_000L;
    private static final int WATER_PER_HEAVY_WATER = 50;
    private static final int MIN_WATER_TO_RUN = 100;
    private static final int PULL_PER_PORT = 1_000;
    private static final int PUSH_PER_PORT = 1_000;

    private final HbmFluidTank waterTank = new HbmFluidTank(water(), EXTRACTOR_WATER_CAPACITY);
    private final HbmFluidTank heavyWaterTank = new HbmFluidTank(heavyWater(), EXTRACTOR_HEAVY_WATER_CAPACITY);
    private long power;
    private long lastInput;
    private boolean active;

    public DeuteriumExtractorBlockEntity(BlockPos pos, BlockState blockState) {
        this(HbmBlockEntities.DEUTERIUM_EXTRACTOR.get(), pos, blockState);
    }

    protected DeuteriumExtractorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, DeuteriumExtractorBlockEntity extractor) {
        if (!level.isClientSide) {
            extractor.tickServer(level);
        }
    }

    public HbmFluidTank inputTank() {
        return this.waterTank;
    }

    public HbmFluidTank outputTank() {
        return this.heavyWaterTank;
    }

    public long power() {
        return this.power;
    }

    public long maxPower() {
        return kind().maxPower();
    }

    public long powerPerOperation() {
        return kind().powerPerOperation();
    }

    public long lastInput() {
        return this.lastInput;
    }

    public boolean active() {
        return this.active;
    }

    @Nullable
    public IFluidHandler fluidHandler(BlockPos queriedPos, @Nullable Direction side) {
        if (!allowsPort(queriedPos, side)) {
            return null;
        }
        return new DeuteriumFluidHandler();
    }

    @Nullable
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return fluidHandler(this.worldPosition, side);
    }

    public List<Port> ports() {
        if (kind() == Kind.TOWER) {
            return towerPorts(this.worldPosition, getBlockState());
        }
        ArrayList<Port> ports = new ArrayList<>(Direction.values().length);
        for (Direction direction : Direction.values()) {
            ports.add(new Port(this.worldPosition, direction));
        }
        return List.copyOf(ports);
    }

    public static List<Port> towerPorts(BlockPos corePos, BlockState state) {
        Direction facing = state.hasProperty(LargeMachineBlock.FACING)
                ? state.getValue(LargeMachineBlock.FACING)
                : Direction.SOUTH;
        Direction rot = LegacyMachineGeometry.forgeRotateDown(facing);
        return List.of(
                Port.fromConnector(corePos.relative(facing, -2), facing.getOpposite()),
                Port.fromConnector(corePos.relative(facing, -2).relative(rot), facing.getOpposite()),
                Port.fromConnector(corePos.relative(facing), facing),
                Port.fromConnector(corePos.relative(facing).relative(rot), facing),
                Port.fromConnector(corePos.relative(rot, -1), rot.getOpposite()),
                Port.fromConnector(corePos.relative(facing, -1).relative(rot, -1), rot.getOpposite()),
                Port.fromConnector(corePos.relative(rot, 2), rot),
                Port.fromConnector(corePos.relative(facing, -1).relative(rot, 2), rot)
        );
    }

    @Override
    public BlockPos getPowerPos() {
        return this.worldPosition;
    }

    @Override
    public List<BlockPos> getPowerConnectorPositions(LevelAccessor level) {
        ArrayList<BlockPos> connectors = new ArrayList<>();
        for (Port port : ports()) {
            connectors.add(port.connectorPos());
        }
        return List.copyOf(connectors);
    }

    @Override
    public boolean canConnectPower(LevelAccessor level, BlockPos connectorPos, Direction machineSide) {
        for (Port port : ports()) {
            if (port.connectorPos().equals(connectorPos) && port.face() == machineSide) {
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
        return Math.max(0L, maxPower() - this.power);
    }

    @Override
    public void applyPower(long usedOutput, long receivedInput) {
        this.power = Math.min(maxPower(), this.power + receivedInput);
        this.lastInput = receivedInput;
        setChanged();
    }

    @Override
    public Component getPowerStatus() {
        return Component.literal(String.format("%,d / %,d HE", this.power, maxPower()));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("Power", this.power);
        tag.putLong("LastInput", this.lastInput);
        tag.putBoolean("Active", this.active);
        tag.put("Water", this.waterTank.save());
        tag.put("HeavyWater", this.heavyWaterTank.save());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        configureTanks();
        this.power = Math.min(tag.getLong("Power"), maxPower());
        this.lastInput = tag.getLong("LastInput");
        this.active = tag.getBoolean("Active");
        this.waterTank.load(tag.getCompound("Water"));
        this.heavyWaterTank.load(tag.getCompound("HeavyWater"));
        configureTanks();
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
        configureTanks();
        PowerNetworkManager.tickFromEndpoint(level, this);
        pullWater(level);

        boolean wasActive = this.active;
        this.active = convert();

        pushHeavyWater(level);
        setChanged();
        if (wasActive != this.active || level.getGameTime() % 10L == 0L) {
            sync(level);
        }
    }

    private boolean convert() {
        if (this.power < powerPerOperation()
                || this.waterTank.amount() < MIN_WATER_TO_RUN
                || this.heavyWaterTank.amount() >= this.heavyWaterTank.capacity()) {
            return false;
        }
        int convert = Math.min(this.heavyWaterTank.capacity(), this.waterTank.amount()) / WATER_PER_HEAVY_WATER;
        convert = Math.min(convert, this.heavyWaterTank.capacity() - this.heavyWaterTank.amount());
        if (convert <= 0) {
            return false;
        }
        this.waterTank.drain(water(), convert * WATER_PER_HEAVY_WATER, false);
        this.heavyWaterTank.fill(heavyWater(), convert, false);
        this.power -= powerPerOperation();
        return true;
    }

    private void pullWater(Level level) {
        if (this.waterTank.amount() >= this.waterTank.capacity()) {
            return;
        }
        for (Port port : ports()) {
            int space = this.waterTank.capacity() - this.waterTank.amount();
            if (space <= 0) {
                break;
            }
            FluidStack drained = HbmFluidNetworks.drainFrom(
                    level,
                    port.connectorPos(),
                    port.face().getOpposite(),
                    water(),
                    Math.min(PULL_PER_PORT, space),
                    this.worldPosition,
                    true
            );
            if (!drained.isEmpty()) {
                this.waterTank.fill(water(), drained.getAmount(), false);
            }
        }
    }

    private void pushHeavyWater(Level level) {
        if (this.heavyWaterTank.amount() <= 0) {
            return;
        }
        for (Port port : ports()) {
            if (this.heavyWaterTank.amount() <= 0) {
                break;
            }
            FluidStack stack = HbmFluids.toNeoStack(heavyWater(), Math.min(PUSH_PER_PORT, this.heavyWaterTank.amount()));
            int accepted = HbmFluidNetworks.fillInto(
                    level,
                    port.connectorPos(),
                    port.face().getOpposite(),
                    stack,
                    this.worldPosition,
                    true
            );
            if (accepted > 0) {
                this.heavyWaterTank.drain(heavyWater(), accepted, false);
            }
        }
    }

    private boolean allowsPort(BlockPos queriedPos, @Nullable Direction side) {
        for (Port port : ports()) {
            if (port.pos().equals(queriedPos) && (side == null || side == port.face())) {
                return true;
            }
        }
        return false;
    }

    private Kind kind() {
        return getBlockState().is(HbmBlocks.MACHINE_DEUTERIUM_TOWER.get()) ? Kind.TOWER : Kind.EXTRACTOR;
    }

    private void configureTanks() {
        Kind kind = kind();
        this.waterTank.setCapacity(kind.waterCapacity());
        this.heavyWaterTank.setCapacity(kind.heavyWaterCapacity());
        if (this.waterTank.amount() == 0 && this.waterTank.type() != water()) {
            this.waterTank.setType(water());
        }
        if (this.heavyWaterTank.amount() == 0 && this.heavyWaterTank.type() != heavyWater()) {
            this.heavyWaterTank.setType(heavyWater());
        }
    }

    private void sync(Level level) {
        level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    private static HbmFluidDefinition water() {
        return HbmFluids.byName("water").orElse(HbmFluids.none());
    }

    private static HbmFluidDefinition heavyWater() {
        return HbmFluids.byName("heavywater").orElse(HbmFluids.none());
    }

    public record Port(BlockPos pos, Direction face) {
        public static Port fromConnector(BlockPos connectorPos, Direction face) {
            return new Port(connectorPos.relative(face.getOpposite()).immutable(), face);
        }

        public BlockPos connectorPos() {
            return this.pos.relative(this.face).immutable();
        }
    }

    private enum Kind {
        EXTRACTOR(EXTRACTOR_WATER_CAPACITY, EXTRACTOR_HEAVY_WATER_CAPACITY, EXTRACTOR_MAX_POWER),
        TOWER(TOWER_WATER_CAPACITY, TOWER_HEAVY_WATER_CAPACITY, TOWER_MAX_POWER);

        private final int waterCapacity;
        private final int heavyWaterCapacity;
        private final long maxPower;

        Kind(int waterCapacity, int heavyWaterCapacity, long maxPower) {
            this.waterCapacity = waterCapacity;
            this.heavyWaterCapacity = heavyWaterCapacity;
            this.maxPower = maxPower;
        }

        int waterCapacity() {
            return this.waterCapacity;
        }

        int heavyWaterCapacity() {
            return this.heavyWaterCapacity;
        }

        long maxPower() {
            return this.maxPower;
        }

        long powerPerOperation() {
            return this.maxPower / 20L;
        }
    }

    private final class DeuteriumFluidHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 2;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            if (tank == 0) {
                return waterTank.getFluidInTank(0);
            }
            if (tank == 1) {
                return heavyWaterTank.getFluidInTank(0);
            }
            return FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            if (tank == 0) {
                return waterTank.capacity();
            }
            if (tank == 1) {
                return heavyWaterTank.capacity();
            }
            return 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && HbmFluids.fromNeoFluid(stack.getFluid()).filter(fluid -> fluid == water()).isPresent();
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return 0;
            }
            HbmFluidDefinition type = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            if (type != water()) {
                return 0;
            }
            return waterTank.fill(water(), resource.getAmount(), action.simulate());
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return FluidStack.EMPTY;
            }
            HbmFluidDefinition type = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            if (type != heavyWater()) {
                return FluidStack.EMPTY;
            }
            HbmFluidStack drained = heavyWaterTank.drain(heavyWater(), resource.getAmount(), action.simulate());
            return drained.isEmpty() ? FluidStack.EMPTY : HbmFluids.toNeoStack(drained.type(), drained.amount());
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            HbmFluidStack drained = heavyWaterTank.drain(heavyWater(), maxDrain, action.simulate());
            return drained.isEmpty() ? FluidStack.EMPTY : HbmFluids.toNeoStack(drained.type(), drained.amount());
        }
    }
}
