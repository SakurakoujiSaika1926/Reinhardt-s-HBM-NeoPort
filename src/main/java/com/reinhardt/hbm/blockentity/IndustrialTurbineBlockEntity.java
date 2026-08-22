package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.client.sound.IndustrialTurbineClientSounds;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidNetworks;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.fluid.HbmThermalConversions;
import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.util.FluidCopiable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
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

public class IndustrialTurbineBlockEntity extends BlockEntity implements PowerEndpoint, FluidCopiable {
    public static final int BASE_INPUT_CAPACITY = 750_000;
    public static final int BASE_OUTPUT_CAPACITY = 3_000_000;
    private static final double EFFICIENCY = 1.0D;
    private static final double CONSUMPTION_PERCENT = 0.2D;
    private static final double FLYWHEEL_MAX_ENERGY = 50_000_000.0D;

    private final HbmFluidTank inputTank = new HbmFluidTank(steam(), BASE_INPUT_CAPACITY);
    private final HbmFluidTank outputTank = new HbmFluidTank(spentSteam(), BASE_OUTPUT_CAPACITY);
    private HbmFluidDefinition configuredInput = steam();
    private long powerBuffer;
    private long lastOutput;
    private long lastPowerTarget;
    private long maxPower;
    private long flywheelEnergy;
    private boolean operational;
    private double spin;
    private float rotor;
    private float lastRotor;

