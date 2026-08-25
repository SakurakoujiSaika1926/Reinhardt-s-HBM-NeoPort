package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.CoolingTowerBlock;
import com.reinhardt.hbm.config.HbmClientConfig;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidNetworks;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.fluid.HbmThermalConversions;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmParticleTypes;
import com.reinhardt.hbm.util.FluidCopiable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CoolingTowerBlockEntity extends BlockEntity implements FluidCopiable {
    private static final Direction[] PORT_DIRECTIONS = {
            Direction.NORTH,
            Direction.SOUTH,
            Direction.WEST,
            Direction.EAST
    };

    private final HbmFluidTank inputTank = new HbmFluidTank(spentSteam(), CoolingTowerBlock.Kind.SMALL.inputCapacity());
    private final HbmFluidTank outputTank = new HbmFluidTank(water(), CoolingTowerBlock.Kind.SMALL.outputCapacity());
    private int waterTimer;
    private int throughput;

    public CoolingTowerBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.COOLING_TOWER.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CoolingTowerBlockEntity tower) {
        tower.setupTanks();
        tower.tryConvert();
        tower.sendOutput(level);
        tower.setChanged();
        if (level.getGameTime() % 20L == 0L) {
            tower.sync();
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, CoolingTowerBlockEntity tower) {
        if (!HbmClientConfig.ENABLE_COOLING_TOWER_PARTICLES.get()) {
            return;
        }
        CoolingTowerBlock.Kind kind = tower.kind();
        long interval = kind == CoolingTowerBlock.Kind.LARGE ? 4L : 2L;
        if (tower.waterTimer <= 0 || level.getGameTime() % interval != 0L) {
            return;
        }
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + (kind == CoolingTowerBlock.Kind.LARGE ? 1.0D : 18.0D);
        double z = pos.getZ() + 0.5D;
        if (kind == CoolingTowerBlock.Kind.LARGE) {
            x += level.random.nextDouble() * 3.0D - 1.5D;
            z += level.random.nextDouble() * 3.0D - 1.5D;
        }
        level.addParticle(
                HbmParticleTypes.COOLING_TOWER.get(),
                x,
                y,
                z,
                kind == CoolingTowerBlock.Kind.LARGE ? 1.0D : 0.0D,
                0.0D,
                0.0D
        );
    }

    public HbmFluidTank inputTank() {
        return this.inputTank;
    }

    public HbmFluidTank outputTank() {
        return this.outputTank;
    }

    public int throughput() {
        return this.throughput;
    }

    public int waterTimer() {
        return this.waterTimer;
    }

    @Override
    public int[] getFluidIdsToCopy() {
        return new int[]{this.inputTank.type().oldId(), this.outputTank.type().oldId()};
    }

    @Override
    public void pasteFluidSetting(HbmFluidDefinition fluid, Level level, Player player, BlockPos pos) {
        if (fluid == spentSteam() && this.inputTank.amount() == 0) {
            this.inputTank.setType(fluid);
            sync();
        }
    }

    @Nullable
    public IFluidHandler fluidHandler(BlockPos queriedPos, @Nullable Direction side) {
        if (queriedPos.equals(this.worldPosition)) {
            return side == null ? new CoolingTowerFluidHandler() : null;
        }
        if (!allowsPort(queriedPos, side)) {
            return null;
        }
        return new CoolingTowerFluidHandler();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("InputTank", this.inputTank.save());
        tag.put("OutputTank", this.outputTank.save());
        tag.putInt("WaterTimer", this.waterTimer);
        tag.putInt("Throughput", this.throughput);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        setupTanks();
        this.inputTank.load(tag.getCompound("InputTank"));
        this.outputTank.load(tag.getCompound("OutputTank"));
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

    private boolean allowsPort(BlockPos queriedPos, @Nullable Direction side) {
        for (Port port : ports()) {
            if (queriedPos.equals(port.dummyPos()) && (side == null || side == port.face())) {
                return true;
            }
        }
        return false;
    }

    private List<Port> ports() {
        return ports(this.worldPosition, kind());
    }

    private void setupTanks() {
        HbmFluidDefinition spent = spentSteam();
        HbmFluidDefinition water = water();
        CoolingTowerBlock.Kind kind = kind();
        this.inputTank.setCapacity(kind.inputCapacity());
        this.outputTank.setCapacity(kind.outputCapacity());
        if (this.inputTank.amount() == 0 && this.inputTank.type() != spent) {
            this.inputTank.setType(spent);
        }
        if (this.outputTank.amount() == 0 && this.outputTank.type() != water) {
            this.outputTank.setType(water);
        }
    }

    private void tryConvert() {
        if (this.waterTimer > 0) {
            this.waterTimer--;
        }
        HbmThermalConversions.condenserStep(this.inputTank.type()).ifPresentOrElse(step -> {
            int convert = Math.min(this.inputTank.amount(), this.outputTank.capacity() - this.outputTank.amount());
            this.throughput = convert;
            if (convert <= 0) {
                return;
            }
            this.inputTank.drain(step.input(), convert, false);
            this.outputTank.fill(step.output(), convert, false);
            this.waterTimer = 20;
        }, () -> this.throughput = 0);
    }

    private void sendOutput(Level level) {
        if (this.outputTank.amount() <= 0 || this.outputTank.type().isNone()) {
            return;
        }
        for (Port port : ports()) {
            if (this.outputTank.amount() <= 0) {
                break;
            }
            int amount = Math.min(16_000, this.outputTank.amount());
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

    private void sync() {
        setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.invalidateCapabilities(this.worldPosition);
            for (Port port : ports()) {
                this.level.invalidateCapabilities(port.dummyPos());
            }
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public CoolingTowerBlock.Kind kind() {
        if (this.getBlockState().getBlock() instanceof CoolingTowerBlock towerBlock) {
            return towerBlock.kind();
        }
        return CoolingTowerBlock.Kind.SMALL;
    }

    public static List<Port> ports(BlockPos corePos, CoolingTowerBlock.Kind kind) {
        ArrayList<Port> ports = new ArrayList<>();
        if (kind == CoolingTowerBlock.Kind.SMALL) {
            for (Direction direction : PORT_DIRECTIONS) {
                ports.add(new Port(corePos.relative(direction, 2), corePos.relative(direction, 3), direction));
            }
            return List.copyOf(ports);
        }

        for (Direction direction : PORT_DIRECTIONS) {
            Direction rot = direction.getClockWise();
            BlockPos sideCenter = corePos.relative(direction, 4);
            BlockPos connectorCenter = corePos.relative(direction, 5);
            ports.add(new Port(sideCenter, connectorCenter, direction));
            ports.add(new Port(sideCenter.relative(rot, 3), connectorCenter.relative(rot, 3), direction));
            ports.add(new Port(sideCenter.relative(rot, -3), connectorCenter.relative(rot, -3), direction));
        }
        return List.copyOf(ports);
    }

    private static HbmFluidDefinition spentSteam() {
        return HbmFluids.byName("spentsteam").orElse(HbmFluids.none());
    }

    private static HbmFluidDefinition water() {
        return HbmFluids.byName("water").orElse(HbmFluids.none());
    }

    private final class CoolingTowerFluidHandler implements IFluidHandler {
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
            if (tank == 0) {
                return inputTank.capacity();
            }
            if (tank == 1) {
                return outputTank.capacity();
            }
            return 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            if (tank != 0 || stack.isEmpty()) {
                return false;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(stack.getFluid()).orElse(HbmFluids.none());
            return fluid == spentSteam();
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return 0;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            if (fluid != spentSteam()) {
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
            if (resource.isEmpty()) {
                return FluidStack.EMPTY;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            if (fluid != outputTank.type()) {
                return FluidStack.EMPTY;
            }
            return drain(resource.getAmount(), action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            FluidStack drained = outputTank.drain(maxDrain, action);
            if (!drained.isEmpty() && action.execute()) {
                sync();
            }
            return drained;
        }
    }

    public record Port(BlockPos dummyPos, BlockPos connectorPos, Direction face) {
    }
}
