package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.client.sound.LeviathanTurbineClientSounds;
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
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.RandomSource;
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

public class LeviathanTurbineBlockEntity extends BlockEntity implements PowerEndpoint, FluidCopiable {
    public static final int INPUT_CAPACITY = 1_000_000_000;
    public static final int OUTPUT_CAPACITY = 1_000_000_000;
    private static final double EFFICIENCY = 0.85D;
    private static final double CONSUMPTION_PERCENT = 1.0D;
    private static final int MAX_TRANSFER_PER_PORT = 1_000_000_000;

    private final HbmFluidTank inputTank = new HbmFluidTank(steam(), INPUT_CAPACITY);
    private final HbmFluidTank outputTank = new HbmFluidTank(spentSteam(), OUTPUT_CAPACITY);
    private HbmFluidDefinition configuredInput = steam();
    private long powerBuffer;
    private long lastOutput;
    private boolean operational;
    private int turnTimer;
    private float rotor;
    private float lastRotor;
    private float fanAcceleration;
    private final float audioDesync;

    public LeviathanTurbineBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.LEVIATHAN_TURBINE.get(), pos, blockState);
        this.audioDesync = Math.floorMod(pos.hashCode(), 50) / 1000.0F;
        setupTanks();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, LeviathanTurbineBlockEntity turbine) {
        if (level.isClientSide) {
            turbine.tickClient(level, state);
            LeviathanTurbineClientSounds.tick(turbine);
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

    public boolean operational() {
        return this.operational;
    }

    public HbmFluidDefinition configuredInput() {
        return this.configuredInput;
    }

    public float rotor(float partialTick) {
        return this.lastRotor + (this.rotor - this.lastRotor) * partialTick;
    }

    public float fanAcceleration() {
        return this.fanAcceleration;
    }

    public float leverAngle() {
        return switch (this.configuredInput.name()) {
            case "hotsteam" -> 5.0F;
            case "superhotsteam" -> -5.0F;
            case "ultrahotsteam" -> -15.0F;
            default -> 15.0F;
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

    public boolean isLeverPart(BlockPos clickedPos) {
        Direction facing = facing();
        Direction turn = facing.getCounterClockWise();
        BlockPos first = this.worldPosition.relative(facing).relative(turn, 2);
        BlockPos second = this.worldPosition.relative(facing, 2).relative(turn, 2);
        return (clickedPos.equals(first) || clickedPos.equals(second))
                && clickedPos.getY() < this.worldPosition.getY() + 2;
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
        return new LeviathanFluidHandler(queriedPos.immutable(), side);
    }

    @Nullable
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return fluidHandler(this.worldPosition, side);
    }

    public List<Port> ports(LevelAccessor level) {
        Direction facing = facing(level);
        Direction right = facing.getClockWise();
        Direction left = facing.getCounterClockWise();
        return List.of(
                new Port(this.worldPosition.relative(facing, 4).above(2), facing),
                new Port(this.worldPosition.relative(right, 2), right),
                new Port(this.worldPosition.relative(left, 2), left)
        );
    }

    @Override
    public BlockPos getPowerPos() {
        return this.worldPosition;
    }

    @Override
    public List<BlockPos> getPowerConnectorPositions(LevelAccessor level) {
        Direction facing = facing(level);
        return List.of(powerConnectorPosition(facing).immutable());
    }

    @Override
    public boolean canConnectPower(LevelAccessor level, BlockPos connectorPos, Direction machineSide) {
        Direction facing = facing(level);
        return connectorPos.equals(powerConnectorPosition(facing)) && machineSide == facing.getOpposite();
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
                "message.reinhardtshbm.power.leviathan_turbine",
                this.lastOutput,
                this.powerBuffer
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
        tag.putBoolean("Operational", this.operational);
        tag.putInt("TurnTimer", this.turnTimer);
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
        this.operational = tag.getBoolean("Operational");
        this.turnTimer = tag.getInt("TurnTimer");
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
        HbmFluidDefinition oldInput = this.inputTank.type();
        HbmFluidDefinition oldOutput = this.outputTank.type();
        int oldInputAmount = this.inputTank.amount();
        int oldOutputAmount = this.outputTank.amount();

        tryConvert();
        pullInput(level);
        sendOutput(level);
        PowerNetworkManager.tickFromEndpoint(level, this);

        this.turnTimer--;
        if (this.operational) {
            this.turnTimer = 25;
        }

        setChanged();
        if (oldOperational != this.operational
                || oldPower != this.powerBuffer
                || oldInput != this.inputTank.type()
                || oldOutput != this.outputTank.type()
                || Math.abs(oldInputAmount - this.inputTank.amount()) > 1000
                || Math.abs(oldOutputAmount - this.outputTank.amount()) > 1000
                || level.getGameTime() % 10L == 0L) {
            sync();
        }
    }

    private void tickClient(Level level, BlockState state) {
        this.lastRotor = this.rotor;
        this.rotor += this.fanAcceleration;
        if (this.rotor >= 360.0F) {
            this.rotor -= 360.0F;
            this.lastRotor -= 360.0F;
        }

        if (this.turnTimer > 0) {
            this.fanAcceleration = Math.max(0.0F, Math.min(25.0F, this.fanAcceleration + 0.075F + this.audioDesync));
            spawnSteamClouds(level, state);
        } else {
            this.fanAcceleration = Math.max(0.0F, Math.min(25.0F, this.fanAcceleration - 0.1F));
        }
        this.turnTimer--;
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
            long convertedHeat = (long) ops * step.heatEnergy();
            this.powerBuffer = (long) (convertedHeat * step.turbineEfficiency() * EFFICIENCY);
            this.operational = true;
        });

        if (!this.operational && !HbmThermalConversions.turbineStep(this.configuredInput).isPresent()) {
            this.outputTank.setType(HbmFluids.none());
        }
    }

    private void pullInput(Level level) {
        int space = this.inputTank.capacity() - this.inputTank.amount();
        if (space <= 0 || this.configuredInput.isNone()) {
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
                    Math.min(MAX_TRANSFER_PER_PORT, space),
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
            int amount = Math.min(MAX_TRANSFER_PER_PORT, this.outputTank.amount());
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

    private void setupTanks() {
        this.inputTank.setCapacity(INPUT_CAPACITY);
        this.outputTank.setCapacity(OUTPUT_CAPACITY);
        HbmThermalConversions.turbineStep(this.configuredInput).ifPresentOrElse(step -> {
            if (this.inputTank.amount() == 0 && this.inputTank.type() != step.input()) {
                this.inputTank.setType(step.input());
            }
            if (this.outputTank.amount() == 0 && this.outputTank.type() != step.output()) {
                this.outputTank.setType(step.output());
            }
        }, () -> {
            this.configuredInput = steam();
            if (this.inputTank.amount() == 0) {
                this.inputTank.setType(steam());
            }
            if (this.outputTank.amount() == 0) {
                this.outputTank.setType(spentSteam());
            }
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

    private Direction facing() {
        return facing(this.level == null ? null : this.level);
    }

    private Direction facing(@Nullable LevelAccessor level) {
        BlockState state = level == null ? getBlockState() : level.getBlockState(this.worldPosition);
        return state.hasProperty(LargeMachineBlock.FACING) ? state.getValue(LargeMachineBlock.FACING) : Direction.NORTH;
    }

    private BlockPos powerConnectorPosition(Direction facing) {
        return this.worldPosition.relative(facing.getOpposite(), 11);
    }

    private void spawnSteamClouds(Level level, BlockState state) {
        Direction facing = state.hasProperty(LargeMachineBlock.FACING) ? state.getValue(LargeMachineBlock.FACING) : Direction.NORTH;
        Direction side = facing.getClockWise();
        RandomSource random = level.random;
        for (int i = 0; i < 10; i++) {
            level.addParticle(
                    ParticleTypes.CLOUD,
                    this.worldPosition.getX() + 0.5D + facing.getStepX() * (random.nextDouble() + 1.25D) + random.nextGaussian() * side.getStepX() * 0.65D,
                    this.worldPosition.getY() + 2.5D + random.nextGaussian() * 0.65D,
                    this.worldPosition.getZ() + 0.5D + facing.getStepZ() * (random.nextDouble() + 1.25D) + random.nextGaussian() * side.getStepZ() * 0.65D,
                    -facing.getStepX() * 0.2D,
                    0.0D,
                    -facing.getStepZ() * 0.2D
            );
        }
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

    private final class LeviathanFluidHandler implements IFluidHandler {
        private final BlockPos queriedPos;
        @Nullable
        private final Direction side;

        private LeviathanFluidHandler(BlockPos queriedPos, @Nullable Direction side) {
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
