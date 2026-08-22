package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.client.sound.GeothermalHeatExchangerClientSounds;
import com.reinhardt.hbm.fluid.HbmFluidNetworks;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.fluid.HbmThermalConversions;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class GeothermalHeatExchangerBlockEntity extends BlockEntity {
    public static final int TANK_CAPACITY = 24_000;
    private static final int PORT_TRANSFER = 24_000;
    private final HbmFluidTank input = new HbmFluidTank(oil(), TANK_CAPACITY);
    private final HbmFluidTank output = new HbmFluidTank(hotOil(), TANK_CAPACITY);
    private HbmFluidDefinition configuredInput = oil();
    private final int[] heatLayers = new int[10];
    private long fissureScanTime = Long.MIN_VALUE;
    private int bufferedHeat;
    private boolean active;
    private float rotor;
    private float previousRotor;

    public GeothermalHeatExchangerBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.GEOTHERMAL_HEAT_EXCHANGER.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, GeothermalHeatExchangerBlockEntity exchanger) {
        if (level.isClientSide) exchanger.tickClient(level);
        else exchanger.tickServer(level);
    }

    public HbmFluidTank inputTank() { return input; }
    public HbmFluidTank outputTank() { return output; }
    public int bufferedHeat() { return bufferedHeat; }
    public boolean active() { return active; }
    public float rotor(float partialTick) { return previousRotor + (rotor - previousRotor) * partialTick; }

    public boolean applyFluidIdentifier(HbmFluidDefinition type) {
        if (HbmThermalConversions.firstGeothermalStep(type).isEmpty()) return false;
        configuredInput = type;
        input.setType(configuredInput);
        setupOutput();
        setChanged();
        sync();
        return true;
    }

    public List<Port> ports(LevelAccessor level) { return portsFor(worldPosition); }

    public static List<Port> portsFor(BlockPos core) {
        return List.of(
                new Port(core.offset(1, 0, 0), Direction.EAST),
                new Port(core.offset(-1, 0, 0), Direction.WEST),
                new Port(core.offset(0, 0, 1), Direction.SOUTH),
                new Port(core.offset(0, 0, -1), Direction.NORTH),
                new Port(core.offset(1, 11, 0), Direction.EAST),
                new Port(core.offset(-1, 11, 0), Direction.WEST),
                new Port(core.offset(0, 11, 1), Direction.SOUTH),
                new Port(core.offset(0, 11, -1), Direction.NORTH)
        );
    }

    @Nullable
    public IFluidHandler fluidHandler(BlockPos queriedPos, @Nullable Direction side) {
        if (!allowsPort(queriedPos, side)) return null;
        return new GeothermalFluidHandler(queriedPos.immutable(), side);
    }

    private void tickServer(Level level) {
        int layer = (int) (level.getGameTime() % 10L);
        heatLayers[layer] = scanLayer(level, layer);
        bufferedHeat = totalHeat(level);
        setupOutput();
        pullInput(level);
        heatFluid();
        pushOutput(level);
        setupOutput();
        setChanged();
        if (level.getGameTime() % 10L == 0L) sync();
    }

    private void tickClient(Level level) {
        GeothermalHeatExchangerClientSounds.tick(this);
        previousRotor = rotor;
        if (bufferedHeat <= 0) return;
        rotor += 0.5F;
        if (rotor >= 360.0F) { rotor -= 360.0F; previousRotor -= 360.0F; }
        if (level.random.nextInt(7) == 0) {
            level.addParticle(net.minecraft.core.particles.ParticleTypes.CLOUD,
                    worldPosition.getX() + 0.5D + level.random.nextGaussian() * 2.0D,
                    worldPosition.getY() + 6.0D + level.random.nextGaussian() * 3.0D,
                    worldPosition.getZ() + 0.5D + level.random.nextGaussian() * 2.0D,
                    0.0D, 0.0D, 0.0D);
        }
    }

    private int scanLayer(Level level, int layer) {
        int y = worldPosition.getY() - 1 - layer;
        if (y < level.getMinBuildHeight()) return 0;
        int heat = 0;
        for (int x = -7; x <= 7; x++) {
            for (int z = -7; z <= 7; z++) {
                BlockState state = level.getBlockState(new BlockPos(worldPosition.getX() + x, y, worldPosition.getZ() + z));
                if (state.is(Blocks.LAVA)) heat += 5;
                else if (state.is(HbmBlocks.VOLCANIC_LAVA_BLOCK.get())) heat += 150;
                else if (state.is(HbmBlocks.ORE_VOLCANO.get())) { heat += 300; fissureScanTime = level.getGameTime(); }
            }
        }
        return heat;
    }

    private int totalHeat(Level level) {
        int total = 0;
        for (int layerHeat : heatLayers) total += layerHeat;
        if (level.getGameTime() - fissureScanTime < 20L) total *= 3;
        return total;
    }

    private void setupOutput() {
        if (input.amount() == 0 && input.type() != configuredInput) input.setType(configuredInput);
        HbmThermalConversions.firstGeothermalStep(configuredInput).ifPresent(step -> {
            if (output.amount() == 0 && output.type() != step.output()) output.setType(step.output());
        });
    }

    private void heatFluid() {
        active = false;
        int availableHeat = bufferedHeat;
        HbmThermalConversions.firstGeothermalStep(input.type()).ifPresent(step -> {
            int operations = Math.min(input.amount() / step.amountReq(),
                    Math.min((output.capacity() - output.amount()) / step.amountProduced(), availableHeat / step.heatReq()));
            if (operations <= 0) return;
            input.drain(step.input(), operations * step.amountReq(), false);
            output.fill(step.output(), operations * step.amountProduced(), false);
            active = true;
        });
    }

    private void pullInput(Level level) {
        int room = input.capacity() - input.amount();
        if (room <= 0 || configuredInput.isNone() || HbmThermalConversions.firstGeothermalStep(configuredInput).isEmpty()) return;
        for (Port port : ports(level)) {
            if (room <= 0) break;
            FluidStack drained = HbmFluidNetworks.drainFrom(level, port.connectorPos(), port.face().getOpposite(), configuredInput,
                    Math.min(PORT_TRANSFER, room), worldPosition, true);
            if (!drained.isEmpty()) {
                input.fill(configuredInput, drained.getAmount(), false);
                room = input.capacity() - input.amount();
            }
        }
    }

    private void pushOutput(Level level) {
        if (output.amount() <= 0 || output.type().isNone()) return;
        for (Port port : ports(level)) {
            if (output.amount() <= 0) break;
            FluidStack stack = HbmFluids.toNeoStack(output.type(), Math.min(PORT_TRANSFER, output.amount()));
            int accepted = HbmFluidNetworks.fillInto(level, port.connectorPos(), port.face().getOpposite(), stack, worldPosition, true);
            if (accepted > 0) output.drain(output.type(), accepted, false);
        }
    }

    private boolean allowsPort(BlockPos queriedPos, @Nullable Direction side) {
        for (Port port : portsFor(worldPosition)) {
            if (port.pos().equals(queriedPos) && (side == null || side == port.face())) return true;
        }
        return false;
    }

    private void sync() {
        if (level == null || level.isClientSide) return;
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        for (Port port : ports(level)) level.invalidateCapabilities(port.pos());
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Input", input.save()); tag.put("Output", output.save()); tag.putIntArray("HeatLayers", heatLayers);
        tag.putString("ConfiguredInput", configuredInput.name());
        tag.putLong("FissureScanTime", fissureScanTime); tag.putInt("BufferedHeat", bufferedHeat); tag.putBoolean("Active", active);
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        input.load(tag.getCompound("Input")); output.load(tag.getCompound("Output"));
        configuredInput = HbmFluids.byName(tag.getString("ConfiguredInput")).orElse(oil());
        if (HbmThermalConversions.firstGeothermalStep(configuredInput).isEmpty()) configuredInput = oil();
        setupOutput();
        int[] saved = tag.getIntArray("HeatLayers"); System.arraycopy(saved, 0, heatLayers, 0, Math.min(saved.length, heatLayers.length));
        fissureScanTime = tag.getLong("FissureScanTime"); bufferedHeat = tag.getInt("BufferedHeat"); active = tag.getBoolean("Active");
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { CompoundTag tag = super.getUpdateTag(registries); saveAdditional(tag, registries); return tag; }
    @Nullable @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

    private static HbmFluidDefinition oil() { return HbmFluids.byName("oil").orElse(HbmFluids.none()); }
    private static HbmFluidDefinition hotOil() { return HbmFluids.byName("hotoil").orElse(HbmFluids.none()); }

    public record Port(BlockPos pos, Direction face) { public BlockPos connectorPos() { return pos.relative(face); } }

    private final class GeothermalFluidHandler implements IFluidHandler {
        private final BlockPos queriedPos; @Nullable private final Direction side;
        private GeothermalFluidHandler(BlockPos queriedPos, @Nullable Direction side) { this.queriedPos = queriedPos; this.side = side; }
        @Override public int getTanks() { return 2; }
        @Override public FluidStack getFluidInTank(int tank) { return tank == 0 ? input.getFluidInTank(0) : tank == 1 ? output.getFluidInTank(0) : FluidStack.EMPTY; }
        @Override public int getTankCapacity(int tank) { return tank == 0 ? input.capacity() : tank == 1 ? output.capacity() : 0; }
        @Override public boolean isFluidValid(int tank, FluidStack stack) { return tank == 0 && !stack.isEmpty() && HbmFluids.fromNeoFluid(stack.getFluid()).flatMap(HbmThermalConversions::firstGeothermalStep).isPresent(); }
        @Override public int fill(FluidStack stack, FluidAction action) {
            if (!allowsPort(queriedPos, side) || stack.isEmpty()) return 0;
            HbmFluidDefinition type = HbmFluids.fromNeoFluid(stack.getFluid()).orElse(HbmFluids.none());
            if (type != configuredInput || HbmThermalConversions.firstGeothermalStep(type).isEmpty()) return 0;
            int accepted = input.fill(type, stack.getAmount(), action.simulate()); if (!action.simulate() && accepted > 0) { setupOutput(); setChanged(); } return accepted;
        }
        @Override public FluidStack drain(FluidStack stack, FluidAction action) {
            if (stack.isEmpty()) return FluidStack.EMPTY; HbmFluidDefinition type = HbmFluids.fromNeoFluid(stack.getFluid()).orElse(HbmFluids.none());
            if (type != output.type()) return FluidStack.EMPTY; return drain(stack.getAmount(), action);
        }
        @Override public FluidStack drain(int amount, FluidAction action) {
            var drained = output.drain(output.type(), amount, action.simulate()); if (!action.simulate() && !drained.isEmpty()) setChanged();
            return drained.isEmpty() ? FluidStack.EMPTY : HbmFluids.toNeoStack(drained.type(), drained.amount());
        }
    }
}
