package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidNetworks;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.fluid.HbmThermalConversions;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmFluids;
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
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class HeatBoilerBlockEntity extends BlockEntity implements FluidCopiable {
    public static final int INPUT_CAPACITY = 16_000;
    public static final int MAX_HEAT = 3_200_000;
    private static final double DIFFUSION = 0.1D;

    private final HbmFluidTank inputTank;
    private final HbmFluidTank outputTank;
    private HbmFluidDefinition configuredInput = HbmFluids.byName("water").orElse(HbmFluids.none());
    private int heat;
    private boolean active;
    private boolean exploded;

    public HeatBoilerBlockEntity(BlockPos pos, BlockState blockState) {
        this(HbmBlockEntities.HEAT_BOILER.get(), pos, blockState, INPUT_CAPACITY);
    }

    protected HeatBoilerBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        this(type, pos, blockState, INPUT_CAPACITY);
    }

    protected HeatBoilerBlockEntity(
            net.minecraft.world.level.block.entity.BlockEntityType<?> type,
            BlockPos pos,
            BlockState blockState,
            int inputCapacity
    ) {
        super(type, pos, blockState);
        this.inputTank = new HbmFluidTank(HbmFluids.byName("water").orElse(HbmFluids.none()), inputCapacity);
        this.outputTank = new HbmFluidTank(HbmFluids.byName("steam").orElse(HbmFluids.none()), inputCapacity * 100);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, HeatBoilerBlockEntity boiler) {
        if (level.isClientSide) {
            return;
        }
        if (boiler.exploded) {
            boiler.active = false;
            if (level.getGameTime() % 10L == 0L) {
                boiler.sync();
            }
            return;
        }
        boiler.setupTanks();
        boiler.pullHeatFromBelow(level);
        boiler.tryConvert();
        boiler.sendOutput(level, state);
        boiler.setChanged();
        if (level.getGameTime() % 10L == 0L) {
            boiler.sync();
        }
    }

    public HbmFluidTank inputTank() {
        return this.inputTank;
    }

    public HbmFluidTank outputTank() {
        return this.outputTank;
    }

    public HbmFluidDefinition configuredInput() {
        return this.configuredInput;
    }

    public int heat() {
        return this.heat;
    }

    public boolean active() {
        return this.active;
    }

    public boolean exploded() {
        return this.exploded;
    }

    public void setConfiguredInput(HbmFluidDefinition fluid) {
        HbmFluidDefinition next = fluid == null ? HbmFluids.none() : fluid;
        if (HbmThermalConversions.firstBoilerStep(next).isEmpty()) {
            next = HbmFluids.none();
        }
        if (this.configuredInput == next) {
            return;
        }
        this.configuredInput = next;
        this.inputTank.clear();
        this.outputTank.clear();
        setupTanks();
        sync();
    }

    @Override
    public int[] getFluidIdsToCopy() {
        return new int[]{this.configuredInput.oldId(), this.outputTank.type().oldId()};
    }

    @Override
    public void pasteFluidSetting(HbmFluidDefinition fluid, Level level, Player player, BlockPos pos) {
        setConfiguredInput(fluid);
    }

    @Nullable
    public IFluidHandler fluidHandler(BlockPos queriedPos, @Nullable Direction side) {
        if (!allowsPort(queriedPos, side)) {
            return null;
        }
        return new BoilerFluidHandler(queriedPos.immutable(), side);
    }

    public List<Port> ports(LevelAccessor level) {
        BlockState state = level.getBlockState(this.worldPosition);
        Direction facing = state.hasProperty(LargeMachineBlock.FACING) ? state.getValue(LargeMachineBlock.FACING) : Direction.NORTH;
        Direction right = facing.getClockWise();
        Direction left = facing.getCounterClockWise();
        return List.of(
                new Port(this.worldPosition.relative(right), right),
                new Port(this.worldPosition.relative(left), left),
                new Port(this.worldPosition.above(3), Direction.UP)
        );
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("InputTank", this.inputTank.save());
        tag.put("OutputTank", this.outputTank.save());
        tag.putString("ConfiguredInput", this.configuredInput.name());
        tag.putInt("Heat", this.heat);
        tag.putBoolean("Active", this.active);
        tag.putBoolean("Exploded", this.exploded);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.configuredInput = HbmFluids.byName(tag.getString("ConfiguredInput"))
                .orElse(HbmFluids.byName("water").orElse(HbmFluids.none()));
        this.inputTank.load(tag.getCompound("InputTank"));
        this.outputTank.load(tag.getCompound("OutputTank"));
        this.heat = tag.getInt("Heat");
        this.active = tag.getBoolean("Active");
        this.exploded = tag.getBoolean("Exploded");
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

    private void setupTanks() {
        HbmThermalConversions.firstBoilerStep(this.configuredInput).ifPresentOrElse(step -> {
            if (this.inputTank.amount() == 0 && this.inputTank.type() != step.input()) {
                this.inputTank.setType(step.input());
            }
            int outputCapacity = Math.max(1, (inputCapacity() * step.amountProduced()) / Math.max(1, step.amountReq()));
            this.outputTank.setCapacity(outputCapacity);
            if (this.outputTank.amount() == 0 && this.outputTank.type() != step.output()) {
                this.outputTank.setType(step.output());
            }
        }, () -> {
            this.inputTank.clear();
            this.outputTank.clear();
        });
    }

    private void pullHeatFromBelow(Level level) {
        if (this.heat >= maxHeat()) {
            return;
        }
        HeatSourceBlockEntity source = heatSourceBelow(level);
        if (source == null) {
            coolPassively();
            return;
        }

        int diff = source.getHeatStored() - this.heat;
        if (diff <= 0) {
            return;
        }
        int pulled = (int) Math.ceil(diff * diffusion());
        source.useHeat(pulled);
        this.heat = Math.min(maxHeat(), this.heat + pulled);
    }

    @Nullable
    private HeatSourceBlockEntity heatSourceBelow(Level level) {
        BlockEntity blockEntity = level.getBlockEntity(this.worldPosition.below());
        if (blockEntity instanceof HeatSourceBlockEntity source) {
            return source;
        }
        if (blockEntity instanceof MachineDummyBlockEntity dummy
                && level.getBlockEntity(dummy.getCorePos()) instanceof HeatSourceBlockEntity source) {
            return source;
        }
        return null;
    }

    private void coolPassively() {
        if (this.heat > 0) {
            this.heat = Math.max(0, this.heat - Math.max(this.heat / 1000, 1));
        }
    }

    private void tryConvert() {
        this.active = false;
        HbmThermalConversions.firstBoilerStep(this.configuredInput).ifPresent(step -> {
            int heatReq = (int) Math.max(step.heatReq() / step.boilerEfficiency(), 1.0D);
            int inputOps = this.inputTank.amount() / step.amountReq();
            int outputOps = (this.outputTank.capacity() - this.outputTank.amount()) / step.amountProduced();
            int heatOps = this.heat / heatReq;
            if (outputOps <= 0) {
                if (canExplode()) {
                    explode();
                }
                return;
            }
            int ops = Math.min(inputOps, Math.min(outputOps, heatOps));
            if (ops <= 0) {
                return;
            }
            this.inputTank.drain(step.input(), step.amountReq() * ops, false);
            this.outputTank.fill(step.output(), step.amountProduced() * ops, false);
            this.heat -= heatReq * ops;
            this.active = true;
        });
    }

    private void sendOutput(Level level, BlockState state) {
        if (this.outputTank.amount() <= 0 || this.outputTank.type().isNone()) {
            return;
        }
        for (Port port : ports(level)) {
            if (this.outputTank.amount() <= 0) {
                break;
            }
            FluidStack stack = HbmFluids.toNeoStack(this.outputTank.type(), this.outputTank.amount());
            BlockPos target = port.pos().relative(port.face());
            int accepted = HbmFluidNetworks.fillInto(level, target, port.face().getOpposite(), stack, this.worldPosition, true);
            if (accepted > 0) {
                this.outputTank.drain(this.outputTank.type(), accepted, false);
            }
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

    protected int inputCapacity() {
        return INPUT_CAPACITY;
    }

    protected int maxHeat() {
        return MAX_HEAT;
    }

    protected double diffusion() {
        return DIFFUSION;
    }

    protected boolean canExplode() {
        return true;
    }

    protected void explode() {
        if (this.level == null || this.level.isClientSide || this.exploded) {
            return;
        }
        this.exploded = true;
        this.active = false;
        for (int x = this.worldPosition.getX() - 1; x <= this.worldPosition.getX() + 1; x++) {
            for (int y = this.worldPosition.getY() + 2; y <= this.worldPosition.getY() + 3; y++) {
                for (int z = this.worldPosition.getZ() - 1; z <= this.worldPosition.getZ() + 1; z++) {
                    BlockPos dummyPos = new BlockPos(x, y, z);
                    if (this.level.getBlockState(dummyPos).is(HbmBlocks.MACHINE_DUMMY.get())) {
                        this.level.removeBlock(dummyPos, false);
                    }
                }
            }
        }
        BlockPos centerTop = this.worldPosition.above();
        if (this.level.getBlockState(centerTop).is(HbmBlocks.MACHINE_DUMMY.get())) {
            this.level.removeBlock(centerTop, false);
        }
        this.level.explode(
                null,
                this.worldPosition.getX() + 0.5D,
                this.worldPosition.getY() + 2.0D,
                this.worldPosition.getZ() + 0.5D,
                5.0F,
                Level.ExplosionInteraction.NONE
        );
        sync();
    }

    public record Port(BlockPos pos, Direction face) {
    }

    private final class BoilerFluidHandler implements IFluidHandler {
        private final BlockPos queriedPos;
        @Nullable
        private final Direction side;

        private BoilerFluidHandler(BlockPos queriedPos, @Nullable Direction side) {
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
            return fluid == configuredInput && HbmThermalConversions.firstBoilerStep(fluid).isPresent();
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !allowsPort(this.queriedPos, this.side)) {
                return 0;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            if (fluid != configuredInput || HbmThermalConversions.firstBoilerStep(fluid).isEmpty()) {
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
            if (fluid != outputTank.type()) {
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
