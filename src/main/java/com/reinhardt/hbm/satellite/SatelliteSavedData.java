package com.reinhardt.hbm.satellite;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.advancement.HbmAdvancements;
import com.reinhardt.hbm.item.SatelliteChipItem;
import com.reinhardt.hbm.registry.HbmItems;
import com.reinhardt.hbm.explosion.NukeExplosionManager;
import com.reinhardt.hbm.entity.LegacyShrapnelEntity;
import com.reinhardt.hbm.entity.LegacyTomEntity;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Per-world satellite registry, matching the 1.7.10 {@code satellites} WorldSavedData. */
public final class SatelliteSavedData extends SavedData {
    private static final String DATA_NAME = ReinhardtsHBM.MOD_ID + "_satellites";
    private static final SavedData.Factory<SatelliteSavedData> FACTORY = new SavedData.Factory<>(
            SatelliteSavedData::new,
            SatelliteSavedData::load
    );

    private final Map<Integer, SatelliteRecord> satellites = new HashMap<>();

    public static SatelliteSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public boolean isFrequencyTaken(int frequency) {
        return this.satellites.containsKey(frequency);
    }

    public Optional<SatelliteRecord> satellite(int frequency) {
        return Optional.ofNullable(this.satellites.get(frequency));
    }

    /** TileEntityMachineSatDock stores the last miner-cargo dispatch on the satellite itself. */
    public boolean markMinerDelivery(int frequency, long timestamp) {
        SatelliteRecord record = this.satellites.get(frequency);
        if (record == null || (record.kind != SatelliteKind.MINER && record.kind != SatelliteKind.LUNAR_MINER)) {
            return false;
        }
        record.lastOperation = timestamp;
        setDirty();
        return true;
    }

