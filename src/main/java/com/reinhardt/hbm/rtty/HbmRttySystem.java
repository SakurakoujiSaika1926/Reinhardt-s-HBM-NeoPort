package com.reinhardt.hbm.rtty;

import com.reinhardt.hbm.ReinhardtsHBM;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side port of 1.7.10's RTTYSystem. Signals written during a tick are
 * published after the level tick, making them visible to listeners next tick.
 */
@EventBusSubscriber(modid = ReinhardtsHBM.MOD_ID)
public final class HbmRttySystem {
    private static final Map<ResourceKey<Level>, Map<String, Channel>> BROADCASTS = new ConcurrentHashMap<>();
    private static final Map<ResourceKey<Level>, Map<String, String>> NEW_MESSAGES = new ConcurrentHashMap<>();

    private HbmRttySystem() {
    }

    public static void broadcast(Level level, String channelName, Object signal) {
        if (level.isClientSide || channelName == null || channelName.isEmpty()) {
            return;
        }

        String value = String.valueOf(signal);
        Map<String, String> queue = NEW_MESSAGES.computeIfAbsent(level.dimension(), ignored -> new HashMap<>());
        String existing = queue.get(channelName);
        if (existing != null && isNumeric(existing) && isNumeric(value)) {
            try {
                queue.put(channelName, Long.toString(Math.addExact(Long.parseLong(existing), Long.parseLong(value))));
                return;
            } catch (NumberFormatException | ArithmeticException ignored) {
                // 1.7.10 silently falls back to replacing the queued signal.
            }
        }
        queue.put(channelName, value);
    }

    public static Channel listen(Level level, String channelName) {
        if (channelName == null || channelName.isEmpty()) {
            return null;
        }
        return BROADCASTS.getOrDefault(level.dimension(), Map.of()).get(channelName);
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        ResourceKey<Level> dimension = level.dimension();
        Map<String, String> queued = NEW_MESSAGES.remove(dimension);
        if (queued == null || queued.isEmpty()) {
            return;
        }

        Map<String, Channel> broadcasts = BROADCASTS.computeIfAbsent(dimension, ignored -> new HashMap<>());
        long timestamp = level.getGameTime();
        for (Map.Entry<String, String> entry : queued.entrySet()) {
            broadcasts.put(entry.getKey(), new Channel(timestamp, entry.getValue()));
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            BROADCASTS.remove(level.dimension());
            NEW_MESSAGES.remove(level.dimension());
        }
    }

    private static boolean isNumeric(String value) {
        try {
            Long.parseLong(value);
            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    public record Channel(long timestamp, String signal) {
    }
}
