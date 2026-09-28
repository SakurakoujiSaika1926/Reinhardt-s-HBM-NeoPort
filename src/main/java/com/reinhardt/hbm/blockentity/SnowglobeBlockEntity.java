package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.SnowglobeType;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;

public final class SnowglobeBlockEntity extends BlockEntity {
    private static volatile Consumer<SnowglobeBlockEntity> clientSnapshotUpdater = ignored -> { };
    private static volatile long clientSnapshotRevision;
    private SnowglobeType type = SnowglobeType.NONE;
    private volatile RenderSnapshot renderSnapshot;
    private boolean clientSnapshotDirty = true;

    public SnowglobeBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.SNOWGLOBE.get(), pos, state);
        this.renderSnapshot = new RenderSnapshot(state, SnowglobeType.NONE, null,
                new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ()), 0L);
    }

    public SnowglobeType type() {
        return type;
    }

    public void setType(SnowglobeType type) {
        this.type = type == null ? SnowglobeType.NONE : type;
        setChanged();
        Level level = getLevel();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
        this.clientSnapshotDirty = true;
        refreshClientRenderSnapshot();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, SnowglobeBlockEntity globe) {
        if (globe.renderSnapshot.revision() != clientSnapshotRevision) {
            globe.clientSnapshotDirty = true;
        }
        globe.refreshClientRenderSnapshot();
    }

    public static void setClientSnapshotUpdater(Consumer<SnowglobeBlockEntity> updater) {
        clientSnapshotUpdater = Objects.requireNonNull(updater);
        clientSnapshotRevision++;
    }

    public static long clientSnapshotRevision() {
        return clientSnapshotRevision;
    }

    public RenderSnapshot renderSnapshot() {
        return this.renderSnapshot;
    }

    public void refreshClientRenderSnapshot() {
        if (this.clientSnapshotDirty && this.level != null && this.level.isClientSide) {
            clientSnapshotUpdater.accept(this);
        }
    }

    public void publishClientRenderSnapshot(RenderSnapshot snapshot) {
        if (this.level == null || !this.level.isClientSide) {
            return;
        }
        RenderSnapshot previous = this.renderSnapshot;
        this.renderSnapshot = Objects.requireNonNull(snapshot);
        this.clientSnapshotDirty = false;
        if (!sameSectionCoverage(previous.bounds(), snapshot.bounds())) {
            this.level.sendBlockUpdated(this.worldPosition, snapshot.blockState(), snapshot.blockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putByte("type", (byte) type.ordinal());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        type = SnowglobeType.byOrdinal(Math.abs(tag.getByte("type")));
        this.clientSnapshotDirty = true;
        refreshClientRenderSnapshot();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        this.clientSnapshotDirty = true;
        refreshClientRenderSnapshot();
    }

    @Override
    public void setBlockState(BlockState state) {
        super.setBlockState(state);
        this.clientSnapshotDirty = true;
        refreshClientRenderSnapshot();
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

    public record RenderSnapshot(BlockState blockState, SnowglobeType type,
                                 Object geometryToken, AABB bounds, long revision) {
        public RenderSnapshot {
            Objects.requireNonNull(blockState);
            Objects.requireNonNull(type);
            Objects.requireNonNull(bounds);
        }
    }
}
