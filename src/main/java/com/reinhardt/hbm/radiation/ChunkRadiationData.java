package com.reinhardt.hbm.radiation;

import com.reinhardt.hbm.ReinhardtsHBM;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ChunkRadiationData extends SavedData {
    private static final String DATA_NAME = ReinhardtsHBM.MOD_ID + "_radiation";
    private static final SavedData.Factory<ChunkRadiationData> FACTORY = new SavedData.Factory<>(
            ChunkRadiationData::new,
            ChunkRadiationData::load
    );

    private final Map<Long, Double> sections = new HashMap<>();
    /**
     * The solver and world-effects planner only need a stable read view. Keep
     * that view immutable so a tick does not copy the complete radiation map
     * for every consumer.
     */
    private volatile Map<Long, Double> immutableSnapshot = Map.of();
    private volatile Map<Long, Double> chunkSnapshot = Map.of();
    private boolean snapshotsDirty = true;
    private long revision;
    private transient ServerLevel owner;

    public static ChunkRadiationData get(ServerLevel level) {
        ChunkRadiationData data = level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
        data.owner = level;
        if (!data.sections.isEmpty()) {
            HbmRadiationWorlds.markActive(level);
        }
        return data;
    }

    private static ChunkRadiationData load(CompoundTag tag, HolderLookup.Provider registries) {
        ChunkRadiationData data = new ChunkRadiationData();
        ListTag list = tag.getList("sections", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            double radiation = sanitize(entry.getDouble("radiation"));
            if (radiation > 0.0D) {
                data.sections.put(entry.getLong("section"), radiation);
            }
        }
        data.refreshSnapshots();
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<Long, Double> entry : sections.entrySet()) {
            double radiation = sanitize(entry.getValue());
            if (radiation <= 0.0D) {
                continue;
            }
            CompoundTag sectionTag = new CompoundTag();
            sectionTag.putLong("section", entry.getKey());
            sectionTag.putDouble("radiation", radiation);
            list.add(sectionTag);
        }
        tag.put("sections", list);
        return tag;
    }

    public double getRadiation(BlockPos pos) {
        return getRadiation(SectionPos.asLong(pos));
    }

    public double getRadiation(long sectionKey) {
        return sections.getOrDefault(sectionKey, 0.0D);
    }

    public void setRadiation(BlockPos pos, double radiation) {
        setRadiation(SectionPos.asLong(pos), radiation);
    }

    public void setRadiation(long sectionKey, double radiation) {
        double sanitized = sanitize(radiation);
        if (sanitized <= 0.0D) {
            if (sections.remove(sectionKey) != null) {
                snapshotsDirty = true;
                revision++;
                setDirty();
                if (sections.isEmpty() && owner != null) {
                    HbmRadiationWorlds.markInactive(owner);
                }
            }
            return;
        }
        Double previous = sections.put(sectionKey, sanitized);
        if (owner != null) {
            HbmRadiationWorlds.markActive(owner);
        }
        if (previous == null || Double.compare(previous, sanitized) != 0) {
            snapshotsDirty = true;
        }
        if (previous == null || Math.abs(previous - sanitized) > HbmRadiationConstants.RAD_EPSILON) {
            revision++;
            setDirty();
        }
    }

    public void incrementRadiation(BlockPos pos, double amount) {
        incrementRadiation(pos, amount, HbmRadiationConstants.CHUNK_RADIATION_MAX);
    }

    public void incrementRadiation(BlockPos pos, double amount, double max) {
        long sectionKey = SectionPos.asLong(pos);
        double current = getRadiation(sectionKey);
        if (current >= max && amount > 0.0D) {
            return;
        }
        setRadiation(sectionKey, Math.min(max, current + amount));
    }

    public void decrementRadiation(BlockPos pos, double amount) {
        if (amount <= 0.0D) {
            return;
        }
        long sectionKey = SectionPos.asLong(pos);
        setRadiation(sectionKey, Math.max(0.0D, getRadiation(sectionKey) - amount));
    }

    public void clearRadiation(BlockPos pos) {
        setRadiation(pos, 0.0D);
    }

    Map<Long, Double> snapshot() {
        return immutableSnapshot;
    }

    Map<Long, Double> chunkSnapshot() {
        return chunkSnapshot;
    }

    Map<Long, Double> loadedChunkSnapshot(Set<Long> loadedChunks) {
        if (chunkSnapshot.isEmpty()) {
            return Map.of();
        }
        Map<Long, Double> loaded = new HashMap<>();
        for (Map.Entry<Long, Double> entry : chunkSnapshot.entrySet()) {
            if (loadedChunks.contains(entry.getKey())) {
                loaded.put(entry.getKey(), entry.getValue());
            }
        }
        return loaded.isEmpty() ? Map.of() : Map.copyOf(loaded);
    }

    Map<Long, Double> loadedSectionSnapshot(Set<Long> loadedChunks) {
        if (immutableSnapshot.isEmpty() || loadedChunks.isEmpty()) {
            return Map.of();
        }
        Map<Long, Double> loaded = new HashMap<>();
        for (Map.Entry<Long, Double> entry : immutableSnapshot.entrySet()) {
            long sectionKey = entry.getKey();
            long chunkKey = ChunkPos.asLong(SectionPos.x(sectionKey), SectionPos.z(sectionKey));
            if (loadedChunks.contains(chunkKey)) {
                loaded.put(sectionKey, entry.getValue());
            }
        }
        return loaded.isEmpty() ? Map.of() : Map.copyOf(loaded);
    }

    boolean isEmpty() {
        return sections.isEmpty();
    }

    long revision() {
        return revision;
    }

    /**
     * Publishes one immutable read view for the solver and world-effects planner.
     * Radiation producers can write several times in one tick, so rebuilding the
     * complete view inside setRadiation would turn each emission into a full-map copy.
     */
    void refreshSnapshots() {
        if (!snapshotsDirty) {
            return;
        }
        immutableSnapshot = sections.isEmpty() ? Map.of() : Map.copyOf(sections);
        if (sections.isEmpty()) {
            chunkSnapshot = Map.of();
        } else {
            Map<Long, Double> chunks = new HashMap<>();
            for (Map.Entry<Long, Double> entry : sections.entrySet()) {
                long chunkKey = ChunkPos.asLong(SectionPos.x(entry.getKey()), SectionPos.z(entry.getKey()));
                chunks.merge(chunkKey, entry.getValue(), Math::max);
            }
            chunkSnapshot = Map.copyOf(chunks);
        }
        snapshotsDirty = false;
    }

    boolean applySolvedSnapshot(Map<Long, Double> solved, long expectedRevision, Set<Long> updatedSections) {
        if (revision != expectedRevision) {
            return false;
        }

        boolean changed = false;
        for (long sectionKey : updatedSections) {
            double radiation = sanitize(solved.getOrDefault(sectionKey, 0.0D));
            if (radiation > 0.0D) {
                Double previous = sections.put(sectionKey, radiation);
                changed = previous == null || Double.compare(previous, radiation) != 0 || changed;
            } else {
                changed = sections.remove(sectionKey) != null || changed;
            }
        }

        if (changed) {
            snapshotsDirty = true;
            revision++;
            setDirty();
            refreshSnapshots();
        }
        if (sections.isEmpty() && owner != null) {
            HbmRadiationWorlds.markInactive(owner);
        }
        return true;
    }

    private static double sanitize(double value) {
        if (!Double.isFinite(value) || value <= HbmRadiationConstants.RAD_EPSILON) {
            return 0.0D;
        }
        return Math.min(value, HbmRadiationConstants.CHUNK_RADIATION_MAX);
    }
}
