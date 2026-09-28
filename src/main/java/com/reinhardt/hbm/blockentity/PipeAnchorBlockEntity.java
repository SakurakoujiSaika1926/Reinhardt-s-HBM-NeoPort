package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.PipeAnchorBlock;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Consumer;

public final class PipeAnchorBlockEntity extends FluidPipeBlockEntity {
    private static final Set<PipeAnchorBlockEntity> CLIENT_INSTANCES =
            Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));
    private static volatile Consumer<PipeAnchorBlockEntity> clientRenderSnapshotUpdater = ignored -> { };
    private static volatile Consumer<PipeAnchorBlockEntity> clientGpuCacheDisposer = ignored -> { };
    private final Set<BlockPos> links = new LinkedHashSet<>();
    private volatile RenderSnapshot renderSnapshot;
    private boolean clientRenderSnapshotDirty = true;

    public PipeAnchorBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.PIPE_ANCHOR.get(), pos, blockState);
        this.renderSnapshot = RenderSnapshot.empty(pos, blockState);
    }

    public Direction externalDirection() {
        return getBlockState().getValue(PipeAnchorBlock.FACING).getOpposite();
    }

    @Override
    public boolean canConnectFrom(Direction direction, HbmFluidDefinition fluid) {
        return direction == externalDirection() && canConnect(fluid);
    }

    @Override
    public List<BlockPos> networkLinks() {
        return List.copyOf(links);
    }

    public RenderSnapshot renderSnapshot() {
        return renderSnapshot;
    }

    public static void setClientRenderSnapshotUpdater(Consumer<PipeAnchorBlockEntity> updater) {
        clientRenderSnapshotUpdater = Objects.requireNonNull(updater);
    }

    public static void setClientGpuCacheDisposer(Consumer<PipeAnchorBlockEntity> disposer) {
        clientGpuCacheDisposer = Objects.requireNonNull(disposer);
    }

    /**
     * Runs from the client block-entity ticker (the main client thread). It is
     * the only code which follows remote links. SectionCompiler and the render
     * thread consume the immutable volatile publication without touching the
     * level or another block entity.
     */
    public void refreshClientRenderSnapshot() {
        if (!clientRenderSnapshotDirty || level == null || !level.isClientSide) {
            return;
        }
        clientRenderSnapshotDirty = false;
        clientRenderSnapshotUpdater.accept(this);
    }

    /** Main-client-thread input capture; remote endpoints are never followed by the render thread. */
    public RenderInputs captureClientRenderInputs() {
        BlockState state = getBlockState();
        Direction facing = state.getValue(PipeAnchorBlock.FACING);
        List<BlockPos> visibleLinks = new ArrayList<>();
        for (BlockPos link : links) {
            if (isDominant(worldPosition, link)
                    && level.getBlockEntity(link) instanceof PipeAnchorBlockEntity other
                    && type() == other.type()) {
                visibleLinks.add(link.immutable());
            }
        }
        return new RenderInputs(state, List.copyOf(visibleLinks), facing, type().color());
    }

    public void publishClientRenderSnapshot(RenderSnapshot snapshot) {
        if (level == null || !level.isClientSide) {
            return;
        }
        RenderSnapshot previous = this.renderSnapshot;
        this.renderSnapshot = Objects.requireNonNull(snapshot);
        if (!sameRenderSections(previous.bounds(), snapshot.bounds())) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** Marks the main-thread snapshot dirty without following any remote link. */
    public void markClientRenderSnapshotDirty() {
        if (level != null && level.isClientSide) {
            clientRenderSnapshotDirty = true;
        }
    }

    @Override
    public void markNetworkChanged() {
        super.markNetworkChanged();
        if (level != null && level.isClientSide) {
            clientRenderSnapshotDirty = true;
            markClientEndpointChanged(level, worldPosition);
        }
    }

    @Override
    public void setBlockState(BlockState state) {
        Direction previousFacing = getBlockState().hasProperty(PipeAnchorBlock.FACING)
                ? getBlockState().getValue(PipeAnchorBlock.FACING) : null;
        super.setBlockState(state);
        Direction nextFacing = state.hasProperty(PipeAnchorBlock.FACING)
                ? state.getValue(PipeAnchorBlock.FACING) : null;
        if (previousFacing != nextFacing && level != null && level.isClientSide) {
            clientRenderSnapshotDirty = true;
            markClientEndpointChanged(level, worldPosition);
        }
    }

    public boolean addLink(BlockPos other) {
        if (other.equals(worldPosition)) {
            return false;
        }
        boolean changed = links.add(other.immutable());
        if (changed) {
            markNetworkChanged();
        }
        return changed;
    }

    public boolean removeLink(BlockPos other) {
        boolean changed = links.remove(other);
        if (changed) {
            markNetworkChanged();
        }
        return changed;
    }

    @Override
    public void setRemoved() {
        if (level != null && level.isClientSide) {
            CLIENT_INSTANCES.remove(this);
            markClientEndpointChanged(level, worldPosition);
            clientGpuCacheDisposer.accept(this);
        }
        if (level != null && !level.isClientSide) {
            for (BlockPos link : List.copyOf(links)) {
                if (level.getBlockEntity(link) instanceof PipeAnchorBlockEntity other) {
                    other.removeLink(worldPosition);
                }
            }
            links.clear();
        }
        super.setRemoved();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        long[] packed = new long[links.size()];
        int index = 0;
        for (BlockPos link : links) {
            packed[index++] = link.asLong();
        }
        tag.putLongArray("Links", packed);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        links.clear();
        for (long packed : tag.getLongArray("Links")) {
            links.add(BlockPos.of(packed));
        }
        clientRenderSnapshotDirty = true;
        if (level != null) {
            markNetworkChanged();
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        clientRenderSnapshotDirty = true;
        if (level != null && level.isClientSide) {
            CLIENT_INSTANCES.add(this);
            markClientEndpointChanged(level, worldPosition);
        }
    }

    /** Chunk events call this on the client thread; the ticker performs the actual lookup once. */
    public static void onClientEndpointChunkChanged(Level level, ChunkPos chunkPos) {
        List<PipeAnchorBlockEntity> anchors;
        synchronized (CLIENT_INSTANCES) {
            anchors = List.copyOf(CLIENT_INSTANCES);
        }
        for (PipeAnchorBlockEntity anchor : anchors) {
            if (anchor.level != level || anchor.isRemoved()) {
                continue;
            }
            for (BlockPos link : anchor.links) {
                if (chunkPos.equals(new ChunkPos(link))) {
                    anchor.clientRenderSnapshotDirty = true;
                    break;
                }
            }
        }
    }

    public static void clearClientRenderInstances() {
        CLIENT_INSTANCES.clear();
    }

    public static void invalidateClientRenderSnapshotsAfterBake() {
        List<PipeAnchorBlockEntity> anchors;
        synchronized (CLIENT_INSTANCES) {
            anchors = List.copyOf(CLIENT_INSTANCES);
        }
        for (PipeAnchorBlockEntity anchor : anchors) {
            if (!anchor.isRemoved()) {
                anchor.clientRenderSnapshotDirty = true;
            }
        }
    }

    private static void markClientEndpointChanged(Level level, BlockPos endpoint) {
        List<PipeAnchorBlockEntity> anchors;
        synchronized (CLIENT_INSTANCES) {
            anchors = List.copyOf(CLIENT_INSTANCES);
        }
        for (PipeAnchorBlockEntity anchor : anchors) {
            if (anchor.level == level && !anchor.isRemoved() && anchor.links.contains(endpoint)) {
                anchor.clientRenderSnapshotDirty = true;
            }
        }
    }

    private static boolean isDominant(BlockPos first, BlockPos second) {
        if (first.getX() != second.getX()) return first.getX() < second.getX();
        if (first.getY() != second.getY()) return first.getY() < second.getY();
        return first.getZ() < second.getZ();
    }

    private static boolean sameRenderSections(AABB first, AABB second) {
        return net.minecraft.core.SectionPos.blockToSectionCoord(net.minecraft.util.Mth.floor(first.minX))
                == net.minecraft.core.SectionPos.blockToSectionCoord(net.minecraft.util.Mth.floor(second.minX))
                && maxSection(first.minX, first.maxX) == maxSection(second.minX, second.maxX)
                && net.minecraft.core.SectionPos.blockToSectionCoord(net.minecraft.util.Mth.floor(first.minY))
                == net.minecraft.core.SectionPos.blockToSectionCoord(net.minecraft.util.Mth.floor(second.minY))
                && maxSection(first.minY, first.maxY) == maxSection(second.minY, second.maxY)
                && net.minecraft.core.SectionPos.blockToSectionCoord(net.minecraft.util.Mth.floor(first.minZ))
                == net.minecraft.core.SectionPos.blockToSectionCoord(net.minecraft.util.Mth.floor(second.minZ))
                && maxSection(first.minZ, first.maxZ) == maxSection(second.minZ, second.maxZ);
    }

    private static int maxSection(double min, double max) {
        return net.minecraft.core.SectionPos.blockToSectionCoord(
                net.minecraft.util.Mth.floor(max > min ? Math.nextDown(max) : max));
    }

    public record RenderInputs(BlockState blockState, List<BlockPos> links, Direction facing, int color) {
        public RenderInputs {
            links = List.copyOf(links);
        }
    }

    public record ConnectionPose(float yawDegrees, float xDegrees, float length, float ringOffset) {
    }

    public record RenderSnapshot(
            Object geometryToken, BlockState blockState, List<BlockPos> links,
            List<ConnectionPose> connections, AABB bounds, Direction facing, int color) {
        public RenderSnapshot {
            links = List.copyOf(links);
            connections = List.copyOf(connections);
            Objects.requireNonNull(bounds);
        }

        private static RenderSnapshot empty(BlockPos pos, BlockState state) {
            AABB empty = new AABB(pos.getX(), pos.getY(), pos.getZ(),
                    pos.getX(), pos.getY(), pos.getZ());
            Direction facing = state.getValue(PipeAnchorBlock.FACING);
            return new RenderSnapshot(null, state, List.of(), List.of(), empty, facing, 0xFFFFFFFF);
        }
    }
}
