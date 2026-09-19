package com.reinhardt.hbm.radiation;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public final class HbmRadiationWorlds {
    private static final ExecutorService SOLVER = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "RHbm-RadiationSolver");
        thread.setDaemon(true);
        return thread;
    });
    private static final ExecutorService EXPOSURE_SOLVER = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "RHbm-RadiationExposure");
        thread.setDaemon(true);
        return thread;
    });
    private static final Map<ResourceKey<Level>, WorldRuntime> RUNTIMES = new ConcurrentHashMap<>();
    private static final Set<ResourceKey<Level>> ACTIVE_WORLDS = ConcurrentHashMap.newKeySet();

    private HbmRadiationWorlds() {
    }

    public static void tick(ServerLevel level) {
        ChunkRadiationData data = ChunkRadiationData.get(level);
        WorldRuntime runtime = runtime(level);
        runtime.tick(level, data);
        if (data.isEmpty()) {
            markInactive(level);
        } else {
            markActive(level);
        }
        RadiationWorldEffects.tick(level, runtime.chunkRadiation());
    }

    public static double getRadiation(ServerLevel level, BlockPos pos) {
        if (!hasRadiation(level)) {
            return 0.0D;
        }
        return ChunkRadiationData.get(level).getRadiation(pos);
    }

    public static boolean hasRadiation(ServerLevel level) {
        return ACTIVE_WORLDS.contains(level.dimension());
    }

    static void markActive(ServerLevel level) {
        ACTIVE_WORLDS.add(level.dimension());
    }

    static void markInactive(ServerLevel level) {
        ACTIVE_WORLDS.remove(level.dimension());
    }

    static void markChunkLoaded(ServerLevel level, ChunkPos chunkPos) {
        long chunkKey = chunkPos.toLong();
        runtime(level).loadChunk(chunkKey, ChunkRadiationData.get(level).radiationForChunk(chunkKey));
    }

    static void markChunkUnloaded(ServerLevel level, ChunkPos chunkPos) {
        WorldRuntime runtime = RUNTIMES.get(level.dimension());
        if (runtime != null) {
            runtime.unloadChunk(chunkPos.toLong());
        }
    }

    static Set<Long> loadedChunks(ServerLevel level) {
        WorldRuntime runtime = RUNTIMES.get(level.dimension());
        return runtime == null ? Set.of() : runtime.loadedChunks();
    }

    static void onRadiationChanged(ServerLevel level, long chunkKey, double radiation) {
        runtime(level).setRadiation(chunkKey, radiation);
    }

    static void queueExposure(ServerLevel level, LivingEntity entity) {
        if (!hasRadiation(level)) {
            return;
        }
        if (entity.tickCount % HbmRadiationConstants.HAZARD_RATE_TICKS != 0) {
            return;
        }
        runtime(level).queueExposure(new ExposureRequest(entity.getUUID(), chunkKey(entity.blockPosition()),
                entity.tickCount, level.getGameTime()));
    }

    static double getExposureRadiation(ServerLevel level, LivingEntity entity) {
        WorldRuntime runtime = RUNTIMES.get(level.dimension());
        if (runtime != null) {
            if (runtime.needsDirectExposureRead()) {
                return getRadiation(level, entity.blockPosition());
            }
            ExposureResult result = runtime.exposure(entity.getUUID());
            long chunkKey = chunkKey(entity.blockPosition());
            long publishedRevision = runtime.publishedRevision();
            if (result != null && result.chunkKey() == chunkKey
                    && result.sourceRevision() == publishedRevision) {
                return result.radiation();
            }
            return runtime.publishedChunkRadiation(chunkKey);
        }
        // Bootstrap only: the worker has not published its first immutable map yet.
        return getRadiation(level, entity.blockPosition());
    }

    static void unload(ServerLevel level) {
        ResourceKey<Level> dimension = level.dimension();
        ACTIVE_WORLDS.remove(dimension);
        RUNTIMES.remove(dimension);
        RadiationWorldEffects.unload(dimension.location());
    }

    public static void invalidateResistance(ServerLevel level, BlockPos pos) {
        RadiationShielding.invalidate(level, pos);
    }

    public static void invalidateResistanceSection(ServerLevel level, long sectionKey) {
        RadiationShielding.invalidateAll(level);
    }

    public static void clear() {
        RUNTIMES.clear();
        ACTIVE_WORLDS.clear();
        RadiationWorldEffects.clear();
    }

    private static WorldRuntime runtime(ServerLevel level) {
        return RUNTIMES.computeIfAbsent(level.dimension(), key -> new WorldRuntime());
    }

    private static long chunkKey(BlockPos pos) {
        return new ChunkPos(pos).toLong();
    }

    private static final class WorldRuntime {
        private CompletableFuture<SolvedSnapshot> inFlight;
        private CompletableFuture<Map<UUID, ExposureResult>> inFlightExposure;
        private long tickCounter;
        private boolean initialized;
        private boolean solveAgain;
        private final ConcurrentLinkedQueue<RadiationEvent> events = new ConcurrentLinkedQueue<>();
        private final Object radiationQueueLock = new Object();
        private final Map<Long, QueuedRadiation> pendingRadiation = new HashMap<>();
        private final AtomicLong radiationSequence = new AtomicLong();
        private boolean radiationFlushQueued;
        private final Set<Long> loadedChunks = ConcurrentHashMap.newKeySet();
        private volatile Map<Long, Double> chunkRadiation = Map.of();
        private final WorkerState workerState = new WorkerState();
        private final Map<UUID, ExposureRequest> pendingExposure = new HashMap<>();
        private final Map<UUID, ExposureResult> readyExposure = new HashMap<>();
        private volatile boolean publishedSnapshot;
        private volatile boolean publishedDirty;
        private volatile long publishedRevision;

        void tick(ServerLevel level, ChunkRadiationData data) {
            tickCounter++;
            applyExposureResults();
            if (inFlight != null && inFlight.isDone()) {
                SolvedSnapshot solved = inFlight.join();
                Set<Long> currentChunks = loadedChunks(solved.updatedChunks(), loadedChunks);
                boolean accepted = data.applySolvedSnapshot(solved.chunks(), solved.revision(), currentChunks);
                if (accepted) {
                    chunkRadiation = filterLoadedChunks(solved.chunks(), loadedChunks);
                    publishedSnapshot = true;
                    publishedDirty = false;
                    publishedRevision = data.revision();
                    solveAgain = false;
                } else {
                    solveAgain = true;
                }
                inFlight = null;
            }
            if (inFlight == null && (!initialized || solveAgain
                    || tickCounter % HbmRadiationConstants.RAD_SOLVE_INTERVAL_TICKS == 0)) {
                initialized = true;
                long revision = data.revision();
                inFlight = CompletableFuture.supplyAsync(
                        () -> simulate(revision), SOLVER);
            }
            scheduleExposure(level.getGameTime());
        }

        Map<Long, Double> chunkRadiation() {
            return chunkRadiation;
        }

        long publishedRevision() {
            return publishedRevision;
        }

        double publishedChunkRadiation(long chunkKey) {
            return chunkRadiation.getOrDefault(chunkKey, 0.0D);
        }

        boolean needsDirectExposureRead() {
            return !publishedSnapshot || publishedDirty;
        }

        Set<Long> loadedChunks() {
            return loadedChunks;
        }

        void loadChunk(long chunkKey, double radiation) {
            flushRadiationBeforeStructure();
            loadedChunks.add(chunkKey);
            publishedDirty = true;
            events.offer(new LoadChunk(chunkKey, radiation));
        }

        void unloadChunk(long chunkKey) {
            flushRadiationBeforeStructure();
            loadedChunks.remove(chunkKey);
            publishedDirty = true;
            events.offer(new UnloadChunk(chunkKey));
        }

        void setRadiation(long chunkKey, double radiation) {
            publishedDirty = true;
            synchronized (radiationQueueLock) {
                long sequence = radiationSequence.incrementAndGet();
                pendingRadiation.put(chunkKey, new QueuedRadiation(sequence, radiation));
                if (!radiationFlushQueued) {
                    radiationFlushQueued = true;
                    events.offer(new RadiationFlush(sequence));
                }
            }
        }

        private void flushRadiationBeforeStructure() {
            synchronized (radiationQueueLock) {
                if (!pendingRadiation.isEmpty()) {
                    events.offer(new RadiationFlush(radiationSequence.get()));
                }
            }
        }

        void queueExposure(ExposureRequest request) {
            pendingExposure.put(request.entityId(), request);
        }

        ExposureResult exposure(UUID entityId) {
            return readyExposure.get(entityId);
        }

        private void applyExposureResults() {
            if (inFlightExposure == null || !inFlightExposure.isDone()) {
                return;
            }
            readyExposure.putAll(inFlightExposure.join());
            inFlightExposure = null;
        }

        private void scheduleExposure(long gameTime) {
            if (inFlightExposure != null || pendingExposure.isEmpty()) {
                return;
            }
            List<ExposureRequest> requests = List.copyOf(pendingExposure.values());
            pendingExposure.clear();
            Map<Long, Double> radiation = chunkRadiation;
            long sourceRevision = publishedRevision;
            inFlightExposure = CompletableFuture.supplyAsync(
                    () -> calculateExposure(requests, radiation, sourceRevision), EXPOSURE_SOLVER);
            if (gameTime % 200L == 0L) {
                readyExposure.entrySet().removeIf(entry -> entry.getValue().worldTime() + 200L < gameTime);
            }
        }

        private SolvedSnapshot simulate(long revision) {
            workerState.apply(events);
            if (workerState.radiation.isEmpty()) {
                Set<Long> updated = workerState.publishedKeys;
                workerState.publishedKeys = Set.of();
                return new SolvedSnapshot(revision, Map.of(), updated);
            }
            Set<Long> activeChunks = activeChunks(workerState.radiation, workerState.loadedChunks);
            Map<Long, Double> solved = solve(workerState.radiation, activeChunks);
            Set<Long> updated = new HashSet<>(workerState.publishedKeys);
            updated.addAll(workerState.radiation.keySet());
            updated.addAll(solved.keySet());
            workerState.radiation = new HashMap<>(solved);
            workerState.publishedKeys = new HashSet<>(workerState.radiation.keySet());
            return new SolvedSnapshot(revision, solved, updated);
        }

        private static Set<Long> activeChunks(Map<Long, Double> radiation, Set<Long> loadedChunks) {
            if (radiation.isEmpty() || loadedChunks.isEmpty()) {
                return Set.of();
            }
            Set<Long> active = new HashSet<>();
            for (long chunkKey : radiation.keySet()) {
                if (!loadedChunks.contains(chunkKey)) {
                    continue;
                }
                active.add(chunkKey);
                int chunkX = ChunkPos.getX(chunkKey);
                int chunkZ = ChunkPos.getZ(chunkKey);
                for (int offsetX = -1; offsetX <= 1; offsetX++) {
                    for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
                        long neighbor = ChunkPos.asLong(chunkX + offsetX, chunkZ + offsetZ);
                        if (loadedChunks.contains(neighbor)) {
                            active.add(neighbor);
                        }
                    }
                }
            }
            return active;
        }

        private static Map<Long, Double> filterLoadedChunks(Map<Long, Double> radiation, Set<Long> loadedChunks) {
            if (radiation.isEmpty() || loadedChunks.isEmpty()) {
                return Map.of();
            }
            Map<Long, Double> filtered = new HashMap<>();
            for (Map.Entry<Long, Double> entry : radiation.entrySet()) {
                if (loadedChunks.contains(entry.getKey())) {
                    filtered.put(entry.getKey(), entry.getValue());
                }
            }
            return filtered.isEmpty() ? Map.of() : Collections.unmodifiableMap(filtered);
        }

        private static Set<Long> loadedChunks(Set<Long> chunks, Set<Long> loadedChunks) {
            if (chunks.isEmpty() || loadedChunks.isEmpty()) {
                return Set.of();
            }
            Set<Long> loaded = new HashSet<>();
            for (long chunkKey : chunks) {
                if (loadedChunks.contains(chunkKey)) {
                    loaded.add(chunkKey);
                }
            }
            return loaded;
        }

        private final class WorkerState {
            private Map<Long, Double> radiation = new HashMap<>();
            private final Set<Long> loadedChunks = new HashSet<>();
            private Set<Long> publishedKeys = Set.of();

            void apply(ConcurrentLinkedQueue<RadiationEvent> queue) {
                RadiationEvent event;
                while ((event = queue.poll()) != null) {
                    switch (event) {
                        case RadiationFlush flush -> applyRadiation(flush.cutoff());
                        case LoadChunk load -> {
                            loadedChunks.add(load.chunkKey());
                            if (load.radiation() > HbmRadiationConstants.RAD_EPSILON) {
                                radiation.put(load.chunkKey(), load.radiation());
                            } else {
                                radiation.remove(load.chunkKey());
                            }
                        }
                        case UnloadChunk unload -> {
                            loadedChunks.remove(unload.chunkKey());
                            radiation.remove(unload.chunkKey());
                        }
                        default -> throw new IllegalStateException("Unknown radiation event: " + event);
                    }
                }
            }

            private void applyRadiation(long cutoff) {
                Map<Long, QueuedRadiation> writes = new HashMap<>();
                synchronized (radiationQueueLock) {
                    var iterator = pendingRadiation.entrySet().iterator();
                    while (iterator.hasNext()) {
                        var entry = iterator.next();
                        if (entry.getValue().sequence() <= cutoff) {
                            writes.put(entry.getKey(), entry.getValue());
                            iterator.remove();
                        }
                    }
                    if (pendingRadiation.isEmpty()) {
                        radiationFlushQueued = false;
                    } else {
                        events.offer(new RadiationFlush(radiationSequence.get()));
                    }
                }
                for (Map.Entry<Long, QueuedRadiation> entry : writes.entrySet()) {
                    long chunkKey = entry.getKey();
                    if (!loadedChunks.contains(chunkKey)) {
                        continue;
                    }
                    double value = entry.getValue().radiation();
                    if (value > HbmRadiationConstants.RAD_EPSILON) {
                        radiation.put(chunkKey, value);
                    } else {
                        radiation.remove(chunkKey);
                    }
                }
            }
        }
    }

    private interface RadiationEvent {
    }

    private record LoadChunk(long chunkKey, double radiation) implements RadiationEvent {
    }

    private record UnloadChunk(long chunkKey) implements RadiationEvent {
    }

    private record RadiationFlush(long cutoff) implements RadiationEvent {
    }

    private record QueuedRadiation(long sequence, double radiation) {
    }

    private record SolvedSnapshot(long revision, Map<Long, Double> chunks, Set<Long> updatedChunks) {
    }

    private record ExposureRequest(UUID entityId, long chunkKey, int entityTick, long worldTime) {
    }

    private record ExposureResult(UUID entityId, long chunkKey, int entityTick, long worldTime,
                                  long sourceRevision, double radiation) {
    }

    private static Map<UUID, ExposureResult> calculateExposure(List<ExposureRequest> requests,
                                                               Map<Long, Double> radiation,
                                                               long sourceRevision) {
        Map<UUID, ExposureResult> result = new HashMap<>(requests.size());
        for (ExposureRequest request : requests) {
            result.put(request.entityId(), new ExposureResult(request.entityId(), request.chunkKey(),
                    request.entityTick(), request.worldTime(), sourceRevision,
                    radiation.getOrDefault(request.chunkKey(), 0.0D)));
        }
        return result.isEmpty() ? Map.of() : Collections.unmodifiableMap(result);
    }

    private static Map<Long, Double> solve(Map<Long, Double> snapshot, Set<Long> activeChunks) {
        if (snapshot.isEmpty() || activeChunks.isEmpty()) {
            return Map.of();
        }
        Map<Long, Double> next = new HashMap<>();
        for (Map.Entry<Long, Double> entry : snapshot.entrySet()) {
            long source = entry.getKey();
            double sourceRadiation = entry.getValue();
            if (sourceRadiation <= HbmRadiationConstants.RAD_EPSILON || !activeChunks.contains(source)) {
                continue;
            }
            int chunkX = ChunkPos.getX(source);
            int chunkZ = ChunkPos.getZ(source);
            for (int offsetX = -1; offsetX <= 1; offsetX++) {
                for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
                    long target = ChunkPos.asLong(chunkX + offsetX, chunkZ + offsetZ);
                    if (!activeChunks.contains(target)) {
                        continue;
                    }
                    double updated = next.getOrDefault(target, 0.0D)
                            + sourceRadiation * legacySpreadWeight(offsetX, offsetZ);
                    if (snapshot.containsKey(target)) {
                        updated = Math.max(0.0D, updated * 0.99D - 0.05D);
                    }
                    if (updated > HbmRadiationConstants.RAD_EPSILON && Double.isFinite(updated)) {
                        next.put(target, Math.min(updated, HbmRadiationConstants.CHUNK_RADIATION_MAX));
                    } else {
                        next.remove(target);
                    }
                }
            }
        }
        return next.isEmpty() ? Map.of() : Collections.unmodifiableMap(next);
    }

    private static double legacySpreadWeight(int offsetX, int offsetZ) {
        int distance = Math.abs(offsetX) + Math.abs(offsetZ);
        if (distance == 0) {
            return 0.6D;
        }
        return distance == 1 ? 0.075D : 0.025D;
    }
}
