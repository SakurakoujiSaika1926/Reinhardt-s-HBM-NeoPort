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
    private long lastRadarWriteTick = Long.MIN_VALUE;
    private long lastClientSyncTick = Long.MIN_VALUE;

    public RadarScreenBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.RADAR_SCREEN.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RadarScreenBlockEntity screen) {
        if (level.isClientSide) {
            return;
        }

        // The 1.7.10 screen resets every server tick. The write stamp keeps the
        // same result independent of the block-entity tick ordering.
        if (screen.lastRadarWriteTick != level.getGameTime()) {
            screen.entries.clear();
            screen.linked = false;
        }
        if (level.getGameTime() - screen.lastClientSyncTick >= CLIENT_SYNC_INTERVAL) {
            screen.lastClientSyncTick = level.getGameTime();
            screen.syncToClient();
        }
    }

    /** Called by TileEntityMachineRadarNT's modern port once each server tick. */
    public void receiveRadarData(List<RadarTarget> targets, BlockPos radarPos, int radarRange) {
        if (this.level == null || this.level.isClientSide) {
            return;
        }
        this.entries.clear();
        this.entries.addAll(targets);
        this.refX = radarPos.getX();
        this.refY = radarPos.getY();
        this.refZ = radarPos.getZ();
        this.range = radarRange;
        this.linked = true;
        this.lastRadarWriteTick = this.level.getGameTime();
        setChanged();
        if (this.level.getGameTime() - this.lastClientSyncTick >= CLIENT_SYNC_INTERVAL) {
            this.lastClientSyncTick = this.level.getGameTime();
            syncToClient();
        }
    }

    public boolean openLinkedRadar(ServerPlayer player) {
        if (this.level == null || !this.linked) {
            return false;
        }
        BlockPos radarPos = new BlockPos(this.refX, this.refY, this.refZ);
        if (!(this.level.getBlockEntity(radarPos) instanceof LegacyMachineBlockEntity radar)
                || (!radar.machineId().equals("machine_radar") && !radar.machineId().equals("machine_radar_large"))) {
            return false;
        }
        player.openMenu(radar, buffer -> buffer.writeBlockPos(radarPos));
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
}
