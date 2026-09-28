package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.satellite.SatelliteSavedData;
import com.reinhardt.hbm.satellite.SatelliteEventTracker;
import com.reinhardt.hbm.rtty.HbmRttySystem;
import com.reinhardt.hbm.rtty.RttyInfo;
import com.reinhardt.hbm.rtty.RttyInteractive;
import com.reinhardt.hbm.rtty.RttyValueProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;

/** Exact connection state and dish animation constants from TileEntityMachineSatLink. */
public final class SatelliteGroundStationBlockEntity extends BlockEntity
        implements RttyValueProvider, RttyInteractive {
    public static final float SPEED = 0.25F;
    public static final float ACTIVE_ROTATION = -15.0F;
    public static final float ACTIVE_LIFT = -45.0F;
    public static final float INACTIVE_ROTATION = 0.0F;
    public static final float INACTIVE_LIFT = -85.0F;

    private boolean connected;
    private int frequency;
    private float rotation = INACTIVE_ROTATION;
    private float previousRotation = INACTIVE_ROTATION;
    private float lift = INACTIVE_LIFT;
    private float previousLift = INACTIVE_LIFT;
    private List<SatelliteSavedData.SatelliteInfoLine> infoLines = List.of();

    public SatelliteGroundStationBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.SATELLITE_GROUND_STATION.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SatelliteGroundStationBlockEntity station) {
        if (level.isClientSide) {
            station.tickClient();
        } else if (level instanceof ServerLevel serverLevel) {
            station.tickServer(serverLevel);
        }
    }

    private void tickClient() {
        this.previousRotation = this.rotation;
        this.previousLift = this.lift;
        this.rotation = approach(this.rotation, this.connected ? ACTIVE_ROTATION : INACTIVE_ROTATION);
        this.lift = approach(this.lift, this.connected ? ACTIVE_LIFT : INACTIVE_LIFT);
    }

    private void tickServer(ServerLevel level) {
        // The authored structure occupies Y + 0..6. Checking the first block above it
        // preserves the old transparent-dummy height check without treating our own
        // full collision dummies as an obstruction.
        boolean nextConnected = level.canSeeSky(this.worldPosition.above(7))
                && SatelliteSavedData.get(level).isFrequencyTaken(this.frequency);
        boolean changed = this.connected != nextConnected;
        if (changed) {
            this.connected = nextConnected;
        }
        if (changed || level.getGameTime() % 20L == 0L) {
            List<SatelliteSavedData.SatelliteInfoLine> nextInfo = nextConnected
                    ? SatelliteSavedData.get(level).satellite(this.frequency)
                    .map(record -> record.infoLines(level)).orElse(List.of())
                    : List.of();
            if (!this.infoLines.equals(nextInfo)) {
                this.infoLines = List.copyOf(nextInfo);
                changed = true;
            }
        }
        if (changed) {
            setChanged();
            sync();
        }
    }

    private static float approach(float value, float target) {
        if (Math.abs(value - target) <= SPEED) {
            return target;
        }
        return value < target ? value + SPEED : value - SPEED;
    }

    public boolean connected() {
        return this.connected;
    }

    public int frequency() {
        return this.frequency;
    }

    public void setFrequency(int frequency) {
        if (this.frequency == frequency) {
            return;
        }
        this.frequency = frequency;
        setChanged();
        sync();
    }

    public float rotation(float partialTick) {
        return this.previousRotation + (this.rotation - this.previousRotation) * partialTick;
    }

    public float lift(float partialTick) {
        return this.previousLift + (this.lift - this.previousLift) * partialTick;
    }

    public List<Component> satelliteInfo() {
        if (!this.connected) return List.of();
        return this.infoLines.stream().map(SatelliteSavedData.SatelliteInfoLine::component).toList();
    }

    @Override
    public String[] rttyFunctionInfo() {
        return new String[] {
                RttyInfo.PREFIX_VALUE + "connected",
                RttyInfo.PREFIX_VALUE + "freq",
                RttyInfo.PREFIX_VALUE + "rx",
                RttyInfo.PREFIX_VALUE + "type",
                RttyInfo.PREFIX_FUNCTION + "setfreq" + RttyInteractive.NAME_SEPARATOR + "freq",
                RttyInfo.PREFIX_FUNCTION + "tx" + RttyInteractive.NAME_SEPARATOR + "payload",
                RttyInfo.PREFIX_FUNCTION + "txrx" + RttyInteractive.NAME_SEPARATOR
                        + "return freq" + RttyInteractive.PARAM_SEPARATOR + "payload"
        };
    }

    @Override
    public String provideRttyValue(String name) {
        if (name.equals(RttyInfo.PREFIX_VALUE + "connected")) {
            return this.connected ? "TRUE" : "FALSE";
        }
        if (name.equals(RttyInfo.PREFIX_VALUE + "freq")) {
            return Integer.toString(this.frequency);
        }
        if (!(this.level instanceof ServerLevel serverLevel)) return null;
        SatelliteSavedData.SatelliteRecord record = SatelliteSavedData.get(serverLevel)
                .satellite(this.frequency).orElse(null);
        if (name.equals(RttyInfo.PREFIX_VALUE + "type")) {
            return record == null ? "" : record.type();
        }
        if (name.equals(RttyInfo.PREFIX_VALUE + "rx")) {
            return record == null ? "" : record.tx();
        }
        return null;
    }

    @Override
    public String runRttyFunction(String name, String[] params) {
        if (name.equals(RttyInfo.PREFIX_FUNCTION + "setfreq") && params.length == 1) {
            setFrequency(RttyInteractive.parseInt(params[0], 0, 100_000));
        } else if (name.equals(RttyInfo.PREFIX_FUNCTION + "tx") && params.length > 0) {
            transmit(null, params);
        } else if (name.equals(RttyInfo.PREFIX_FUNCTION + "txrx") && params.length > 1) {
            transmit(params[0], Arrays.copyOfRange(params, 1, params.length));
        }
        return null;
    }

    private void transmit(@Nullable String returnChannel, String[] payload) {
        if (!(this.level instanceof ServerLevel serverLevel)) return;
        SatelliteSavedData data = SatelliteSavedData.get(serverLevel);
        SatelliteSavedData.SatelliteRecord record = data.satellite(this.frequency).orElse(null);
        if (record != null) {
            String[] command = String.join(RttyInteractive.PARAM_SEPARATOR, payload).split(" ");
            record.onCommand(serverLevel, command);
            if (returnChannel != null) {
                HbmRttySystem.broadcast(serverLevel, returnChannel, record.tx());
            }
            data.setDirty();
        }
        SatelliteEventTracker.reportRay(serverLevel, this.worldPosition,
                SatelliteEventTracker.RayEvent.INFO_RADIO, 300);
        setChanged();
        sync();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("freq", this.frequency);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.frequency = tag.getInt("freq");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("freq", this.frequency);
        tag.putBoolean("connected", this.connected);
        ListTag info = new ListTag();
        for (SatelliteSavedData.SatelliteInfoLine line : this.infoLines) {
            CompoundTag entry = new CompoundTag();
            entry.putString("key", line.translationKey());
            entry.putBoolean("hasValue", line.hasValue());
            if (line.hasValue()) entry.putInt("value", line.value());
            info.add(entry);
        }
        tag.put("info", info);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        this.connected = tag.getBoolean("connected");
        ListTag info = tag.getList("info", Tag.TAG_COMPOUND);
        List<SatelliteSavedData.SatelliteInfoLine> decoded = new ArrayList<>(info.size());
        for (int index = 0; index < info.size(); index++) {
            CompoundTag entry = info.getCompound(index);
            String key = entry.getString("key");
            if (key.isBlank()) continue;
            decoded.add(entry.getBoolean("hasValue")
                    ? SatelliteSavedData.SatelliteInfoLine.of(key, entry.getInt("value"))
                    : SatelliteSavedData.SatelliteInfoLine.of(key));
        }
        this.infoLines = List.copyOf(decoded);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void sync() {
        if (this.level != null && !this.level.isClientSide) {
            BlockState state = getBlockState();
            this.level.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }
}
