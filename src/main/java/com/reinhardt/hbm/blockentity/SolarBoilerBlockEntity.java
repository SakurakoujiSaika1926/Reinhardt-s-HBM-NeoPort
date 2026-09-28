package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.client.SolarBoilerClientHooks;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidNetworks;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.registry.HbmBlockEntities;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.fml.loading.FMLEnvironment;
import org.joml.AxisAngle4f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class SolarBoilerBlockEntity extends BlockEntity {
    // TileEntitySolarBoiler in 1.7.10 stores only one bucket tenth of water
    // and ten buckets of steam. Mirror heat is an unbuffered tick value.
    public static final int INPUT_CAPACITY = 100;
    public static final int OUTPUT_CAPACITY = 10_000;

    private final HbmFluidTank inputTank = new HbmFluidTank(water(), INPUT_CAPACITY);
    private final HbmFluidTank outputTank = new HbmFluidTank(steam(), OUTPUT_CAPACITY);
    private int heat;
    private int heatDisplay;
    // Matches TileEntitySolarBoiler's delayed client-side mirror queue.
    private final Set<BlockPos> primaryMirrors = new LinkedHashSet<>();
    private volatile RenderSnapshot renderSnapshot;

    public SolarBoilerBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.SOLAR_BOILER.get(), pos, blockState);
        // Bootstrap with the same proved face-used static model envelope used by
        // client snapshots.  Beam geometry is added only after the client publishes
        // its immutable mirror set; the owner model can therefore never be clipped
        // before the first client tick.
        this.renderSnapshot = new RenderSnapshot(
                blockState, facing(blockState), false, Set.of(), List.of(),
                modelBounds(pos, facing(blockState)));
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SolarBoilerBlockEntity boiler) {
        if (level.isClientSide) {
            if (FMLEnvironment.dist.isClient()) {
                SolarBoilerClientHooks.tick(boiler);
            }
            return;
        }
        boiler.tickServer(level);
    }

    public HbmFluidTank inputTank() {
        return this.inputTank;
    }

    public HbmFluidTank outputTank() {
        return this.outputTank;
    }

    public int heat() {
        return this.heatDisplay;
    }

    public int heatInput() {
        return 0;
    }

    public void addHeatInput(int amount) {
        if (amount > 0) {
            this.heat += amount;
        }
    }

    public void registerActiveMirror(BlockPos mirrorPos) {
        if (this.level != null && this.level.isClientSide) {
            this.primaryMirrors.add(mirrorPos.immutable());
        }
    }

    public Set<BlockPos> activeMirrors() {
        return this.renderSnapshot.activeMirrors();
    }

    public RenderSnapshot renderSnapshot() {
        return this.renderSnapshot;
    }

    @Nullable
    public IFluidHandler fluidHandler(BlockPos queriedPos, @Nullable Direction side) {
        if (!allowsPort(queriedPos, side)) {
            return null;
        }
        return new SolarBoilerFluidHandler(queriedPos.immutable(), side);
    }

    @Nullable
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return fluidHandler(this.worldPosition, side);
    }

    public List<Port> ports(LevelAccessor level) {
        return portsFor(this.worldPosition);
    }

    public static List<Port> portsFor(BlockPos corePos) {
        return List.of(
                new Port(corePos, Direction.DOWN),
                new Port(corePos.above(2), Direction.UP)
        );
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("InputTank", this.inputTank.save());
        tag.put("OutputTank", this.outputTank.save());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.inputTank.load(tag.getCompound("InputTank"));
        this.outputTank.load(tag.getCompound("OutputTank"));
        // Legacy heat is transient and deliberately not persisted.
        this.heat = 0;
        this.heatDisplay = 0;
        ensureTankTypes();
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        tag.putInt("HeatDisplay", this.heatDisplay);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void tickServer(Level level) {
        ensureTankTypes();
        pullInput(level);
        tryConvert();
        pushOutput(level);

        setChanged();
        if (level.getGameTime() % 10L == 0L) {
            sync();
        }
    }

    public void tickClient(boolean beamsEnabled) {
        Set<BlockPos> nextMirrors = Collections.unmodifiableSet(new LinkedHashSet<>(this.primaryMirrors));
        this.primaryMirrors.clear();
        BlockState state = this.getBlockState();
        RenderSnapshot previous = this.renderSnapshot;
        if (previous.blockState() == state
                && previous.beamsEnabled() == beamsEnabled
                && previous.activeMirrors().equals(nextMirrors)) {
            return;
        }

        RenderSnapshot next = buildClientRenderSnapshot(state, beamsEnabled, nextMirrors);
        this.renderSnapshot = next;
        if (this.level != null && !sameSectionCoverage(previous.bounds(), next.bounds())) {
            this.level.sendBlockUpdated(
                    this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private RenderSnapshot buildClientRenderSnapshot(
            BlockState state, boolean beamsEnabled, Set<BlockPos> mirrors) {
        Direction direction = facing(state);
        AABB bounds = modelBounds(this.worldPosition, direction);
        List<BeamVertex> beamVertices = List.of();
        if (beamsEnabled) {
            ArrayList<BeamVertex> builtVertices = new ArrayList<>();
            int rendered = 0;
            for (BlockPos mirrorPos : mirrors) {
                if (rendered++ >= 64) {
                    break;
                }
                int firstVertex = builtVertices.size();
                appendMirrorBeam(this.worldPosition, mirrorPos, builtVertices);
                for (int vertexIndex = firstVertex; vertexIndex < builtVertices.size(); vertexIndex++) {
                    BeamVertex vertex = builtVertices.get(vertexIndex);
                    double worldX = this.worldPosition.getX() + vertex.x();
                    double worldY = this.worldPosition.getY() + vertex.y();
                    double worldZ = this.worldPosition.getZ() + vertex.z();
                    bounds = bounds.minmax(new AABB(
                            worldX, worldY, worldZ,
                            worldX, worldY, worldZ));
                }
            }
            beamVertices = List.copyOf(builtVertices);
        }
        return new RenderSnapshot(state, direction, beamsEnabled, mirrors, beamVertices, bounds);
    }

    private static AABB modelBounds(BlockPos pos, Direction direction) {
        Matrix4f transform = new Matrix4f()
                .translation(0.5F, 0.0F, 0.5F);
        applySolarBoilerBoundsPose(transform, direction);
        // RenderSolarBoiler renders only Base.  These are the exact face-used
        // extrema of that group in solar_boiler.obj; Overglow_1..3 are not
        // world geometry and must not enlarge the culling envelope.
        return transformedBounds(pos, transform,
                -1.5F, 0.0F, -1.5F,
                1.5F, 3.0F, 1.5F);
    }

    private static void appendMirrorBeam(
            BlockPos boilerPos, BlockPos mirrorPos, List<BeamVertex> output) {
        int dx = boilerPos.getX() - mirrorPos.getX();
        int dy = boilerPos.getY() - mirrorPos.getY();
        int dz = boilerPos.getZ() - mirrorPos.getZ();
        double distance = Math.sqrt(dx * (double) dx + dy * (double) dy + dz * (double) dz);
        if (distance <= 0.0001D) {
            return;
        }

        float pitch = (float) (Math.toDegrees(-Math.asin((dy + 0.5D) / distance)) + 90.0D);
        float yaw = (float) (Math.toDegrees(-Math.atan2(dz, dx)) + 180.0D);
        Matrix4f transform = new Matrix4f()
                .translation(-dx, -dy, -dz)
                .translate(0.0F, 1.0F, 0.0F)
                .rotate(yawQuaternion(yaw))
                .rotate(new Quaternionf(new AxisAngle4f(
                        (float) Math.toRadians(pitch), 0.0F, 0.0F, 1.0F)))
                .translate(0.0F, -1.0F, 0.0F);
        float end = (float) distance;
        appendBeamQuad(output, transform,
                0.5F, 1.0625F, 0.5F,
                0.5F, 1.0625F, -0.5F,
                0.5F, end, -0.5F,
                0.5F, end, 0.5F);
        appendBeamQuad(output, transform,
                -0.5F, 1.0625F, 0.5F,
                -0.5F, 1.0625F, -0.5F,
                -0.5F, end, -0.5F,
                -0.5F, end, 0.5F);
        appendBeamQuad(output, transform,
                0.5F, 1.0625F, 0.5F,
                -0.5F, 1.0625F, 0.5F,
                -0.5F, end, 0.5F,
                0.5F, end, 0.5F);
        appendBeamQuad(output, transform,
                0.5F, 1.0625F, -0.5F,
                -0.5F, 1.0625F, -0.5F,
                -0.5F, end, -0.5F,
                0.5F, end, -0.5F);
    }

    private static void appendBeamQuad(
            List<BeamVertex> output, Matrix4f transform,
            float ax, float ay, float az,
            float bx, float by, float bz,
            float cx, float cy, float cz,
            float dx, float dy, float dz) {
        appendBeamVertex(output, transform, ax, ay, az, Math.round(0.01F * 255.0F));
        appendBeamVertex(output, transform, bx, by, bz, Math.round(0.01F * 255.0F));
        appendBeamVertex(output, transform, cx, cy, cz, Math.round(0.005F * 255.0F));
        appendBeamVertex(output, transform, dx, dy, dz, Math.round(0.005F * 255.0F));
    }

    private static void appendBeamVertex(
            List<BeamVertex> output, Matrix4f transform,
            float x, float y, float z, int alpha) {
        Vector3f transformed = transform.transformPosition(new Vector3f(x, y, z));
        output.add(new BeamVertex(transformed.x, transformed.y, transformed.z, alpha));
    }

    private static AABB transformedBounds(
            BlockPos pos, Matrix4f transform,
            float minX, float minY, float minZ,
            float maxX, float maxY, float maxZ) {
        float outMinX = Float.POSITIVE_INFINITY;
        float outMinY = Float.POSITIVE_INFINITY;
        float outMinZ = Float.POSITIVE_INFINITY;
        float outMaxX = Float.NEGATIVE_INFINITY;
        float outMaxY = Float.NEGATIVE_INFINITY;
        float outMaxZ = Float.NEGATIVE_INFINITY;
        for (float x : new float[]{minX, maxX}) {
            for (float y : new float[]{minY, maxY}) {
                for (float z : new float[]{minZ, maxZ}) {
                    Vector3f point = transform.transformPosition(new Vector3f(x, y, z));
                    outMinX = Math.min(outMinX, point.x);
                    outMinY = Math.min(outMinY, point.y);
                    outMinZ = Math.min(outMinZ, point.z);
                    outMaxX = Math.max(outMaxX, point.x);
                    outMaxY = Math.max(outMaxY, point.y);
                    outMaxZ = Math.max(outMaxZ, point.z);
                }
            }
        }
        return new AABB(
                pos.getX() + outMinX, pos.getY() + outMinY, pos.getZ() + outMinZ,
                pos.getX() + outMaxX, pos.getY() + outMaxY, pos.getZ() + outMaxZ);
    }

    private static Direction facing(BlockState state) {
        if (!state.hasProperty(LargeMachineBlock.FACING)) {
            throw new IllegalStateException("Solar boiler block state is missing its facing property: " + state);
        }
        Direction direction = state.getValue(LargeMachineBlock.FACING);
        if (direction.getAxis().isVertical()) {
            throw new IllegalStateException("Solar boiler block state has a vertical facing: " + direction);
        }
        return direction;
    }

    private static void applySolarBoilerBoundsPose(Matrix4f transform, Direction facing) {
        switch (facing) {
            case EAST -> transform.rotate(new Quaternionf(new AxisAngle4f(
                    (float) Math.toRadians(90.0F), 0.0F, 1.0F, 0.0F)));
            case SOUTH -> { }
            case WEST -> transform.rotate(new Quaternionf(new AxisAngle4f(
                    (float) Math.toRadians(270.0F), 0.0F, 1.0F, 0.0F)));
            case NORTH -> transform.rotate(new Quaternionf(new AxisAngle4f(
                    (float) Math.toRadians(180.0F), 0.0F, 1.0F, 0.0F)));
            case UP, DOWN -> throw new IllegalArgumentException("Solar boiler facing must be horizontal: " + facing);
        }
    }

    private static Quaternionf yawQuaternion(float degrees) {
        return new Quaternionf(new AxisAngle4f(
                (float) Math.toRadians(degrees), 0.0F, 1.0F, 0.0F));
    }

    private static boolean sameSectionCoverage(AABB first, AABB second) {
        return sectionMin(first.minX) == sectionMin(second.minX)
                && sectionMin(first.minY) == sectionMin(second.minY)
                && sectionMin(first.minZ) == sectionMin(second.minZ)
                && sectionMax(first.maxX) == sectionMax(second.maxX)
                && sectionMax(first.maxY) == sectionMax(second.maxY)
                && sectionMax(first.maxZ) == sectionMax(second.maxZ);
    }

    private static int sectionMin(double value) {
        return net.minecraft.util.Mth.floor(value) >> 4;
    }

    private static int sectionMax(double value) {
        return net.minecraft.util.Mth.floor(Math.nextDown(value)) >> 4;
    }

    private void tryConvert() {
        int process = this.heat / 50;
        this.heatDisplay = process;
        process = Math.min(process, this.inputTank.amount());
        process = Math.min(process, (this.outputTank.capacity() - this.outputTank.amount()) / 100);
        if (process > 0) {
            this.inputTank.drain(water(), process, false);
            this.outputTank.fill(steam(), process * 100, false);
        }
        this.heat = 0;
        ensureTankTypes();
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
                    water(),
                    space,
                    this.worldPosition,
                    true
            );
            if (!drained.isEmpty()) {
                this.inputTank.fill(water(), drained.getAmount(), false);
            }
        }
    }

    private void pushOutput(Level level) {
        if (this.outputTank.amount() <= 0 || this.outputTank.type().isNone()) {
            return;
        }
        for (Port port : ports(level)) {
            if (this.outputTank.amount() <= 0) {
                break;
            }
            FluidStack stack = HbmFluids.toNeoStack(steam(), this.outputTank.amount());
            int accepted = HbmFluidNetworks.fillInto(
                    level,
                    port.connectorPos(),
                    port.face().getOpposite(),
                    stack,
                    this.worldPosition,
                    true
            );
            if (accepted > 0) {
                this.outputTank.drain(steam(), accepted, false);
            }
        }
    }

    private void ensureTankTypes() {
        if (this.inputTank.amount() == 0 && this.inputTank.type() != water()) {
            this.inputTank.setType(water());
        }
        if (this.outputTank.amount() == 0 && this.outputTank.type() != steam()) {
            this.outputTank.setType(steam());
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

    private static HbmFluidDefinition water() {
        return HbmFluids.byName("water").orElse(HbmFluids.none());
    }

    private static HbmFluidDefinition steam() {
        return HbmFluids.byName("steam").orElse(HbmFluids.none());
    }

    public record Port(BlockPos pos, Direction face) {
        public BlockPos connectorPos() {
            return this.pos.relative(this.face);
        }
    }

    public record RenderSnapshot(
            BlockState blockState,
            Direction facing,
            boolean beamsEnabled,
            Set<BlockPos> activeMirrors,
            List<BeamVertex> beamVertices,
            AABB bounds) {
        public RenderSnapshot {
            activeMirrors = Collections.unmodifiableSet(new LinkedHashSet<>(activeMirrors));
            beamVertices = List.copyOf(beamVertices);
        }
    }

    /** One already-transformed local vertex from the legacy four-face beam. */
    public record BeamVertex(float x, float y, float z, int alpha) {
    }

    private final class SolarBoilerFluidHandler implements IFluidHandler {
        private final BlockPos queriedPos;
        @Nullable
        private final Direction side;

        private SolarBoilerFluidHandler(BlockPos queriedPos, @Nullable Direction side) {
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
            return fluid == water();
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !allowsPort(this.queriedPos, this.side)) {
                return 0;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            if (fluid != water()) {
                return 0;
            }
            int filled = inputTank.fill(water(), resource.getAmount(), action.simulate());
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
            if (fluid != steam()) {
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
