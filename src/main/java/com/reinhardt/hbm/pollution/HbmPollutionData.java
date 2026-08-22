package com.reinhardt.hbm.pollution;

import com.reinhardt.hbm.ReinhardtsHBM;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class HbmPollutionData extends SavedData {
    private static final String DATA_NAME = ReinhardtsHBM.MOD_ID + "_pollution";
    private static final SavedData.Factory<HbmPollutionData> FACTORY = new SavedData.Factory<>(
            HbmPollutionData::new,
            HbmPollutionData::load
    );

    private final Map<Long, PollutionValues> regions = new HashMap<>();
    private long revision;

    public static HbmPollutionData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    private static HbmPollutionData load(CompoundTag tag, HolderLookup.Provider registries) {
        HbmPollutionData data = new HbmPollutionData();
        ListTag list = tag.getList("regions", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            PollutionValues values = PollutionValues.load(entry);
            if (!values.isEmpty()) {
                data.regions.put(key(entry.getInt("x"), entry.getInt("z")), values);
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<Long, PollutionValues> entry : regions.entrySet()) {
            PollutionValues values = entry.getValue().sanitized();
            if (values.isEmpty()) {
                continue;
            }
            CompoundTag regionTag = new CompoundTag();
            regionTag.putInt("x", regionX(entry.getKey()));
            regionTag.putInt("z", regionZ(entry.getKey()));
            values.save(regionTag);
            list.add(regionTag);
        }
        tag.put("regions", list);
        return tag;
    }

    public double get(BlockPos pos, HbmPollutionType type) {
        return get(regionKey(pos), type);
    }

    public double get(long regionKey, HbmPollutionType type) {
        PollutionValues values = regions.get(regionKey);
        return values == null ? 0.0D : values.get(type);
    }

    public PollutionValues get(BlockPos pos) {
        PollutionValues values = regions.get(regionKey(pos));
        return values == null ? PollutionValues.EMPTY : values.copy();
    }

    public void set(BlockPos pos, HbmPollutionType type, double amount) {
        set(regionKey(pos), type, amount);
    }

    public void set(long regionKey, HbmPollutionType type, double amount) {
        double sanitized = sanitize(amount);
        PollutionValues values = regions.computeIfAbsent(regionKey, key -> new PollutionValues());
        double previous = values.get(type);
        values.set(type, sanitized);
        if (values.isEmpty()) {
            regions.remove(regionKey);
        }
        if (Math.abs(previous - sanitized) > HbmPollutionConstants.EPSILON) {
            revision++;
            setDirty();
        }
    }

    public void increment(BlockPos pos, HbmPollutionType type, double amount, double multiplier) {
        if (amount == 0.0D || !Double.isFinite(amount)) {
            return;
        }
        long key = regionKey(pos);
        set(key, type, get(key, type) + amount * multiplier);
    }

    public void clear(BlockPos pos) {
        long key = regionKey(pos);
        if (regions.remove(key) != null) {
            revision++;
            setDirty();
        }
    }

    Map<Long, PollutionValues> snapshot() {
        Map<Long, PollutionValues> copy = new HashMap<>();
        for (Map.Entry<Long, PollutionValues> entry : regions.entrySet()) {
            copy.put(entry.getKey(), entry.getValue().copy());
        }
        return Collections.unmodifiableMap(copy);
    }

    boolean applySolvedSnapshot(Map<Long, PollutionValues> baseline, Map<Long, PollutionValues> solved) {
        Map<Long, double[]> mergedRaw = new HashMap<>();
        for (Map.Entry<Long, PollutionValues> entry : solved.entrySet()) {
            PollutionValues values = entry.getValue().sanitized();
            if (!values.isEmpty()) {
                mergedRaw.put(entry.getKey(), values.rawCopy());
            }
        }

        for (Map.Entry<Long, PollutionValues> entry : regions.entrySet()) {
            long key = entry.getKey();
            PollutionValues current = entry.getValue();
            PollutionValues original = baseline.getOrDefault(key, PollutionValues.EMPTY);
            if (current.equals(original)) {
                continue;
            }
            double[] target = mergedRaw.computeIfAbsent(key, unused -> new double[HbmPollutionType.values().length]);
            for (HbmPollutionType type : HbmPollutionType.values()) {
                target[type.ordinal()] += current.get(type) - original.get(type);
            }
        }
        for (Map.Entry<Long, PollutionValues> entry : baseline.entrySet()) {
            long key = entry.getKey();
            if (regions.containsKey(key)) {
                continue;
            }
            double[] target = mergedRaw.get(key);
            if (target == null) {
                continue;
            }
            PollutionValues original = entry.getValue();
            for (HbmPollutionType type : HbmPollutionType.values()) {
                target[type.ordinal()] -= original.get(type);
            }
        }

        Map<Long, PollutionValues> merged = new HashMap<>();
        for (Map.Entry<Long, double[]> entry : mergedRaw.entrySet()) {
            PollutionValues values = PollutionValues.fromRaw(entry.getValue());
            if (!values.isEmpty()) {
                merged.put(entry.getKey(), values);
            }
        }

        if (!regions.equals(merged)) {
            regions.clear();
            regions.putAll(merged);
            revision++;
            setDirty();
        }
        return true;
    }

    static long regionKey(BlockPos pos) {
        return key(pos.getX() >> HbmPollutionConstants.REGION_SHIFT, pos.getZ() >> HbmPollutionConstants.REGION_SHIFT);
    }

    static long key(int x, int z) {
        return ((long) x & 0xffffffffL) | (((long) z & 0xffffffffL) << 32);
    }

    static int regionX(long key) {
        return (int) key;
    }

    static int regionZ(long key) {
        return (int) (key >> 32);
    }

    private static double sanitize(double value) {
        if (!Double.isFinite(value) || value <= HbmPollutionConstants.EPSILON) {
            return 0.0D;
        }
        return Mth.clamp(value, 0.0D, HbmPollutionConstants.MAX_POLLUTION);
    }

    public static final class PollutionValues {
        private static final PollutionValues EMPTY = new PollutionValues();
        private final double[] values = new double[HbmPollutionType.values().length];

        public double get(HbmPollutionType type) {
            return values[type.ordinal()];
        }

        public void set(HbmPollutionType type, double value) {
            values[type.ordinal()] = sanitize(value);
        }

        public void add(HbmPollutionType type, double value) {
            set(type, get(type) + value);
        }

        public boolean isEmpty() {
            for (double value : values) {
                if (value > HbmPollutionConstants.EPSILON) {
                    return false;
                }
            }
            return true;
        }

        PollutionValues sanitized() {
            PollutionValues copy = new PollutionValues();
            for (HbmPollutionType type : HbmPollutionType.values()) {
                copy.set(type, get(type));
            }
            return copy;
        }

        PollutionValues copy() {
            PollutionValues copy = new PollutionValues();
            System.arraycopy(values, 0, copy.values, 0, values.length);
            return copy;
        }

        double[] rawCopy() {
            return Arrays.copyOf(values, values.length);
        }

        static PollutionValues fromRaw(double[] raw) {
            PollutionValues values = new PollutionValues();
            for (HbmPollutionType type : HbmPollutionType.values()) {
                values.set(type, raw[type.ordinal()]);
            }
            return values;
        }

        static PollutionValues load(CompoundTag tag) {
            PollutionValues values = new PollutionValues();
            for (HbmPollutionType type : HbmPollutionType.values()) {
                values.set(type, tag.getDouble(type.name().toLowerCase(Locale.ROOT)));
            }
            return values;
        }

        void save(CompoundTag tag) {
            for (HbmPollutionType type : HbmPollutionType.values()) {
                tag.putDouble(type.name().toLowerCase(Locale.ROOT), get(type));
            }
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof PollutionValues other && Arrays.equals(values, other.values);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(values);
        }
    }
}