    public IndustrialTurbineBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.INDUSTRIAL_TURBINE.get(), pos, blockState);
        setupTanks();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, IndustrialTurbineBlockEntity turbine) {
        if (level.isClientSide) {
            turbine.tickClient();
            IndustrialTurbineClientSounds.tick(turbine);
            return;
        }
        turbine.tickServer(level);
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

    public long lastPowerTarget() {
        return this.lastPowerTarget;
    }

    public boolean operational() {
        return this.operational;
    }

    public double spin() {
        return this.spin;
    }

    public HbmFluidDefinition configuredInput() {
        return this.configuredInput;
    }

    public float rotor(float partialTick) {
        return this.lastRotor + (this.rotor - this.lastRotor) * partialTick;
    }

    public float gaugeAngle() {
        return switch (this.configuredInput.name()) {
            case "hotsteam" -> 45.0F;
            case "superhotsteam" -> -45.0F;
            case "ultrahotsteam" -> -135.0F;
            default -> 135.0F;
        };
    }

    public boolean cycleCompression(Player player) {
        if (this.operational) {
            player.sendSystemMessage(Component.translatable(
                    "message.reinhardtshbm.industrial_turbine.compressor_locked"
            ).withStyle(ChatFormatting.RED));
            return false;
        }

        this.configuredInput = nextInput(this.configuredInput);
        this.inputTank.setType(this.configuredInput);
        this.outputTank.setType(outputFor(this.configuredInput));
        setupTanks();
        sync();
        return true;
    }

    @Override
    public int[] getFluidIdsToCopy() {
        return new int[]{this.inputTank.type().oldId(), this.outputTank.type().oldId()};
    }

    @Override
    public void pasteFluidSetting(HbmFluidDefinition fluid, Level level, Player player, BlockPos pos) {
        if (this.operational) {
            player.sendSystemMessage(Component.translatable(
                    "message.reinhardtshbm.industrial_turbine.compressor_locked"
            ).withStyle(ChatFormatting.RED));
            return;
        }
        if (fluid == null || HbmThermalConversions.turbineStep(fluid).isEmpty()) {
            return;
        }
        this.configuredInput = fluid;
        this.inputTank.setType(fluid);
        this.outputTank.setType(outputFor(fluid));
        setupTanks();
        sync();
    }

    @Nullable
    public IFluidHandler fluidHandler(BlockPos queriedPos, @Nullable Direction side) {
        if (!allowsPort(queriedPos, side)) {
            return null;
        }
        return new IndustrialTurbineFluidHandler(queriedPos.immutable(), side);
    }

    @Nullable
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return fluidHandler(this.worldPosition, side);
    }

    public List<Port> ports(LevelAccessor level) {
        BlockState state = level.getBlockState(this.worldPosition);
        Direction facing = state.hasProperty(LargeMachineBlock.FACING) ? state.getValue(LargeMachineBlock.FACING) : Direction.NORTH;
        Direction right = facing.getClockWise();
        Direction left = facing.getCounterClockWise();
        BlockPos front = this.worldPosition.relative(facing, 3);
        BlockPos rear = this.worldPosition.relative(facing.getOpposite());
        return List.of(
                new Port(front.relative(right), right),
                new Port(front.relative(left), left),
                new Port(rear.relative(right), right),
                new Port(rear.relative(left), left),
                new Port(front.above(2), Direction.UP),
                new Port(rear.above(2), Direction.UP)
        );
    }

    @Override
    public BlockPos getPowerPos() {
        return this.worldPosition;
    }

    @Override
    public List<BlockPos> getPowerConnectorPositions(LevelAccessor level) {
        BlockState state = level.getBlockState(this.worldPosition);
        Direction facing = state.hasProperty(LargeMachineBlock.FACING) ? state.getValue(LargeMachineBlock.FACING) : Direction.NORTH;
        return List.of(this.worldPosition.relative(facing.getOpposite(), 4).above().immutable());
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
        return Component.translatable(
                "message.reinhardtshbm.power.industrial_turbine",
                this.lastOutput,
                this.powerBuffer,
                Math.round(this.spin * 100.0D)
        );
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("InputTank", this.inputTank.save());
        tag.put("OutputTank", this.outputTank.save());
        tag.putString("ConfiguredInput", this.configuredInput.name());
        tag.putLong("PowerBuffer", this.powerBuffer);
        tag.putLong("LastOutput", this.lastOutput);
        tag.putLong("LastPowerTarget", this.lastPowerTarget);
        tag.putBoolean("Operational", this.operational);
        tag.putDouble("Spin", this.spin);
        tag.putFloat("Rotor", this.rotor);
        tag.putFloat("LastRotor", this.lastRotor);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.configuredInput = HbmFluids.byName(tag.getString("ConfiguredInput")).orElse(steam());
        setupTanks();
        this.inputTank.load(tag.getCompound("InputTank"));
        this.outputTank.load(tag.getCompound("OutputTank"));
        setupTanks();
        this.powerBuffer = tag.getLong("PowerBuffer");
        this.lastOutput = tag.getLong("LastOutput");
        this.lastPowerTarget = tag.getLong("LastPowerTarget");
        this.operational = tag.getBoolean("Operational");
        this.spin = Math.max(0.0D, Math.min(1.0D, tag.getDouble("Spin")));
        this.rotor = tag.getFloat("Rotor");
        this.lastRotor = tag.getFloat("LastRotor");
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
        boolean oldOperational = this.operational;
        long oldPower = this.powerBuffer;
        double oldSpin = this.spin;

        tryConvert();
        pullInput(level);
        sendOutput(level);
        PowerNetworkManager.tickFromEndpoint(level, this);

        setChanged();
        if (oldOperational != this.operational
                || oldPower != this.powerBuffer
                || Math.abs(oldSpin - this.spin) > 0.005D
                || level.getGameTime() % 10L == 0L) {
            sync();
        }
    }

    private void tickClient() {
        this.lastRotor = this.rotor;
        if (this.spin <= 0.0D) {
            return;
        }

        float speed = this.spin >= 0.5D
                ? 30.0F
                : (float) (Math.pow(this.spin * 2.0D, 0.5D) * 30.0D);
        this.rotor += speed;
        if (this.rotor >= 360.0F) {
            this.rotor -= 360.0F;
            this.lastRotor -= 360.0F;
        }
    }

    private void tryConvert() {
        this.powerBuffer = 0L;
        this.operational = false;

        HbmThermalConversions.turbineStep(this.configuredInput).ifPresent(step -> {
            int availableInput = Math.min((int) Math.ceil(this.inputTank.amount() * CONSUMPTION_PERCENT), this.inputTank.amount());
            int inputOps = availableInput / step.amountReq();
            int outputOps = (this.outputTank.capacity() - this.outputTank.amount()) / step.amountProduced();
            int ops = Math.min(inputOps, outputOps);
            if (ops <= 0) {
                return;
            }

            int consumed = ops * step.amountReq();
            this.inputTank.drain(step.input(), consumed, false);
            this.outputTank.fill(step.output(), ops * step.amountProduced(), false);
            generatePower(step, ops);
            this.operational = true;
        });
        updateFlywheel();
    }

    private void generatePower(HbmThermalConversions.CoolingStep step, int ops) {
        int maxOps = (int) Math.ceil((this.inputTank.capacity() * CONSUMPTION_PERCENT) / step.amountReq());
        if (maxOps <= 0) {
            this.maxPower = 0L;
            this.lastPowerTarget = 0L;
            return;
        }
        this.maxPower = (long) (maxOps * step.heatEnergy() * step.turbineEfficiency() * EFFICIENCY);
        this.flywheelEnergy += (long) (ops * step.heatEnergy() * step.turbineEfficiency() * EFFICIENCY);
    }

    private void updateFlywheel() {
        this.spin = this.flywheelEnergy / FLYWHEEL_MAX_ENERGY;
        this.lastPowerTarget = Math.min((long) (Math.max(this.spin, 0.05D) * this.maxPower), this.flywheelEnergy);
        this.flywheelEnergy -= this.lastPowerTarget;
        this.powerBuffer = this.lastPowerTarget;
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
            FluidStack drained = HbmFluidNetworks.drainFrom(
                    level,
                    port.connectorPos(),
                    port.face().getOpposite(),
                    this.configuredInput,
                    space,
                    this.worldPosition,
                    true
            );
            if (!drained.isEmpty()) {
                this.inputTank.fill(this.configuredInput, drained.getAmount(), false);
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

    private void setupTanks() {
        HbmThermalConversions.turbineStep(this.configuredInput).ifPresentOrElse(step -> {
            this.inputTank.setCapacity(inputCapacity(this.configuredInput));
            this.outputTank.setCapacity(outputCapacity(this.configuredInput));
            if (this.inputTank.amount() == 0 && this.inputTank.type() != step.input()) {
                this.inputTank.setType(step.input());
            }
            if (this.outputTank.amount() == 0 && this.outputTank.type() != step.output()) {
                this.outputTank.setType(step.output());
            }
        }, () -> {
            this.configuredInput = steam();
            this.inputTank.clear();
            this.outputTank.clear();
            this.inputTank.setCapacity(BASE_INPUT_CAPACITY);
            this.outputTank.setCapacity(BASE_OUTPUT_CAPACITY);
            this.inputTank.setType(steam());
            this.outputTank.setType(spentSteam());
        });
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

    private static HbmFluidDefinition nextInput(HbmFluidDefinition current) {
        return switch (current.name()) {
            case "steam" -> hotSteam();
            case "hotsteam" -> superHotSteam();
            case "superhotsteam" -> ultraHotSteam();
            default -> steam();
        };
    }

    private static HbmFluidDefinition outputFor(HbmFluidDefinition input) {
        return HbmThermalConversions.turbineStep(input)
                .map(HbmThermalConversions.CoolingStep::output)
                .orElse(spentSteam());
    }

    private static int inputCapacity(HbmFluidDefinition input) {
        return switch (input.name()) {
            case "hotsteam" -> BASE_INPUT_CAPACITY / 10;
            case "superhotsteam" -> BASE_INPUT_CAPACITY / 100;
            case "ultrahotsteam" -> BASE_INPUT_CAPACITY / 1000;
            default -> BASE_INPUT_CAPACITY;
        };
    }

    private static int outputCapacity(HbmFluidDefinition input) {
        return switch (input.name()) {
            case "hotsteam" -> BASE_OUTPUT_CAPACITY / 10;
            case "superhotsteam" -> BASE_OUTPUT_CAPACITY / 100;
            case "ultrahotsteam" -> BASE_OUTPUT_CAPACITY / 1000;
            default -> BASE_OUTPUT_CAPACITY;
        };
    }

    private static HbmFluidDefinition steam() {
        return HbmFluids.byName("steam").orElse(HbmFluids.none());
    }

    private static HbmFluidDefinition hotSteam() {
        return HbmFluids.byName("hotsteam").orElse(steam());
    }

    private static HbmFluidDefinition superHotSteam() {
        return HbmFluids.byName("superhotsteam").orElse(steam());
    }

    private static HbmFluidDefinition ultraHotSteam() {
        return HbmFluids.byName("ultrahotsteam").orElse(steam());
    }

    private static HbmFluidDefinition spentSteam() {
        return HbmFluids.byName("spentsteam").orElse(HbmFluids.none());
    }

    public record Port(BlockPos pos, Direction face) {
        public BlockPos connectorPos() {
            return this.pos.relative(this.face);
        }
    }

    private final class IndustrialTurbineFluidHandler implements IFluidHandler {
        private final BlockPos queriedPos;
        @Nullable
        private final Direction side;

        private IndustrialTurbineFluidHandler(BlockPos queriedPos, @Nullable Direction side) {
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
            return fluid == configuredInput && HbmThermalConversions.turbineStep(fluid).isPresent();
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !allowsPort(this.queriedPos, this.side)) {
                return 0;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            if (fluid != configuredInput || HbmThermalConversions.turbineStep(fluid).isEmpty()) {
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