    /** Execute the coordinate action exposed by ItemSatDesignator. */
    public boolean performCoordinateAction(ServerLevel level, ServerPlayer player, SatelliteRecord record, BlockPos target) {
        if (record.kind == SatelliteKind.RESONATOR) {
            player.stopRiding();
            player.teleportTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D);
            level.playSound(null, target, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
            return true;
        }
        if (record.kind == SatelliteKind.HORIZONS && !record.used) {
            record.used = true;
            record.lastOperation = level.getGameTime();
            setDirty();
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "message.reinhardtshbm.satellite.horizons"), false);
            LegacyTomEntity.spawn(level, target.getX(), target.getZ());
            HbmAdvancements.awardAll(level, "horizons_end");
            return true;
        }
        return false;
    }

    /** Execute the old laser satellite's panel click cooldown. */
    public boolean performPanelAction(ServerLevel level, SatelliteRecord record, BlockPos target) {
        if (record.kind != SatelliteKind.LASER || record.lastOperation + 10_000L > System.currentTimeMillis()) {
            return false;
        }
        record.lastOperation = System.currentTimeMillis();
        setDirty();
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, target.getX(), target.getZ());
        double x = target.getX() + 0.5D, z = target.getZ() + 0.5D;
        // SatelliteLaser uses the dedicated EntityDeathBlast sequence in 1.7.10:
        // delayed MK5 no-radiation core followed by a radial bolt burst.
        NukeExplosionManager.scheduleMk5NoRadiation(level, x, y, z, 40);
        for (int i = 0; i < 100; i++) {
            double angle = Math.PI * 2.0D * i / 100.0D;
            level.addFreshEntity(new LegacyShrapnelEntity(level, x, y + 2.0D, z,
                    new Vec3(Math.cos(angle) * 0.2D, -0.01D, Math.sin(angle) * 0.2D), false));
        }
        return true;
    }

    /** Satellite.orbit: a newly launched payload replaces an existing frequency. */
    public boolean orbit(ServerLevel level, ItemStack payload) {
        SatelliteKind kind = SatelliteKind.fromItem(payload);
        if (kind == null) {
            return false;
        }
        this.satellites.put(SatelliteChipItem.frequency(payload), new SatelliteRecord(kind));
        setDirty();
        if (kind == SatelliteKind.RELAY) {
            HbmAdvancements.awardAll(level, "foeq");
        } else if (kind == SatelliteKind.HORIZONS) {
            HbmAdvancements.awardAll(level, "horizons_start");
        }
        return true;
    }

    private static SatelliteSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        SatelliteSavedData data = new SatelliteSavedData();
        ListTag entries = tag.getList("satellites", Tag.TAG_COMPOUND);
        for (int index = 0; index < entries.size(); index++) {
            CompoundTag entry = entries.getCompound(index);
            SatelliteKind kind = SatelliteKind.byId(entry.getString("kind"));
            if (kind != null) {
                data.satellites.put(entry.getInt("frequency"), SatelliteRecord.load(kind, entry));
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag entries = new ListTag();
        for (Map.Entry<Integer, SatelliteRecord> entry : this.satellites.entrySet()) {
            CompoundTag satelliteTag = new CompoundTag();
            satelliteTag.putInt("frequency", entry.getKey());
            entry.getValue().save(satelliteTag);
            entries.add(satelliteTag);
        }
        tag.put("satellites", entries);
        return tag;
    }

    /** Stable 1.7.10 Satellite.register() order and item mapping. */
    public enum SatelliteKind {
        MAPPER("mapper"),
        SCANNER("scanner"),
        RADAR("radar"),
        LASER("laser"),
        RESONATOR("resonator"),
        RELAY("relay"),
        MINER("miner"),
        LUNAR_MINER("lunar_miner"),
        HORIZONS("horizons");

        private final String id;

        SatelliteKind(String id) {
            this.id = id;
        }

        private static SatelliteKind byId(String id) {
            for (SatelliteKind kind : values()) {
                if (kind.id.equals(id)) return kind;
            }
            return null;
        }

        public static SatelliteKind fromItem(ItemStack stack) {
            if (stack.is(HbmItems.SAT_MAPPER.get())) return MAPPER;
            if (stack.is(HbmItems.SAT_SCANNER.get())) return SCANNER;
            if (stack.is(HbmItems.SAT_RADAR.get())) return RADAR;
            if (stack.is(HbmItems.SAT_LASER.get())) return LASER;
            if (stack.is(HbmItems.SAT_RESONATOR.get())) return RESONATOR;
            // 1.7.10 maps the FOEQ payload to SatelliteRelay. sat_relay is a
            // separately craftable radar relay module, not a launch payload.
            if (stack.is(HbmItems.SAT_FOEQ.get())) return RELAY;
            if (stack.is(HbmItems.SAT_MINER.get())) return MINER;
            if (stack.is(HbmItems.SAT_LUNAR_MINER.get())) return LUNAR_MINER;
            if (stack.is(HbmItems.SAT_GERALD.get())) return HORIZONS;
            return null;
        }
    }

    /** State serialized by the mutable 1.7.10 satellite subclasses. */
    public static final class SatelliteRecord {
        private final SatelliteKind kind;
        private long lastOperation;
        private boolean used;

        private SatelliteRecord(SatelliteKind kind) {
            this.kind = kind;
        }

        private static SatelliteRecord load(SatelliteKind kind, CompoundTag tag) {
            SatelliteRecord record = new SatelliteRecord(kind);
            record.lastOperation = tag.getLong("lastOperation");
            record.used = tag.getBoolean("used");
            return record;
        }

        private void save(CompoundTag tag) {
            tag.putString("kind", this.kind.id);
            tag.putLong("lastOperation", this.lastOperation);
            tag.putBoolean("used", this.used);
        }

        public SatelliteKind kind() {
            return this.kind;
        }

        public long lastOperation() {
            return this.lastOperation;
        }

        public boolean used() {
            return this.used;
        }
    }
}
