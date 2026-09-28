package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class DecoLootBlockEntity extends BlockEntity {
    private static volatile Consumer<DecoLootBlockEntity> clientRenderSnapshotUpdater = ignored -> { };
    private static volatile long clientRenderSnapshotRevision;

    private final List<LootEntry> items = new ArrayList<>();
    private volatile RenderSnapshot renderSnapshot;
    private boolean clientRenderSnapshotDirty = true;

    public DecoLootBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.DECO_LOOT.get(), pos, state);
        this.renderSnapshot = new RenderSnapshot(List.of(), null,
                new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ()), 0L);
    }

    public List<LootEntry> items() {
        return this.items;
    }

    public void addItem(ItemStack stack, double x, double y, double z) {
        if (!stack.isEmpty()) {
            this.items.add(new LootEntry(stack.copy(), x, y, z));
            this.clientRenderSnapshotDirty = true;
            sync();
            refreshClientRenderSnapshot();
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, DecoLootBlockEntity loot) {
        if (loot.renderSnapshot.revision() != clientRenderSnapshotRevision) {
            loot.clientRenderSnapshotDirty = true;
        }
        loot.refreshClientRenderSnapshot();
    }

    /** Installed by the client renderer without introducing client classes on a dedicated server. */
    public static void setClientRenderSnapshotUpdater(Consumer<DecoLootBlockEntity> updater) {
        clientRenderSnapshotUpdater = Objects.requireNonNull(updater);
        clientRenderSnapshotRevision++;
    }

    public static long clientRenderSnapshotRevision() {
        return clientRenderSnapshotRevision;
    }

    public RenderSnapshot renderSnapshot() {
        return renderSnapshot;
    }

    public List<LootEntry> copyEntriesForRenderSnapshot() {
        List<LootEntry> copy = new ArrayList<>(items.size());
        for (LootEntry entry : items) {
            copy.add(new LootEntry(entry.stack().copy(), entry.x(), entry.y(), entry.z()));
        }
        return List.copyOf(copy);
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

    public void dropContents() {
        if (this.level == null || this.level.isClientSide) {
            return;
        }
        for (LootEntry entry : this.items) {
            ItemEntity item = new ItemEntity(
                    this.level,
                    this.worldPosition.getX() + 0.5D,
                    this.worldPosition.getY(),
                    this.worldPosition.getZ() + 0.5D,
                    entry.stack.copy()
            );
            this.level.addFreshEntity(item);
        }
        this.items.clear();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag list = new ListTag();
        for (LootEntry entry : this.items) {
            CompoundTag item = new CompoundTag();
            item.put("stack", entry.stack.saveOptional(registries));
            item.putDouble("x", entry.x);
            item.putDouble("y", entry.y);
            item.putDouble("z", entry.z);
            list.add(item);
        }
        tag.put("items", list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.items.clear();
        ListTag list = tag.getList("items", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag item = list.getCompound(i);
            ItemStack stack = ItemStack.parseOptional(registries, item.getCompound("stack"));
            if (!stack.isEmpty()) {
                this.items.add(new LootEntry(stack, item.getDouble("x"), item.getDouble("y"), item.getDouble("z")));
            }
        }
        clientRenderSnapshotDirty = true;
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
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public record LootEntry(ItemStack stack, double x, double y, double z) {
    }

    /** Client-baked render data; Object keeps BakedModel out of common/dedicated-server linkage. */
    public record RenderEntry(ItemStack stack, double x, double y, double z,
                              Object modelToken, Object legacyGeometryToken, boolean legacyLayer) {
        public RenderEntry {
            stack = stack.copy();
            if (stack.isEmpty()) {
                modelToken = null;
                legacyGeometryToken = null;
                legacyLayer = false;
            }
        }
    }

    public record RenderSnapshot(List<RenderEntry> entries, Object geometryToken,
                                 AABB bounds, long revision) {
        public RenderSnapshot {
            entries = List.copyOf(entries);
            Objects.requireNonNull(bounds);
        }
    }
}
