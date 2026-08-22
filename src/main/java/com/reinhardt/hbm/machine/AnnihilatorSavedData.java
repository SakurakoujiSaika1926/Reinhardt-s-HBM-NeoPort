package com.reinhardt.hbm.machine;

import com.reinhardt.hbm.ReinhardtsHBM;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

/**
 * The 1.7.10 annihilator stored its counters per world rather than per block.
 * Pool names retain that behaviour: every annihilator using the same name
 * contributes to and redeems from the same counter set.
 */
public final class AnnihilatorSavedData extends SavedData {
    private static final String DATA_NAME = ReinhardtsHBM.MOD_ID + "_annihilator";
    private static final SavedData.Factory<AnnihilatorSavedData> FACTORY = new SavedData.Factory<>(
            AnnihilatorSavedData::new,
            AnnihilatorSavedData::load
    );

    private final Map<String, Map<String, BigInteger>> pools = new HashMap<>();

    public static AnnihilatorSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public BigInteger count(String pool, String key) {
        return pools.getOrDefault(normalizePool(pool), Map.of()).getOrDefault(key, BigInteger.ZERO);
    }

    public BigInteger increment(String pool, String key, long amount) {
        if (key == null || key.isBlank() || amount <= 0L) {
            return count(pool, key == null ? "" : key);
        }
        Map<String, BigInteger> counters = pools.computeIfAbsent(normalizePool(pool), ignored -> new HashMap<>());
        BigInteger previous = counters.getOrDefault(key, BigInteger.ZERO);
        counters.put(key, previous.add(BigInteger.valueOf(amount)));
        setDirty();
        return previous;
    }

    private static AnnihilatorSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        AnnihilatorSavedData data = new AnnihilatorSavedData();
        ListTag poolList = tag.getList("pools", Tag.TAG_COMPOUND);
        for (int poolIndex = 0; poolIndex < poolList.size(); poolIndex++) {
            CompoundTag poolTag = poolList.getCompound(poolIndex);
            String poolName = normalizePool(poolTag.getString("name"));
            Map<String, BigInteger> counters = data.pools.computeIfAbsent(poolName, ignored -> new HashMap<>());
            ListTag entries = poolTag.getList("entries", Tag.TAG_COMPOUND);
            for (int entryIndex = 0; entryIndex < entries.size(); entryIndex++) {
                CompoundTag entry = entries.getCompound(entryIndex);
                String key = entry.getString("key");
                byte[] amount = entry.getByteArray("amount");
                if (key.isBlank() || amount.length == 0) {
                    continue;
                }
                try {
                    BigInteger value = new BigInteger(amount);
                    if (value.signum() > 0) {
                        counters.put(key, value);
                    }
                } catch (NumberFormatException ignored) {
                    // Preserve the old data class' tolerance for damaged counters.
                }
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag poolList = new ListTag();
        for (Map.Entry<String, Map<String, BigInteger>> pool : pools.entrySet()) {
            CompoundTag poolTag = new CompoundTag();
            poolTag.putString("name", pool.getKey());
            ListTag entries = new ListTag();
            for (Map.Entry<String, BigInteger> entry : pool.getValue().entrySet()) {
                if (entry.getValue().signum() <= 0) {
                    continue;
                }
                CompoundTag entryTag = new CompoundTag();
                entryTag.putString("key", entry.getKey());
                entryTag.putByteArray("amount", entry.getValue().toByteArray());
                entries.add(entryTag);
            }
            poolTag.put("entries", entries);
            poolList.add(poolTag);
        }
        tag.put("pools", poolList);
        return tag;
    }

    private static String normalizePool(String pool) {
        return pool == null || pool.isBlank() ? "Recycling" : pool;
    }
}
