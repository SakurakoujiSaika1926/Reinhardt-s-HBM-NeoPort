package com.reinhardt.hbm.radiation;

import com.reinhardt.hbm.ReinhardtsHBM;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ChunkRadiationData extends SavedData {
    private static final String DATA_NAME = ReinhardtsHBM.MOD_ID + "_radiation";
    private static final SavedData.Factory<ChunkRadiationData> FACTORY = new SavedData.Factory<>(
            ChunkRadiationData::new,
            ChunkRadiationData::load
    );

    private final Map<Long, Double> sections = new HashMap<>();
    private long revision;

    public static ChunkRadiationData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
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
                revision++;
                setDirty();
            }
            return;
        }
        Double previous = sections.put(sectionKey, sanitized);
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

    public void clearRadiation(BlockPos pos) {
        setRadiation(pos, 0.0D);
    }

    Map<Long, Double> snapshot() {
        return Collections.unmodifiableMap(new HashMap<>(sections));
    }

    long revision() {
        return revision;
    }

    boolean applySolvedSnapshot(Map<Long, Double> solved, long expectedRevision) {
        if (revision != expectedRevision) {
            return false;
        }

        Map<Long, Double> filtered = new HashMap<>();
        for (Map.Entry<Long, Double> entry : solved.entrySet()) {
            double radiation = sanitize(entry.getValue());
            if (radiation > 0.0D) {
                filtered.put(entry.getKey(), radiation);
            }
        }
        if (!sections.equals(filtered)) {
            sections.clear();
            sections.putAll(filtered);
            revision++;
            setDirty();
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
