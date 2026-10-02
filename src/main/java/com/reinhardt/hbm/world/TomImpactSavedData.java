package com.reinhardt.hbm.world;

import com.reinhardt.hbm.ReinhardtsHBM;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Persistent per-dimension state used by the latest 1.7.10 TOM aftermath. */
public final class TomImpactSavedData extends SavedData {
    private static final String DATA_NAME = ReinhardtsHBM.MOD_ID + "_impact";
    private static final Factory<TomImpactSavedData> FACTORY = new Factory<>(
            TomImpactSavedData::new, TomImpactSavedData::load);
    private static final float DUST_SETTLE_PER_TICK = 1.0F / 14_400_000.0F;
    private static final float FIRE_COOL_PER_TICK = 1.0F / 24_000.0F;

    /**
     * World generation runs on worker threads in modern Minecraft.  Keep a
     * read-only live lookup so features never touch DimensionDataStorage's
     * unsynchronised HashMap from a generation worker.
     */
    private static final ConcurrentMap<ServerLevel, TomImpactSavedData> LIVE = new ConcurrentHashMap<>();

    private volatile float dust;
    private volatile float fire;
    private volatile boolean impact;

    public static TomImpactSavedData get(ServerLevel level) {
        TomImpactSavedData data = level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
        LIVE.put(level, data);
        return data;
    }

    /** Worker-safe lookup; callers must not mutate the returned data. */
    @Nullable
    public static TomImpactSavedData cached(ServerLevel level) {
        return LIVE.get(level);
    }

    public static void release(ServerLevel level) {
        LIVE.remove(level);
    }

    private static TomImpactSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        TomImpactSavedData data = new TomImpactSavedData();
        data.dust = Mth.clamp(tag.getFloat("dust"), 0.0F, 1.0F);
        data.fire = Mth.clamp(tag.getFloat("fire"), 0.0F, 1.0F);
        data.impact = tag.getBoolean("impact");
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putFloat("dust", dust);
        tag.putFloat("fire", fire);
        tag.putBoolean("impact", impact);
        return tag;
    }

    public void beginImpact() {
        impact = true;
        fire = 1.0F;
        setDirty();
    }

    /** Advances the exact old one-day fire / 600-day dust state machine. */
    public void tickAftermath() {
        float previousDust = dust;
        float previousFire = fire;
        if (dust > 0.0F && fire == 0.0F) {
            dust = Math.max(0.0F, dust - DUST_SETTLE_PER_TICK);
        }
        if (fire > 0.0F) {
            fire = Math.max(0.0F, fire - FIRE_COOL_PER_TICK);
            dust = Math.min(1.0F, dust + FIRE_COOL_PER_TICK);
        }
        if (dust != previousDust || fire != previousFire) setDirty();
    }

    public float dust() { return dust; }
    public float fire() { return fire; }
    public boolean impact() { return impact; }
}
