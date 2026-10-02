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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Direct state port of TileEntityMachineRadarScreen. */
public final class RadarScreenBlockEntity extends BlockEntity {
    private static final long CLIENT_SYNC_INTERVAL = 25L;

    private final List<RadarTarget> entries = new ArrayList<>();
    private int refX;
    private int refY;
    private int refZ;
    private int range;
    private boolean linked;
    private volatile RenderSnapshot renderSnapshot = RenderSnapshot.empty();
    private long lastRadarWriteTick = Long.MIN_VALUE;
    private long lastClientSyncTick = Long.MIN_VALUE;

    public RadarScreenBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.RADAR_SCREEN.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RadarScreenBlockEntity screen) {
        if (level.isClientSide) {
            return;
        }

        long gameTime = level.getGameTime();
        // TileEntityMachineRadarScreen clears its transient feed every tick and
        // TileEntityMachineRadarNT writes it again.  Defer the clear until one
        // complete tick passed without a write so the result does not depend on
        // which block entity happens to tick first in the modern level list.
        if (screen.lastRadarWriteTick < gameTime - 1L
                && (screen.linked || !screen.entries.isEmpty())) {
            screen.entries.clear();
            screen.linked = false;
            screen.setChanged();
            screen.lastClientSyncTick = gameTime;
            screen.syncToClient();
        }
    }

    /** Called by TileEntityMachineRadarNT's modern port once each server tick. */
    public void receiveRadarData(List<RadarTarget> targets, BlockPos radarPos, int radarRange) {
        if (this.level == null || this.level.isClientSide) {
            return;
        }
        boolean becameLinked = !this.linked;
        this.entries.clear();
        this.entries.addAll(targets);
        this.refX = radarPos.getX();
        this.refY = radarPos.getY();
        this.refZ = radarPos.getZ();
        this.range = radarRange;
        this.linked = true;
        this.lastRadarWriteTick = this.level.getGameTime();
        publishRenderSnapshot();
        setChanged();
        long gameTime = this.level.getGameTime();
        // Explicitly handle the sentinel.  Subtracting Long.MIN_VALUE wraps to
        // a negative number and previously prevented the first client update
        // forever, leaving every placed display on its unlinked noise frame.
        if (becameLinked || this.lastClientSyncTick == Long.MIN_VALUE
                || gameTime - this.lastClientSyncTick >= CLIENT_SYNC_INTERVAL) {
            this.lastClientSyncTick = gameTime;
            syncToClient();
        }
    }

    public boolean openLinkedRadar(ServerPlayer player) {
        return openLinkedRadar(player, this.worldPosition, this.worldPosition);
    }

    public boolean openLinkedRadar(ServerPlayer player, BlockPos accessPos, BlockPos accessCorePos) {
        if (this.level == null || !this.linked) {
            return false;
        }
        BlockPos radarPos = new BlockPos(this.refX, this.refY, this.refZ);
        if (!(this.level.getBlockEntity(radarPos) instanceof LegacyMachineBlockEntity radar)
                || (!radar.machineId().equals("machine_radar") && !radar.machineId().equals("machine_radar_large"))) {
            return false;
        }
        player.openMenu(radar, buffer -> buffer.writeBlockPos(radarPos));
        MachineDummyMenuAccess.record(player, accessPos, accessCorePos, radarPos);
        return true;
    }

    public boolean linked() {
        return this.linked;
    }

    public int refX() {
        return this.refX;
    }

    public int refY() {
        return this.refY;
    }

    public int refZ() {
        return this.refZ;
    }

    public int range() {
        return this.range;
    }

    public List<RadarTarget> entries() {
        return List.copyOf(this.entries);
    }

    /** One immutable publication consumed once by the renderer per frame. */
    public RenderSnapshot renderSnapshot() {
        return this.renderSnapshot;
    }

    private void publishRenderSnapshot() {
        if (this.level != null && !this.level.isClientSide) {
            return;
        }
        this.renderSnapshot = new RenderSnapshot(
                this.linked, this.refX, this.refZ, this.range, List.copyOf(this.entries));
    }

    private void syncToClient() {
        setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("RefX", this.refX);
        tag.putInt("RefY", this.refY);
        tag.putInt("RefZ", this.refZ);
        tag.putInt("Range", this.range);
        tag.putBoolean("Linked", this.linked);
        ListTag targets = new ListTag();
        for (RadarTarget target : this.entries) {
            CompoundTag entry = new CompoundTag();
            entry.putString("Name", target.name());
            entry.putInt("Blip", target.blipLevel());
            entry.putInt("X", target.x());
            entry.putInt("Y", target.y());
            entry.putInt("Z", target.z());
            entry.putInt("Entity", target.entityId());
            entry.putBoolean("Redstone", target.redstone());
            targets.add(entry);
        }
        tag.put("Targets", targets);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.refX = tag.getInt("RefX");
        this.refY = tag.getInt("RefY");
        this.refZ = tag.getInt("RefZ");
        this.range = tag.getInt("Range");
        this.linked = tag.getBoolean("Linked");
        this.entries.clear();
        ListTag targets = tag.getList("Targets", Tag.TAG_COMPOUND);
        for (int index = 0; index < targets.size(); index++) {
            CompoundTag entry = targets.getCompound(index);
            this.entries.add(new RadarTarget(
                    entry.getString("Name"), entry.getInt("Blip"), entry.getInt("X"), entry.getInt("Y"),
                    entry.getInt("Z"), entry.getInt("Entity"), entry.getBoolean("Redstone")
            ));
        }
        publishRenderSnapshot();
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

    public AABB legacyRenderBounds() {
        return new AABB(this.worldPosition.getX() - 1, this.worldPosition.getY(), this.worldPosition.getZ() - 1,
                this.worldPosition.getX() + 2, this.worldPosition.getY() + 2, this.worldPosition.getZ() + 2);
    }

    public record RenderSnapshot(boolean linked, int refX, int refZ, int range,
                                 List<RadarTarget> entries) {
        public RenderSnapshot {
            entries = List.copyOf(entries);
        }

        private static RenderSnapshot empty() {
            return new RenderSnapshot(false, 0, 0, 0, List.of());
        }
    }
}
