package com.reinhardt.hbm.util;

import com.reinhardt.hbm.ReinhardtsHBM;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reports failures at third-party capability boundaries without allowing a
 * broken provider to flood the log every server tick.  This class does not
 * hide HBM failures: callers catch only the individual external capability
 * invocation and fail that one transfer closed.
 */
@EventBusSubscriber(modid = ReinhardtsHBM.MOD_ID)
public final class ExternalCapabilitySafety {
    private static final long LOG_INTERVAL_TICKS = 200L;
    private static final Map<FailureKey, Long> NEXT_LOG_TICK = new ConcurrentHashMap<>();

    private ExternalCapabilitySafety() {
    }

    public static void report(Level level, BlockPos pos, @Nullable Direction side,
                              String capability, String operation, Throwable failure) {
        long gameTime = level.getGameTime();
        FailureKey key = new FailureKey(
                System.identityHashCode(level),
                level.dimension(),
                pos.asLong(),
                side == null ? -1 : side.ordinal(),
                capability
        );

        Long nextLogTick = NEXT_LOG_TICK.get(key);
        if (nextLogTick != null && gameTime < nextLogTick) {
            return;
        }
        long next = gameTime + LOG_INTERVAL_TICKS;
        if (nextLogTick == null) {
            if (NEXT_LOG_TICK.putIfAbsent(key, next) != null) {
                return;
            }
        } else if (!NEXT_LOG_TICK.replace(key, nextLogTick, next)) {
            return;
        }

        ReinhardtsHBM.LOGGER.error(
                "External {} capability failed during {} at {} side {} in {}; this endpoint is ignored for the current operation "
                        + "(repeated reports are suppressed for {} ticks)",
                capability,
                operation,
                pos,
                side == null ? "unsided" : side,
                level.dimension().location(),
                LOG_INTERVAL_TICKS,
                failure
        );
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (!(event.getLevel() instanceof Level level)) {
            return;
        }
        int levelIdentity = System.identityHashCode(level);
        NEXT_LOG_TICK.keySet().removeIf(key -> key.levelIdentity() == levelIdentity);
    }

    private record FailureKey(int levelIdentity, ResourceKey<Level> dimension, long packedPos,
                              int sideOrdinal, String capability) {
    }
}
