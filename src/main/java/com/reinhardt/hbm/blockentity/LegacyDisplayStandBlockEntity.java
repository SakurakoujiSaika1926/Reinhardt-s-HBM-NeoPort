package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.event.MeteorPedestalBroadcasts;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;

public final class LegacyDisplayStandBlockEntity extends BlockEntity {
    private static volatile Consumer<LegacyDisplayStandBlockEntity> clientRenderSnapshotUpdater = ignored -> { };
    private static volatile long clientRenderSnapshotRevision;

    private ItemStack displayedItem = ItemStack.EMPTY;
    private volatile RenderSnapshot renderSnapshot;
    private boolean clientRenderSnapshotDirty = true;

    public LegacyDisplayStandBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.LEGACY_DISPLAY_STAND.get(), pos, state);
        this.renderSnapshot = new RenderSnapshot(state, ItemStack.EMPTY, false, null, null,
                new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ()), 0L);
    }

    public ItemStack displayedItem() {
        return this.displayedItem;
    }

    public void setDisplayedItem(ItemStack stack) {
        this.displayedItem = stack.copy();
        this.clientRenderSnapshotDirty = true;
        sync();
        refreshClientRenderSnapshot();
    }

    public ItemStack takeDisplayedItem() {
        ItemStack result = this.displayedItem;
        this.displayedItem = ItemStack.EMPTY;
        this.clientRenderSnapshotDirty = true;
        sync();
        refreshClientRenderSnapshot();
        return result;
    }

    void clearDisplayedItem() {
        this.displayedItem = ItemStack.EMPTY;
        this.clientRenderSnapshotDirty = true;
        sync();
        refreshClientRenderSnapshot();
    }

    public void dropDisplayedItem() {
        if (this.level == null || this.level.isClientSide || this.displayedItem.isEmpty()) {
            return;
        }
        this.level.addFreshEntity(new ItemEntity(this.level,
                this.worldPosition.getX() + 0.5D, this.worldPosition.getY() + 0.5D,
                this.worldPosition.getZ() + 0.5D, this.displayedItem.copy()));
        this.displayedItem = ItemStack.EMPTY;
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state,
                                  LegacyDisplayStandBlockEntity stand) {
        if (stand.renderSnapshot.revision() != clientRenderSnapshotRevision) {
            stand.clientRenderSnapshotDirty = true;
        }
        stand.refreshClientRenderSnapshot();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  LegacyDisplayStandBlockEntity stand) {
        if (level instanceof ServerLevel serverLevel && level.getGameTime() % 20L == 0L) {
            MeteorPedestalBroadcasts.publish(serverLevel, pos, stand.displayedItem);
        }
    }

    /** Installed by the client renderer without loading client classes on a dedicated server. */
    public static void setClientRenderSnapshotUpdater(Consumer<LegacyDisplayStandBlockEntity> updater) {
        clientRenderSnapshotUpdater = Objects.requireNonNull(updater);
        clientRenderSnapshotRevision++;
    }

    public static long clientRenderSnapshotRevision() {
        return clientRenderSnapshotRevision;
    }

    public RenderSnapshot renderSnapshot() {
        return renderSnapshot;
    }

    public ItemStack copyDisplayedItemForRenderSnapshot() {
        return displayedItem.copy();
    }

    public void refreshClientRenderSnapshot() {
        if (clientRenderSnapshotDirty && level != null && level.isClientSide) {
            clientRenderSnapshotUpdater.accept(this);
        }
    }

    public void publishClientRenderSnapshot(RenderSnapshot snapshot) {
        if (level == null || !level.isClientSide) {
            return;
        }
        RenderSnapshot previous = renderSnapshot;
        renderSnapshot = Objects.requireNonNull(snapshot);
        clientRenderSnapshotDirty = false;
        if (!previous.bounds().equals(snapshot.bounds())) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
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
        tag.put("item", this.displayedItem.saveOptional(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.displayedItem = ItemStack.parseOptional(registries, tag.getCompound("item"));
        this.clientRenderSnapshotDirty = true;
        refreshClientRenderSnapshot();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        clientRenderSnapshotDirty = true;
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

    private void sync() {
        setChanged();
        if (this.level != null) {
            BlockState state = getBlockState();
            this.level.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    public record RenderSnapshot(BlockState state, ItemStack displayedItem, boolean threeDimensional,
                                 Object modelToken, Object geometryToken, AABB bounds, long revision) {
        public RenderSnapshot {
            Objects.requireNonNull(state);
            displayedItem = displayedItem.copy();
            if (displayedItem.isEmpty()) {
                modelToken = null;
            } else {
                Objects.requireNonNull(modelToken);
            }
            Objects.requireNonNull(bounds);
        }
    }
}
