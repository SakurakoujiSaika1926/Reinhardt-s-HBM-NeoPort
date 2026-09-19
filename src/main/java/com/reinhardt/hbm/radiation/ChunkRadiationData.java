package com.reinhardt.hbm.radiation;

import com.reinhardt.hbm.ReinhardtsHBM;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
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

    /** Legacy-style world radiation: one radiation value per X/Z chunk, no Y component. */
    private final Map<Long, Double> chunks = new HashMap<>();
    /**
     * Main-thread emission combiner. Several systems, especially RBMK neutron
     * streaming, may add radiation to the same chunk many times in one game
     * tick. The old gameplay result is additive, but notifying SavedData and the
     * async diffusion worker for every tiny add is pure overhead.
     */
    private final Map<Long, PendingRadiation> pendingIncrements = new HashMap<>();
    private long revision;
    private transient ServerLevel owner;

    public static ChunkRadiationData get(ServerLevel level) {
        ChunkRadiationData data = level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
        data.owner = level;
        if (!data.chunks.isEmpty()) {
            HbmRadiationWorlds.markActive(level);
        }
        return data;
    }

    private static ChunkRadiationData load(CompoundTag tag, HolderLookup.Provider registries) {
        ChunkRadiationData data = new ChunkRadiationData();
        ListTag list = tag.getList("chunks", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            double radiation = sanitize(entry.getDouble("radiation"));
            if (radiation > 0.0D) {
                data.chunks.put(entry.getLong("chunk"), radiation);
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        flushPendingIncrements();
        ListTag list = new ListTag();
        for (Map.Entry<Long, Double> entry : chunks.entrySet()) {
            double radiation = sanitize(entry.getValue());
            if (radiation <= 0.0D) {
                continue;
            }
            CompoundTag chunkTag = new CompoundTag();
            chunkTag.putLong("chunk", entry.getKey());
            chunkTag.putDouble("radiation", radiation);
            list.add(chunkTag);
        }
        tag.put("chunks", list);
        return tag;
    }

    public double getRadiation(BlockPos pos) {
        return getChunkRadiation(chunkKey(pos));
    }

    public double getChunkRadiation(long chunkKey) {
        flushPendingIncrements();
        return chunks.getOrDefault(chunkKey, 0.0D);
    }

    public void setRadiation(BlockPos pos, double radiation) {
        flushPendingIncrements();
        setRadiationImmediate(chunkKey(pos), radiation);
    }

    private void setRadiationImmediate(long chunkKey, double radiation) {
        double sanitized = sanitize(radiation);
        if (sanitized <= 0.0D) {
            if (chunks.remove(chunkKey) != null) {
                revision++;
                setDirty();
                if (chunks.isEmpty() && owner != null) {
                    HbmRadiationWorlds.markInactive(owner);
                }
                if (owner != null) {
                    HbmRadiationWorlds.onRadiationChanged(owner, chunkKey, 0.0D);
                }
            }
            return;
        }
        Double previous = chunks.put(chunkKey, sanitized);
        if (owner != null) {
            HbmRadiationWorlds.markActive(owner);
        }
        boolean changed = previous == null || Math.abs(previous - sanitized) > HbmRadiationConstants.RAD_EPSILON;
        if (changed) {
            revision++;
            setDirty();
        }
        if (owner != null && changed) {
            HbmRadiationWorlds.onRadiationChanged(owner, chunkKey, sanitized);
        }
    }

    public void incrementRadiation(BlockPos pos, double amount) {
        incrementRadiation(pos, amount, HbmRadiationConstants.CHUNK_RADIATION_MAX);
    }

    public void incrementRadiation(BlockPos pos, double amount, double max) {
        if (!Double.isFinite(amount) || amount == 0.0D) {
            return;
        }
        pendingIncrements
                .computeIfAbsent(chunkKey(pos), ignored -> new PendingRadiation())
                .add(amount, sanitizeMax(max));
    }

    public void decrementRadiation(BlockPos pos, double amount) {
        if (amount <= 0.0D) {
            return;
        }
        long chunkKey = chunkKey(pos);
        setRadiationImmediate(chunkKey, Math.max(0.0D, getChunkRadiation(chunkKey) - amount));
    }

    public void clearRadiation(BlockPos pos) {
        setRadiation(pos, 0.0D);
    }

    boolean isEmpty() {
        flushPendingIncrements();
        return chunks.isEmpty();
    }

    long revision() {
        flushPendingIncrements();
        return revision;
    }

    double radiationForChunk(long chunkKey) {
        flushPendingIncrements();
        return chunks.getOrDefault(chunkKey, 0.0D);
    }

    boolean applySolvedSnapshot(Map<Long, Double> solved, long expectedRevision, java.util.Set<Long> updatedChunks) {
        flushPendingIncrements();
        if (revision != expectedRevision) {
            return false;
        }

        boolean changed = false;
        for (long chunkKey : updatedChunks) {
            double radiation = sanitize(solved.getOrDefault(chunkKey, 0.0D));
            if (radiation > 0.0D) {
                Double previous = chunks.put(chunkKey, radiation);
                changed = previous == null || Double.compare(previous, radiation) != 0 || changed;
            } else {
                if (chunks.remove(chunkKey) != null) {
                    changed = true;
                }
            }
        }

        if (changed) {
            revision++;
            setDirty();
        }
        if (chunks.isEmpty() && owner != null) {
            HbmRadiationWorlds.markInactive(owner);
        }
        return true;
    }

    void flushPendingIncrements() {
        if (pendingIncrements.isEmpty()) {
            return;
        }

        Map<Long, Double> changedChunks = new HashMap<>();
        for (Map.Entry<Long, PendingRadiation> entry : pendingIncrements.entrySet()) {
            long chunkKey = entry.getKey();
            double current = chunks.getOrDefault(chunkKey, 0.0D);
            double updated = sanitize(entry.getValue().apply(current));
            if (Double.compare(current, updated) == 0) {
                continue;
            }
            if (updated > 0.0D) {
                chunks.put(chunkKey, updated);
            } else {
                chunks.remove(chunkKey);
            }
            changedChunks.put(chunkKey, updated);
        }
        pendingIncrements.clear();
        if (changedChunks.isEmpty()) {
            return;
        }

        revision++;
        setDirty();
        if (owner != null) {
            if (chunks.isEmpty()) {
                HbmRadiationWorlds.markInactive(owner);
            } else {
                HbmRadiationWorlds.markActive(owner);
            }
            for (Map.Entry<Long, Double> entry : changedChunks.entrySet()) {
                HbmRadiationWorlds.onRadiationChanged(owner, entry.getKey(), entry.getValue());
            }
        }
    }

    private static long chunkKey(BlockPos pos) {
        return new ChunkPos(pos).toLong();
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
