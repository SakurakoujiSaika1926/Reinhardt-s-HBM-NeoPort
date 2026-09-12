package com.reinhardt.hbm.integration.sable;

import com.reinhardt.hbm.ReinhardtsHBM;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Soft Sable/Aeronautics bridge for HBM's own HE network.
 *
 * <p>Aeronautics stores moving vehicles in Sable sublevel plots inside the
 * same Minecraft dimension. HBM's power solver must therefore separate those
 * plot-local blocks from the ordinary dimension-wide cache without making
 * Sable a hard dependency.</p>
 */
public final class HbmSablePowerCompat {
    private static final String WORLD_POWER_SPACE = "";
    private static final Adapter ADAPTER = Adapter.tryCreate();
    private static final Map<Level, TickCache> CACHES = Collections.synchronizedMap(new WeakHashMap<>());

    private HbmSablePowerCompat() {
    }

    public static boolean isSubLevelBlock(LevelAccessor level, BlockPos pos) {
        return powerSpaceId(level, pos) != null;
    }

    public static boolean samePowerSpace(LevelAccessor level, BlockPos first, BlockPos second) {
        return Objects.equals(powerSpaceId(level, first), powerSpaceId(level, second));
    }

    @Nullable
    public static String powerSpaceId(LevelAccessor level, BlockPos pos) {
        if (!(level instanceof Level realLevel) || ADAPTER == null || !isSableLoaded()) {
            return null;
        }
        TickCache cache;
        synchronized (CACHES) {
            cache = CACHES.computeIfAbsent(realLevel, ignored -> new TickCache());
        }
        return cache.powerSpaceId(realLevel, pos);
    }

    private static boolean isSableLoaded() {
        try {
            return ModList.get().isLoaded("sable");
        } catch (IllegalStateException ignored) {
            return true;
        }
    }

    private static final class TickCache {
        private final Map<Long, String> values = new HashMap<>();
        private long gameTime = Long.MIN_VALUE;

        @Nullable
        synchronized String powerSpaceId(Level level, BlockPos pos) {
            long now = level.getGameTime();
            if (now != this.gameTime) {
                this.values.clear();
                this.gameTime = now;
            }

            long key = pos.asLong();
            String cached = this.values.get(key);
            if (cached != null) {
                return WORLD_POWER_SPACE.equals(cached) ? null : cached;
            }

            String value = ADAPTER.powerSpaceId(level, pos);
            this.values.put(key, value == null ? WORLD_POWER_SPACE : value);
            return value;
        }
    }

    private static final class Adapter {
        private final Method getContainer;
        private final Method inBounds;
        private final Method getPlot;
        private final Method getSubLevel;
        private final Method getUniqueId;
        private final Field plotPos;
        private final AtomicBoolean warnedRuntimeFailure = new AtomicBoolean();

        private Adapter(
                Method getContainer,
                Method inBounds,
                Method getPlot,
                Method getSubLevel,
                Method getUniqueId,
                Field plotPos
        ) {
            this.getContainer = getContainer;
            this.inBounds = inBounds;
            this.getPlot = getPlot;
            this.getSubLevel = getSubLevel;
            this.getUniqueId = getUniqueId;
            this.plotPos = plotPos;
        }

        @Nullable
        static Adapter tryCreate() {
            try {
                Class<?> containerClass = Class.forName("dev.ryanhcode.sable.api.sublevel.SubLevelContainer");
                Class<?> plotClass = Class.forName("dev.ryanhcode.sable.sublevel.plot.LevelPlot");
                Class<?> subLevelClass = Class.forName("dev.ryanhcode.sable.sublevel.SubLevel");
                Method getContainer = containerClass.getMethod("getContainer", Level.class);
                Method inBounds = containerClass.getMethod("inBounds", BlockPos.class);
                Method getPlot = containerClass.getMethod("getPlot", ChunkPos.class);
                Method getSubLevel = plotClass.getMethod("getSubLevel");
                Method getUniqueId = subLevelClass.getMethod("getUniqueId");
                Field plotPos = plotClass.getField("plotPos");
                return new Adapter(getContainer, inBounds, getPlot, getSubLevel, getUniqueId, plotPos);
            } catch (ReflectiveOperationException | LinkageError ignored) {
                return null;
            }
        }

        @Nullable
        String powerSpaceId(Level level, BlockPos pos) {
            try {
                Object container = getContainer.invoke(null, level);
                if (container == null) {
                    return null;
                }
                Object inside = inBounds.invoke(container, pos);
                if (!(inside instanceof Boolean inBoundsValue) || !inBoundsValue) {
                    return null;
                }

                ChunkPos chunkPos = new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4);
                Object plot = getPlot.invoke(container, chunkPos);
                if (plot == null) {
                    return null;
                }

                Object subLevel = getSubLevel.invoke(plot);
                Object uniqueId = subLevel == null ? null : getUniqueId.invoke(subLevel);
                if (uniqueId instanceof UUID uuid) {
                    return "sable:" + uuid;
                }
                if (uniqueId != null) {
                    return "sable:" + uniqueId;
                }

                Object fallbackPlotPos = plotPos.get(plot);
                if (fallbackPlotPos instanceof ChunkPos fallback) {
                    return "sable-plot:" + fallback.x + "," + fallback.z;
                }
                return "sable-plot:" + System.identityHashCode(plot);
            } catch (ReflectiveOperationException | RuntimeException exception) {
                if (warnedRuntimeFailure.compareAndSet(false, true)) {
                    ReinhardtsHBM.LOGGER.warn("Failed to query Sable sublevel power-space; HBM will fall back to dimension-local power networking", exception);
                }
                return null;
            }
        }
    }
}
