package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Consumer;

public class SolarMirrorBlockEntity extends BlockEntity {
    private static volatile Consumer<SolarMirrorBlockEntity> clientRenderSnapshotUpdater = ignored -> { };
    private static volatile long clientRenderSnapshotRevision;
    private static final Set<SolarMirrorBlockEntity> loadedClientInstances =
            Collections.newSetFromMap(new WeakHashMap<>());

    private BlockPos target = BlockPos.ZERO;
    private boolean on;
    private volatile RenderSnapshot renderSnapshot;
    private boolean clientRenderSnapshotDirty = true;

    public SolarMirrorBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.SOLAR_MIRROR.get(), pos, blockState);
        this.renderSnapshot = new RenderSnapshot(blockState, false, 0.0F, 0.0F,
                null, null, initialConservativeBounds(pos), -1L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SolarMirrorBlockEntity mirror) {
        if (level.isClientSide) {
            mirror.tickClient(level);
            return;
        }
        mirror.tickServer(level);
    }

    public BlockPos target() {
        return this.target;
    }

    public boolean isOn() {
        return this.on;
    }

    public void setTarget(BlockPos target) {
        this.target = target.immutable();
        this.clientRenderSnapshotDirty = true;
        setChanged();
        sync();
        refreshClientRenderSnapshot();
    }

    /** Installed by the client renderer without loading client classes on a dedicated server. */
    public static void setClientRenderSnapshotUpdater(Consumer<SolarMirrorBlockEntity> updater) {
        clientRenderSnapshotUpdater = Objects.requireNonNull(updater);
        markLoadedClientSnapshotsDirty(false);
    }

    /** Called on the client thread after a new model bake has been captured. */
    public static void invalidateClientRenderSnapshotsAfterBake() {
        clientRenderSnapshotRevision++;
        markLoadedClientSnapshotsDirty(true);
    }

    public static long clientRenderSnapshotRevision() {
        return clientRenderSnapshotRevision;
    }

    private static void markLoadedClientSnapshotsDirty(boolean refreshNow) {
        List<SolarMirrorBlockEntity> loaded;
        synchronized (loadedClientInstances) {
            loaded = new ArrayList<>(loadedClientInstances);
        }
        for (SolarMirrorBlockEntity mirror : loaded) {
            mirror.clientRenderSnapshotDirty = true;
            if (refreshNow) {
                mirror.refreshClientRenderSnapshot();
            }
        }
    }

    public RenderSnapshot renderSnapshot() {
        return renderSnapshot;
    }

    public RenderInputs captureRenderInputs() {
        return new RenderInputs(getBlockState(), target);
    }

    public void publishClientRenderSnapshot(RenderSnapshot snapshot) {
        if (level == null || !level.isClientSide) {
            return;
        }
        RenderSnapshot previous = renderSnapshot;
        renderSnapshot = Objects.requireNonNull(snapshot);
        clientRenderSnapshotDirty = false;
        if (!sameRenderSections(previous.bounds(), snapshot.bounds())) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public void refreshClientRenderSnapshot() {
        if (clientRenderSnapshotDirty && level != null && level.isClientSide) {
            clientRenderSnapshotUpdater.accept(this);
        }
    }

    private static boolean sameRenderSections(AABB first, AABB second) {
        return minSection(first.minX) == minSection(second.minX)
                && maxSection(first.minX, first.maxX) == maxSection(second.minX, second.maxX)
                && minSection(first.minY) == minSection(second.minY)
                && maxSection(first.minY, first.maxY) == maxSection(second.minY, second.maxY)
                && minSection(first.minZ) == minSection(second.minZ)
                && maxSection(first.minZ, first.maxZ) == maxSection(second.minZ, second.maxZ);
    }

    private static int minSection(double coordinate) {
        return SectionPos.blockToSectionCoord(Mth.floor(coordinate));
    }

    private static int maxSection(double min, double max) {
        double inclusiveMax = max > min ? Math.nextDown(max) : max;
        return SectionPos.blockToSectionCoord(Mth.floor(inclusiveMax));
    }

    @Override
    public void setBlockState(BlockState state) {
        BlockState previous = getBlockState();
        super.setBlockState(state);
        if (renderSnapshot != null && previous != state) {
            clientRenderSnapshotDirty = true;
            refreshClientRenderSnapshot();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("targetX", this.target.getX());
        tag.putInt("targetY", this.target.getY());
        tag.putInt("targetZ", this.target.getZ());
        tag.putBoolean("On", this.on);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.target = new BlockPos(tag.getInt("targetX"), tag.getInt("targetY"), tag.getInt("targetZ"));
        this.on = tag.getBoolean("On");
        this.clientRenderSnapshotDirty = true;
        refreshClientRenderSnapshot();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && level.isClientSide) {
            synchronized (loadedClientInstances) {
                loadedClientInstances.add(this);
            }
        }
        clientRenderSnapshotDirty = true;
        refreshClientRenderSnapshot();
    }

    @Override
    public void setRemoved() {
        synchronized (loadedClientInstances) {
            loadedClientInstances.remove(this);
        }
        super.setRemoved();
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
        boolean wasOn = this.on;
        this.on = false;

        if (this.target.getY() >= this.worldPosition.getY()) {
            int sun = level.getBrightness(LightLayer.SKY, this.worldPosition) - level.getSkyDarken() - 11;
            if (sun > 0 && level.canSeeSky(this.worldPosition.above())) {
                this.on = true;
                BlockEntity targetEntity = level.getBlockEntity(this.target.below());
                if (targetEntity instanceof SolarBoilerBlockEntity boiler) {
                    boiler.addHeatInput(sun);
                }
            }
        }

        if (wasOn != this.on || level.getGameTime() % 20L == 0L) {
            sync();
        }
    }

    private void tickClient(Level level) {
        if (renderSnapshot.revision() != clientRenderSnapshotRevision) {
            clientRenderSnapshotDirty = true;
        }
        refreshClientRenderSnapshot();
        if (!this.on || this.target.getY() < this.worldPosition.getY()) {
            return;
        }
        BlockEntity targetEntity = level.getBlockEntity(this.target.below());
        if (targetEntity instanceof SolarBoilerBlockEntity boiler) {
            boiler.registerActiveMirror(this.worldPosition);
        }
    }

    private void sync() {
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public record RenderInputs(BlockState state, BlockPos target) {
        public RenderInputs {
            Objects.requireNonNull(state);
            target = target.immutable();
        }
    }

    private static AABB initialConservativeBounds(BlockPos pos) {
        /* Built-in mirror face vertices lie at x/z +/-0.5 and y=1+/-0.0625.
         * Its full two-axis rotational supports are sqrt(.50390625) in X/Z
         * and sqrt(.25390625) in Y. gamma(64) bounds the float quaternion,
         * matrix-composition, pivot-translation, and vertex-transform path;
         * nextUp/nextDown provide explicit outward rounding. Until captured
         * baked geometry is published the renderer emits no model, so this
         * also safely covers construction and onLoad ordering on any pack. */
        double unitRoundoff = 0x1.0p-24;
        double gamma64 = 64.0D * unitRoundoff / (1.0D - 64.0D * unitRoundoff);
        double horizontalRadius = Math.sqrt(0.50390625D);
        double verticalRadius = Math.sqrt(0.25390625D);
        double horizontalExtent = Math.nextUp(horizontalRadius
                + (horizontalRadius + 1.0D) * gamma64);
        double verticalExtent = Math.nextUp(verticalRadius
                + (verticalRadius + 1.0D) * gamma64);
        return new AABB(
                pos.getX() + Math.nextDown(0.5D - horizontalExtent), pos.getY(),
                pos.getZ() + Math.nextDown(0.5D - horizontalExtent),
                pos.getX() + Math.nextUp(0.5D + horizontalExtent),
                pos.getY() + Math.nextUp(1.0D + verticalExtent),
                pos.getZ() + Math.nextUp(0.5D + horizontalExtent));
    }

    public record RenderSnapshot(BlockState state, boolean rotationValid,
                                 float yawRadians, float pitchRadians,
                                 Object baseGeometryToken, Object mirrorGeometryToken,
                                 AABB bounds, long revision) {
        public RenderSnapshot {
            Objects.requireNonNull(state);
            Objects.requireNonNull(bounds);
        }
    }
}
