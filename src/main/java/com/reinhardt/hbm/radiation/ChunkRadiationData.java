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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChunkRadiationData extends SavedData {
    private static final String DATA_NAME = ReinhardtsHBM.MOD_ID + "_radiation";
    private static final SavedData.Factory<ChunkRadiationData> FACTORY = new SavedData.Factory<>(
            ChunkRadiationData::new,
            ChunkRadiationData::load
    );

    private final Map<Long, Double> sections = new HashMap<>();
    /** Main-thread index used to seed one loaded chunk without scanning all saved radiation. */
    private final Map<Long, Map<Long, Double>> sectionsByChunk = new HashMap<>();
    /**
     * Main-thread emission combiner.  Several systems, especially RBMK neutron
     * streaming, may add radiation to the same section many times in one game
     * tick.  The old gameplay result is additive, but notifying SavedData and
     * the async diffusion worker for every tiny add is pure overhead.  Queue
     * increments in call order and flush them at stable boundaries.
     */
    private final Map<Long, PendingRadiation> pendingIncrements = new HashMap<>();
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
                data.putLoaded(entry.getLong("section"), radiation);
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        flushPendingIncrements();
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
        flushPendingIncrements();
        return sections.getOrDefault(sectionKey, 0.0D);
    }

    public void setRadiation(BlockPos pos, double radiation) {
        setRadiation(SectionPos.asLong(pos), radiation);
    }

    public void setRadiation(long sectionKey, double radiation) {
        flushPendingIncrements();
        setRadiationImmediate(sectionKey, radiation);
    }

    private void setRadiationImmediate(long sectionKey, double radiation) {
        double sanitized = sanitize(radiation);
        if (sanitized <= 0.0D) {
            if (sections.remove(sectionKey) != null) {
                removeIndexed(sectionKey);
                revision++;
                setDirty();
                if (sections.isEmpty() && owner != null) {
                    HbmRadiationWorlds.markInactive(owner);
                }
                if (owner != null) {
                    HbmRadiationWorlds.onRadiationChanged(owner, sectionKey, 0.0D, revision);
                }
            }
            return;
        }
        Double previous = sections.put(sectionKey, sanitized);
        index(sectionKey, sanitized);
        if (owner != null) {
            HbmRadiationWorlds.markActive(owner);
        }
        boolean changed = previous == null || Math.abs(previous - sanitized) > HbmRadiationConstants.RAD_EPSILON;
        if (changed) {
            revision++;
            setDirty();
        }
        if (owner != null && changed) {
            HbmRadiationWorlds.onRadiationChanged(owner, sectionKey, sanitized, revision);
        }
    }

    public void incrementRadiation(BlockPos pos, double amount) {
        incrementRadiation(pos, amount, HbmRadiationConstants.CHUNK_RADIATION_MAX);
    }

    public void incrementRadiation(BlockPos pos, double amount, double max) {
        if (!Double.isFinite(amount) || amount == 0.0D) {
            return;
        }
        long sectionKey = SectionPos.asLong(pos);
        pendingIncrements
                .computeIfAbsent(sectionKey, ignored -> new PendingRadiation())
                .add(amount, sanitizeMax(max));
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

    boolean isEmpty() {
        flushPendingIncrements();
        return sections.isEmpty();
    }

    long revision() {
        flushPendingIncrements();
        return revision;
    }

    Map<Long, Double> sectionsForChunk(long chunkKey) {
        flushPendingIncrements();
        return Map.copyOf(sectionsByChunk.getOrDefault(chunkKey, Map.of()));
    }

    boolean applySolvedSnapshot(Map<Long, Double> solved, long expectedRevision, java.util.Set<Long> updatedSections) {
        flushPendingIncrements();
        if (revision != expectedRevision) {
            return false;
        }

        boolean changed = false;
        for (long sectionKey : updatedSections) {
            double radiation = sanitize(solved.getOrDefault(sectionKey, 0.0D));
            if (radiation > 0.0D) {
                Double previous = sections.put(sectionKey, radiation);
                index(sectionKey, radiation);
                changed = previous == null || Double.compare(previous, radiation) != 0 || changed;
            } else {
                if (sections.remove(sectionKey) != null) {
                    removeIndexed(sectionKey);
                    changed = true;
                }
            }
        }

        if (changed) {
            revision++;
            setDirty();
        }
        if (sections.isEmpty() && owner != null) {
            HbmRadiationWorlds.markInactive(owner);
        }
        return true;
    }

    void flushPendingIncrements() {
        if (pendingIncrements.isEmpty()) {
            return;
        }

        Map<Long, Double> changedSections = new HashMap<>();
        for (Map.Entry<Long, PendingRadiation> entry : pendingIncrements.entrySet()) {
            long sectionKey = entry.getKey();
            double current = sections.getOrDefault(sectionKey, 0.0D);
            double updated = entry.getValue().apply(current);
            updated = sanitize(updated);
            if (Double.compare(current, updated) == 0) {
                continue;
            }
            if (updated > 0.0D) {
                sections.put(sectionKey, updated);
                index(sectionKey, updated);
            } else {
                sections.remove(sectionKey);
                removeIndexed(sectionKey);
            }
            changedSections.put(sectionKey, updated);
        }
        pendingIncrements.clear();
        if (changedSections.isEmpty()) {
            return;
        }

        revision++;
        setDirty();
        if (owner != null) {
            if (sections.isEmpty()) {
                HbmRadiationWorlds.markInactive(owner);
            } else {
                HbmRadiationWorlds.markActive(owner);
            }
            for (Map.Entry<Long, Double> entry : changedSections.entrySet()) {
                HbmRadiationWorlds.onRadiationChanged(owner, entry.getKey(), entry.getValue(), revision);
            }
        }
    }

    private void putLoaded(long sectionKey, double radiation) {
        sections.put(sectionKey, radiation);
        index(sectionKey, radiation);
    }

    private void index(long sectionKey, double radiation) {
        long chunkKey = ChunkPos.asLong(SectionPos.x(sectionKey), SectionPos.z(sectionKey));
        sectionsByChunk.computeIfAbsent(chunkKey, ignored -> new HashMap<>()).put(sectionKey, radiation);
    }

    private void removeIndexed(long sectionKey) {
        long chunkKey = ChunkPos.asLong(SectionPos.x(sectionKey), SectionPos.z(sectionKey));
        Map<Long, Double> chunkSections = sectionsByChunk.get(chunkKey);
        if (chunkSections == null) {
            return;
        }
        chunkSections.remove(sectionKey);
        if (chunkSections.isEmpty()) {
            sectionsByChunk.remove(chunkKey);
        }
    }

    private static double sanitize(double value) {
        if (!Double.isFinite(value) || value <= HbmRadiationConstants.RAD_EPSILON) {
            return 0.0D;
        }
        return Math.min(value, HbmRadiationConstants.CHUNK_RADIATION_MAX);
    }

    private static double sanitizeMax(double max) {
        if (!Double.isFinite(max) || max <= 0.0D) {
            return HbmRadiationConstants.CHUNK_RADIATION_MAX;
        }
        return Math.min(max, HbmRadiationConstants.CHUNK_RADIATION_MAX);
    }

    private static final class PendingRadiation {
        private final List<Increment> increments = new ArrayList<>(1);

        void add(double amount, double max) {
            int lastIndex = increments.size() - 1;
            if (lastIndex >= 0) {
                Increment last = increments.get(lastIndex);
                if (Double.compare(last.max(), max) == 0) {
                    increments.set(lastIndex, new Increment(last.amount() + amount, max));
                    return;
                }
            }
            increments.add(new Increment(amount, max));
        }

        double apply(double base) {
            double current = base;
            for (Increment increment : increments) {
                if (current >= increment.max() && increment.amount() > 0.0D) {
                    continue;
                }
                current = Math.min(increment.max(), current + increment.amount());
            }
            return current;
        }
    }

    private record Increment(double amount, double max) {
    }
}
