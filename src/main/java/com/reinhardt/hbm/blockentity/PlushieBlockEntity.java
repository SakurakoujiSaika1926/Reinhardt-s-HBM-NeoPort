package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.PlushieType;
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

public final class PlushieBlockEntity extends BlockEntity {
    private static volatile Consumer<PlushieBlockEntity> clientSnapshotUpdater = ignored -> { };
    private static volatile long clientSnapshotRevision;
    private PlushieType type = PlushieType.NONE;
    private int squishTimer;
    private volatile RenderSnapshot renderSnapshot;
    private boolean clientSnapshotDirty = true;

    public PlushieBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.PLUSHIE.get(), pos, state);
        this.renderSnapshot = new RenderSnapshot(state, PlushieType.NONE, null,
                new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ()), 0L);
    }

    public PlushieType type() {
        return type;
    }

    public void setType(PlushieType type) {
        this.type = type == null ? PlushieType.NONE : type;
        setChanged();
        Level level = getLevel();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
        this.clientSnapshotDirty = true;
        refreshClientRenderSnapshot();
    }

    public void squish() {
        squishTimer = 11;
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, PlushieBlockEntity plushie) {
        if (plushie.renderSnapshot.revision() != clientSnapshotRevision) {
            plushie.clientSnapshotDirty = true;
        }
        plushie.refreshClientRenderSnapshot();
        if (plushie.squishTimer > 0) plushie.squishTimer--;
    }

    public static void setClientSnapshotUpdater(Consumer<PlushieBlockEntity> updater) {
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

    public float squishTimer(float partialTick) {
        return squishTimer > 0 ? Math.max(0.0F, squishTimer - partialTick) : 0.0F;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putByte("type", (byte) type.ordinal());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        type = PlushieType.byOrdinal(Math.abs(tag.getByte("type")));
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

    public record RenderSnapshot(BlockState blockState, PlushieType type,
                                 Object geometryToken, AABB bounds, long revision) {
        public RenderSnapshot {
            Objects.requireNonNull(blockState);
            Objects.requireNonNull(type);
            Objects.requireNonNull(bounds);
        }
    }
}
