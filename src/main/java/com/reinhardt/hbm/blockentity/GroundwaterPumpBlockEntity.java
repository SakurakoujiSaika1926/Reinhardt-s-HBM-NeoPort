package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.GroundwaterPumpBlock;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidNetworks;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class GroundwaterPumpBlockEntity extends BlockEntity implements PowerEndpoint {
    public static final int GROUND_HEIGHT = 70;
    public static final int GROUND_DEPTH = 4;
    public static final int STEAM_SPEED = 1_000;
    public static final int ELECTRIC_SPEED = 10_000;
    public static final int STEAM_INPUT_PER_TICK = 100;
    public static final int SPENT_STEAM_PER_TICK = 1;
    public static final long ELECTRIC_POWER_PER_TICK = 1_000L;
    public static final long ELECTRIC_MAX_POWER = 10_000L;

    private static final int PUSH_PER_PORT = 16_000;
    private static final int STEAM_PULL_PER_PORT = 1_000;

    private final HbmFluidTank waterTank = new HbmFluidTank(water(), ELECTRIC_SPEED * 100);
    private final HbmFluidTank steamTank = new HbmFluidTank(steam(), 1_000);
    private final HbmFluidTank spentSteamTank = new HbmFluidTank(spentSteam(), 10);

    private long power;
    private long lastInput;
    private boolean active;
    private boolean onGround;
    private float rotor;
    private float lastRotor;
    private int groundCheckDelay;

    public GroundwaterPumpBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.GROUNDWATER_PUMP.get(), pos, blockState);
        configureTankCapacities();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, GroundwaterPumpBlockEntity pump) {
        if (level.isClientSide) {
            pump.tickClient(level);
            return;
        }
        pump.tickServer(level, state);
    }

    public GroundwaterPumpBlock.Kind kind() {
        BlockState state = getBlockState();
        if (state.is(HbmBlocks.PUMP_ELECTRIC.get())) {
            return GroundwaterPumpBlock.Kind.ELECTRIC;
        }
        return GroundwaterPumpBlock.Kind.STEAM;
    }

    public HbmFluidTank waterTank() {
        return this.waterTank;
    }

    public HbmFluidTank steamTank() {
        return this.steamTank;
    }

    public HbmFluidTank spentSteamTank() {
        return this.spentSteamTank;
    }

    public long power() {
        return this.power;
    }

    public long lastInput() {
        return this.lastInput;
    }

    public boolean active() {
        return this.active;
    }

    public boolean onGround() {
        return this.onGround;
    }

    public float rotor(float partialTick) {
        return this.lastRotor + (this.rotor - this.lastRotor) * partialTick;
    }

    @Nullable
    public IFluidHandler fluidHandler(BlockPos queriedPos, @Nullable Direction side) {
        if (!allowsPort(queriedPos, side)) {
            return null;
        }
        return new PumpFluidHandler(queriedPos.immutable(), side);
    }

    public List<Port> ports(LevelAccessor level) {
        return List.of(
                new Port(this.worldPosition.relative(Direction.EAST), Direction.EAST),
                new Port(this.worldPosition.relative(Direction.WEST), Direction.WEST),
                new Port(this.worldPosition.relative(Direction.SOUTH), Direction.SOUTH),
                new Port(this.worldPosition.relative(Direction.NORTH), Direction.NORTH)
        );
    }

    @Override
    public BlockPos getPowerPos() {
        return this.worldPosition;
    }

    @Override
    public List<BlockPos> getPowerConnectorPositions(LevelAccessor level) {
        if (kind() != GroundwaterPumpBlock.Kind.ELECTRIC) {
            return List.of();
        }
        List<BlockPos> connectors = new ArrayList<>(4);
        for (Port port : ports(level)) {
            connectors.add(port.pos().relative(port.face()).immutable());
        }
        return List.copyOf(connectors);
    }

    @Override
    public long getAvailableOutput() {
        return 0L;
    }

    @Override
    public long getRequestedInput() {
        if (kind() != GroundwaterPumpBlock.Kind.ELECTRIC || this.power >= ELECTRIC_MAX_POWER) {
            return 0L;
        }
        return ELECTRIC_MAX_POWER - this.power;
    }

    @Override
    public void applyPower(long usedOutput, long receivedInput) {
        this.power = Math.min(ELECTRIC_MAX_POWER, this.power + receivedInput);
        this.lastInput = receivedInput;
        setChanged();
    }

    @Override
    public Component getPowerStatus() {
        return Component.literal(String.format("%,d / %,d HE", this.power, ELECTRIC_MAX_POWER));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("WaterTank", this.waterTank.save());
        tag.put("SteamTank", this.steamTank.save());
        tag.put("SpentSteamTank", this.spentSteamTank.save());
        tag.putLong("Power", this.power);
        tag.putLong("LastInput", this.lastInput);
        tag.putBoolean("Active", this.active);
        tag.putBoolean("OnGround", this.onGround);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        configureTankCapacities();
        this.waterTank.load(tag.getCompound("WaterTank"));
        this.steamTank.load(tag.getCompound("SteamTank"));
        this.spentSteamTank.load(tag.getCompound("SpentSteamTank"));
        this.power = tag.getLong("Power");
        this.lastInput = tag.getLong("LastInput");
        this.active = tag.getBoolean("Active");
        this.onGround = tag.getBoolean("OnGround");
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

    private void tickServer(Level level, BlockState state) {
        configureTankCapacities();
        ensureTankTypes();

        if (kind() == GroundwaterPumpBlock.Kind.ELECTRIC) {
            PowerNetworkManager.tickFromEndpoint(level, this);
        } else {
            pullSteam(level);
            pushTank(level, this.spentSteamTank);
        }
        pushTank(level, this.waterTank);

        boolean oldGround = this.onGround;
        if (this.groundCheckDelay > 0) {
            this.groundCheckDelay--;
        } else {
            this.onGround = checkGround(level);
            this.groundCheckDelay = 20;
        }

        boolean oldActive = this.active;
        this.active = false;
        if (canOperate() && this.worldPosition.getY() <= GROUND_HEIGHT && this.onGround) {
            this.active = true;
            operate();
        }

        setChanged();
        if (oldActive != this.active || oldGround != this.onGround || level.getGameTime() % 10L == 0L) {
            sync();
        }
    }

    private void tickClient(Level level) {
        this.lastRotor = this.rotor;
        if (!this.active) {
            return;
        }

        this.rotor += 10.0F;
        if (this.rotor >= 360.0F) {
            this.rotor -= 360.0F;
            this.lastRotor -= 360.0F;
            level.playLocalSound(
                    this.worldPosition.getX(),
                    this.worldPosition.getY(),
                    this.worldPosition.getZ(),
                    HbmSoundEvents.STEAM_ENGINE_OPERATE.get(),
                    SoundSource.BLOCKS,
                    0.5F,
                    0.75F,
                    false
            );
            level.playLocalSound(
                    this.worldPosition.getX(),
                    this.worldPosition.getY(),
                    this.worldPosition.getZ(),
                    SoundEvents.GENERIC_SPLASH,
                    SoundSource.BLOCKS,
                    1.0F,
                    0.5F,
                    false
            );
        }
    }

    private boolean canOperate() {
        if (this.waterTank.amount() >= this.waterTank.capacity()) {
            return false;
        }
        if (kind() == GroundwaterPumpBlock.Kind.ELECTRIC) {
            return this.power >= ELECTRIC_POWER_PER_TICK;
        }
        return this.steamTank.amount() >= STEAM_INPUT_PER_TICK
                && this.spentSteamTank.capacity() - this.spentSteamTank.amount() >= SPENT_STEAM_PER_TICK;
    }

    private void operate() {
        if (kind() == GroundwaterPumpBlock.Kind.ELECTRIC) {
            this.power -= ELECTRIC_POWER_PER_TICK;
            this.waterTank.fill(water(), ELECTRIC_SPEED, false);
            return;
        }
        this.steamTank.drain(steam(), STEAM_INPUT_PER_TICK, false);
        this.spentSteamTank.fill(spentSteam(), SPENT_STEAM_PER_TICK, false);
        this.waterTank.fill(water(), STEAM_SPEED, false);
    }

    private void pullSteam(Level level) {
        if (this.steamTank.amount() >= this.steamTank.capacity()) {
            return;
        }
        for (Port port : ports(level)) {
            int space = this.steamTank.capacity() - this.steamTank.amount();
            if (space <= 0) {
                break;
            }
            FluidStack drained = HbmFluidNetworks.drainFrom(
                    level,
                    port.pos().relative(port.face()),
                    port.face().getOpposite(),
                    steam(),
                    Math.min(STEAM_PULL_PER_PORT, space),
                    this.worldPosition,
                    true
            );
            if (!drained.isEmpty()) {
                this.steamTank.fill(steam(), drained.getAmount(), false);
            }
        }
    }

    private void pushTank(Level level, HbmFluidTank tank) {
        if (tank.amount() <= 0 || tank.type().isNone()) {
            return;
        }
        for (Port port : ports(level)) {
            if (tank.amount() <= 0) {
                break;
            }
            int amount = Math.min(PUSH_PER_PORT, tank.amount());
            FluidStack stack = HbmFluids.toNeoStack(tank.type(), amount);
            int accepted = HbmFluidNetworks.fillInto(
                    level,
                    port.pos().relative(port.face()),
                    port.face().getOpposite(),
                    stack,
                    this.worldPosition,
                    true
            );
            if (accepted > 0) {
                tank.drain(tank.type(), accepted, false);
            }
        }
    }

    private boolean checkGround(Level level) {
        if (!level.dimensionType().hasSkyLight()) {
            return false;
        }

        int valid = 0;
        int invalid = 0;
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y >= -GROUND_DEPTH; y--) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos pos = this.worldPosition.offset(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (y == -1 && !state.isCollisionShapeFullBlock(level, pos)) {
                        return false;
                    }
                    if (isValidGroundBlock(state)) {
                        valid++;
                    } else {
                        invalid++;
                    }
                }
            }
        }
        return valid >= invalid;
    }

    private boolean isValidGroundBlock(BlockState state) {
        if (state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.DIRT)
                || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.ROOTED_DIRT)
                || state.is(Blocks.SAND)
                || state.is(Blocks.RED_SAND)
                || state.is(Blocks.MYCELIUM)) {
            return true;
        }

        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (!ReinhardtsHBM.MOD_ID.equals(id.getNamespace())) {
            return false;
        }
        return id.getPath().equals("waste_earth")
                || id.getPath().equals("dirt_dead")
                || id.getPath().equals("dirt_oily")
                || id.getPath().equals("sand_dirty")
                || id.getPath().equals("sand_dirty_red");
    }

    private void configureTankCapacities() {
        this.waterTank.setCapacity(kind() == GroundwaterPumpBlock.Kind.ELECTRIC ? ELECTRIC_SPEED * 100 : STEAM_SPEED * 100);
        this.steamTank.setCapacity(1_000);
        this.spentSteamTank.setCapacity(10);
    }

    private void ensureTankTypes() {
        if (this.waterTank.amount() == 0 && this.waterTank.type() != water()) {
            this.waterTank.setType(water());
        }
        if (this.steamTank.amount() == 0 && this.steamTank.type() != steam()) {
            this.steamTank.setType(steam());
        }
        if (this.spentSteamTank.amount() == 0 && this.spentSteamTank.type() != spentSteam()) {
            this.spentSteamTank.setType(spentSteam());
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
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
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

    private static HbmFluidDefinition spentSteam() {
        return HbmFluids.byName("spentsteam").orElse(HbmFluids.none());
    }

    public record Port(BlockPos pos, Direction face) {
    }

    private final class PumpFluidHandler implements IFluidHandler {
        private final BlockPos queriedPos;
        @Nullable
        private final Direction side;

        private PumpFluidHandler(BlockPos queriedPos, @Nullable Direction side) {
            this.queriedPos = queriedPos;
            this.side = side;
        }

        @Override
        public int getTanks() {
            return kind() == GroundwaterPumpBlock.Kind.STEAM ? 3 : 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            HbmFluidTank selected = tankForIndex(tank);
            return selected == null ? FluidStack.EMPTY : selected.getFluidInTank(0);
        }

        @Override
        public int getTankCapacity(int tank) {
            HbmFluidTank selected = tankForIndex(tank);
            return selected == null ? 0 : selected.capacity();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            if (kind() != GroundwaterPumpBlock.Kind.STEAM || tank != 0 || stack.isEmpty() || !allowsPort(this.queriedPos, this.side)) {
                return false;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(stack.getFluid()).orElse(HbmFluids.none());
            return fluid == steam();
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || kind() != GroundwaterPumpBlock.Kind.STEAM || !allowsPort(this.queriedPos, this.side)) {
                return 0;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            if (fluid != steam()) {
                return 0;
            }
            int filled = steamTank.fill(steam(), resource.getAmount(), action.simulate());
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
            if (fluid == water()) {
                return drainTank(waterTank, fluid, resource.getAmount(), action);
            }
            if (kind() == GroundwaterPumpBlock.Kind.STEAM && fluid == spentSteam()) {
                return drainTank(spentSteamTank, fluid, resource.getAmount(), action);
            }
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (maxDrain <= 0 || !allowsPort(this.queriedPos, this.side)) {
                return FluidStack.EMPTY;
            }
            if (waterTank.amount() > 0) {
                return drainTank(waterTank, water(), maxDrain, action);
            }
            if (kind() == GroundwaterPumpBlock.Kind.STEAM && spentSteamTank.amount() > 0) {
                return drainTank(spentSteamTank, spentSteam(), maxDrain, action);
            }
            return FluidStack.EMPTY;
        }

        @Nullable
        private HbmFluidTank tankForIndex(int tank) {
            if (kind() == GroundwaterPumpBlock.Kind.STEAM) {
                return switch (tank) {
                    case 0 -> steamTank;
                    case 1 -> spentSteamTank;
                    case 2 -> waterTank;
                    default -> null;
                };
            }
            return tank == 0 ? waterTank : null;
        }

        private FluidStack drainTank(HbmFluidTank tank, HbmFluidDefinition fluid, int amount, FluidAction action) {
            FluidStack drained = tank.drain(HbmFluids.toNeoStack(fluid, amount), action);
            if (!drained.isEmpty() && action.execute()) {
                sync();
            }
            return drained;
        }
    }
}
