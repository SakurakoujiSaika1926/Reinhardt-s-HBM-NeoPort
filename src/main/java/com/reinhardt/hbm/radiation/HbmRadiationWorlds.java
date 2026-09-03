package com.reinhardt.hbm.radiation;

import com.reinhardt.hbm.block.LegacyVariantStrengths;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
    private static final Direction[] POSITIVE_DIRECTIONS = {Direction.UP, Direction.SOUTH, Direction.EAST};
    private static final double RESISTANCE_SCALE = 10_000.0D;
    private static final Map<BlockState, Float> BLOCK_RESISTANCE_CACHE = new ConcurrentHashMap<>();
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

    public static double getRadiation(ServerLevel level, net.minecraft.core.BlockPos pos) {
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

    static void markChunkLoaded(ServerLevel level, net.minecraft.world.level.ChunkPos chunkPos) {
        long chunkKey = chunkPos.toLong();
        runtime(level).loadChunk(chunkKey, ChunkRadiationData.get(level).sectionsForChunk(chunkKey));
    }

    static void markChunkUnloaded(ServerLevel level, net.minecraft.world.level.ChunkPos chunkPos) {
        WorldRuntime runtime = RUNTIMES.get(level.dimension());
        if (runtime != null) {
            runtime.unloadChunk(chunkPos.toLong());
        }
    }

    static Set<Long> loadedChunks(ServerLevel level) {
        WorldRuntime runtime = RUNTIMES.get(level.dimension());
        return runtime == null ? Set.of() : runtime.loadedChunks();
    }

    static void onRadiationChanged(ServerLevel level, long sectionKey, double radiation, long revision) {
        runtime(level).setRadiation(sectionKey, radiation, revision);
    }

    static void queueExposure(ServerLevel level, LivingEntity entity) {
        if (!hasRadiation(level)) {
            return;
        }
        if (entity.tickCount % HbmRadiationConstants.HAZARD_RATE_TICKS != 0) {
            return;
        }
        runtime(level).queueExposure(new ExposureRequest(entity.getUUID(), SectionPos.asLong(entity.blockPosition()),
                entity.tickCount, level.getGameTime()));
    }

    static double getExposureRadiation(ServerLevel level, LivingEntity entity) {
        WorldRuntime runtime = RUNTIMES.get(level.dimension());
        if (runtime != null) {
            ExposureResult result = runtime.exposure(entity.getUUID());
            long sectionKey = SectionPos.asLong(entity.blockPosition());
            if (result != null && result.sectionKey() == sectionKey
                    && result.sourceRevision() == runtime.sourceRevision()) {
                return result.radiation();
            }
            return getRadiation(level, entity.blockPosition());
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
        WorldRuntime runtime = RUNTIMES.get(level.dimension());
        if (runtime != null) {
            runtime.invalidateResistance(pos);
        }
    }

    public static void invalidateResistanceSection(ServerLevel level, long sectionKey) {
        WorldRuntime runtime = RUNTIMES.get(level.dimension());
        if (runtime != null) {
            runtime.invalidateResistanceSection(sectionKey);
        }
    }

    public static void clear() {
        RUNTIMES.clear();
        ACTIVE_WORLDS.clear();
        RadiationWorldEffects.clear();
    }

    private static WorldRuntime runtime(ServerLevel level) {
        return RUNTIMES.computeIfAbsent(level.dimension(), key -> new WorldRuntime());
    }

    private static final class WorldRuntime {
        private static final int RESISTANCE_CACHE_PRUNE_INTERVAL = 20 * 30;
        private static final int RESISTANCE_CACHE_MAX_IDLE_TICKS = 20 * 120;

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
        private final Set<Long> trackedSections = ConcurrentHashMap.newKeySet();
        private final Map<Long, CachedResistance> resistanceCache = new HashMap<>();
        /** Number of active radiation sections that need each loaded section's resistance. */
        private final Map<Long, Integer> resistanceDemand = new HashMap<>();
        /** Sections whose cached resistance must be published to the worker. */
        private final Set<Long> pendingResistance = new HashSet<>();
        private final Set<Long> dirtyResistance = new HashSet<>();
        private volatile Map<Long, Double> chunkRadiation = Map.of();
        private final WorkerState workerState = new WorkerState();
        private final Map<UUID, ExposureRequest> pendingExposure = new HashMap<>();
        private final Map<UUID, ExposureResult> readyExposure = new HashMap<>();
        private volatile Map<Long, Double> publishedRadiation = Map.of();
        private volatile long sourceRevision;
        private volatile long publishedRevision;

        void tick(ServerLevel level, ChunkRadiationData data) {
            tickCounter++;
            applyExposureResults();
            if (inFlight != null && inFlight.isDone()) {
                SolvedSnapshot solved = inFlight.join();
                Set<Long> currentSections = loadedSections(solved.updatedSections(), loadedChunks);
                boolean accepted = data.applySolvedSnapshot(solved.sections(), solved.revision(), currentSections);
                if (accepted) {
                    updateTrackedSections(solved.sections());
                    Map<Long, Double> acceptedSections = filterLoadedSections(solved.sections(), loadedChunks);
                    chunkRadiation = filterLoadedChunks(solved.chunks(), loadedChunks);
                    publishedRadiation = acceptedSections;
                    publishedRevision = sourceRevision = data.revision();
                    solveAgain = false;
                } else {
                    solveAgain = true;
                }
                inFlight = null;
            }
            if (inFlight == null && (!initialized || solveAgain
                    || tickCounter % HbmRadiationConstants.RAD_SOLVE_INTERVAL_TICKS == 0)) {
                initialized = true;
                prepareResistance(level);
                long revision = data.revision();
                inFlight = CompletableFuture.supplyAsync(
                        () -> simulate(revision), SOLVER);
            }
            scheduleExposure(level.getGameTime());
        }

        Map<Long, Double> chunkRadiation() {
            return chunkRadiation;
        }

        long sourceRevision() {
            return sourceRevision;
        }

        Set<Long> loadedChunks() {
            return loadedChunks;
        }

        void loadChunk(long chunkKey, Map<Long, Double> radiation) {
            flushRadiationBeforeStructure();
            loadedChunks.add(chunkKey);
            for (long sectionKey : radiation.keySet()) {
                addTrackedSection(sectionKey);
            }
            rebuildResistanceDemand();
            events.offer(new LoadChunk(chunkKey, radiation));
        }

        void unloadChunk(long chunkKey) {
            flushRadiationBeforeStructure();
            loadedChunks.remove(chunkKey);
            Iterator<Long> iterator = trackedSections.iterator();
            while (iterator.hasNext()) {
                long sectionKey = iterator.next();
                if (chunkOf(sectionKey) == chunkKey) {
                    iterator.remove();
                    removeResistanceDemand(sectionKey);
                }
            }
            rebuildResistanceDemand();
            events.offer(new UnloadChunk(chunkKey));
        }

        void setRadiation(long sectionKey, double radiation, long revision) {
            sourceRevision = revision;
            if (radiation > HbmRadiationConstants.RAD_EPSILON) {
                addTrackedSection(sectionKey);
            } else {
                removeTrackedSection(sectionKey);
            }
            synchronized (radiationQueueLock) {
                long sequence = radiationSequence.incrementAndGet();
                pendingRadiation.put(sectionKey, new QueuedRadiation(sequence, radiation));
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

        ExposureResult exposure(java.util.UUID entityId) {
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
            Map<Long, Double> radiation = publishedRadiation;
            long sourceRevision = publishedRevision;
            inFlightExposure = CompletableFuture.supplyAsync(
                    () -> calculateExposure(requests, radiation, sourceRevision), EXPOSURE_SOLVER);
            if (gameTime % 200L == 0L) {
                readyExposure.entrySet().removeIf(entry -> entry.getValue().worldTime() + 200L < gameTime);
            }
        }

        void invalidateResistance(BlockPos pos) {
            invalidateResistanceSection(SectionPos.asLong(pos));
        }

        void invalidateResistanceSection(long sectionKey) {
            dirtyResistance.add(sectionKey);
            if (resistanceDemand.containsKey(sectionKey)) {
                pendingResistance.add(sectionKey);
            }
        }

        private void prepareResistance(ServerLevel level) {
            if (resistanceDemand.isEmpty() && !trackedSections.isEmpty()) {
                rebuildResistanceDemand();
            }
            Iterator<Long> iterator = pendingResistance.iterator();
            while (iterator.hasNext()) {
                long key = iterator.next();
                iterator.remove();
                if (!resistanceDemand.containsKey(key) || !isInLoadedChunk(key)) {
                    continue;
                }
                CachedResistance cached = resistanceCache.get(key);
                if (cached == null || dirtyResistance.remove(key)) {
                    cached = new CachedResistance(scanResistance(level, key), tickCounter);
                    resistanceCache.put(key, cached);
                } else {
                    resistanceCache.put(key, cached.touch(tickCounter));
                }
                events.offer(new ResistanceUpdate(key, cached.resistance()));
            }
            if (tickCounter % RESISTANCE_CACHE_PRUNE_INTERVAL == 0) {
                pruneResistanceCache();
            }
        }

        private void updateTrackedSections(Map<Long, Double> solved) {
            for (long sectionKey : solved.keySet()) {
                addTrackedSection(sectionKey);
            }

            Iterator<Long> iterator = trackedSections.iterator();
            while (iterator.hasNext()) {
                long sectionKey = iterator.next();
                if (!solved.containsKey(sectionKey) && isInLoadedChunk(sectionKey)) {
                    iterator.remove();
                    removeResistanceDemand(sectionKey);
                }
            }
        }

        private void addTrackedSection(long sectionKey) {
            if (trackedSections.add(sectionKey)) {
                addResistanceDemand(sectionKey);
            }
        }

        private void removeTrackedSection(long sectionKey) {
            if (trackedSections.remove(sectionKey)) {
                removeResistanceDemand(sectionKey);
            }
        }

        private void addResistanceDemand(long sourceSection) {
            changeResistanceDemand(sourceSection, 1);
            for (Direction direction : Direction.values()) {
                changeResistanceDemand(SectionPos.offset(sourceSection, direction), 1);
            }
        }

        private void removeResistanceDemand(long sourceSection) {
            changeResistanceDemand(sourceSection, -1);
            for (Direction direction : Direction.values()) {
                changeResistanceDemand(SectionPos.offset(sourceSection, direction), -1);
            }
        }

        private void changeResistanceDemand(long sectionKey, int delta) {
            if (!isInLoadedChunk(sectionKey)) {
                return;
            }
            int previous = resistanceDemand.getOrDefault(sectionKey, 0);
            int next = previous + delta;
            if (next <= 0) {
                resistanceDemand.remove(sectionKey);
                pendingResistance.remove(sectionKey);
                return;
            }
            resistanceDemand.put(sectionKey, next);
            if (previous == 0 || !resistanceCache.containsKey(sectionKey) || dirtyResistance.contains(sectionKey)) {
                pendingResistance.add(sectionKey);
            }
        }

        private void rebuildResistanceDemand() {
            Map<Long, Integer> previous = resistanceDemand.isEmpty()
                    ? Map.of()
                    : new HashMap<>(resistanceDemand);
            resistanceDemand.clear();
            for (long sourceSection : trackedSections) {
                addDemandWithoutQueue(sourceSection);
            }
            pendingResistance.removeIf(sectionKey -> !resistanceDemand.containsKey(sectionKey));
            for (long sectionKey : resistanceDemand.keySet()) {
                if (!previous.containsKey(sectionKey) || !resistanceCache.containsKey(sectionKey)
                        || dirtyResistance.contains(sectionKey)) {
                    pendingResistance.add(sectionKey);
                }
            }
        }

        private void addDemandWithoutQueue(long sourceSection) {
            addDemandWithoutQueueFor(sourceSection);
            for (Direction direction : Direction.values()) {
                addDemandWithoutQueueFor(SectionPos.offset(sourceSection, direction));
            }
        }

        private void addDemandWithoutQueueFor(long sectionKey) {
            if (isInLoadedChunk(sectionKey)) {
                resistanceDemand.merge(sectionKey, 1, Integer::sum);
            }
        }

        private SolvedSnapshot simulate(long revision) {
            workerState.apply(events);
            if (workerState.radiation.isEmpty()) {
                Set<Long> updated = workerState.publishedKeys;
                workerState.publishedKeys = Set.of();
                return new SolvedSnapshot(revision, Map.of(), updated, Map.of());
            }
            Set<Long> activeSections = activeSections(workerState.radiation, workerState.loadedChunks);
            Map<Long, Double> solved = solve(workerState.radiation, workerState.resistance, activeSections);
            Set<Long> updated = new HashSet<>(workerState.publishedKeys);
            updated.addAll(workerState.radiation.keySet());
            updated.addAll(solved.keySet());
            workerState.radiation = new HashMap<>(solved);
            workerState.publishedKeys = Set.copyOf(workerState.radiation.keySet());
            return new SolvedSnapshot(revision, solved, Set.copyOf(updated), aggregateChunks(solved));
        }

        private boolean isInLoadedChunk(long sectionKey) {
            return loadedChunks.contains(chunkOf(sectionKey));
        }

        private static Set<Long> activeSections(Map<Long, Double> radiation, Set<Long> loadedChunks) {
            Set<Long> loaded = new HashSet<>();
            for (long key : radiation.keySet()) {
                long chunkKey = chunkOf(key);
                if (loadedChunks.contains(chunkKey)) {
                    loaded.add(key);
                    for (Direction direction : Direction.values()) {
                        long neighbor = SectionPos.offset(key, direction);
                        long neighborChunk = net.minecraft.world.level.ChunkPos.asLong(
                                SectionPos.x(neighbor), SectionPos.z(neighbor));
                        if (loadedChunks.contains(neighborChunk)) {
                            loaded.add(neighbor);
                        }
                    }
                }
            }
            return loaded;
        }

        private static Map<Long, Double> aggregateChunks(Map<Long, Double> sections) {
            if (sections.isEmpty()) {
                return Map.of();
            }
            Map<Long, Double> chunks = new HashMap<>();
            for (Map.Entry<Long, Double> entry : sections.entrySet()) {
                chunks.merge(chunkOf(entry.getKey()), entry.getValue(), Math::max);
            }
            return Map.copyOf(chunks);
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
            return filtered.isEmpty() ? Map.of() : Map.copyOf(filtered);
        }

        private static Map<Long, Double> filterLoadedSections(Map<Long, Double> radiation, Set<Long> loadedChunks) {
            if (radiation.isEmpty() || loadedChunks.isEmpty()) {
                return Map.of();
            }
            Map<Long, Double> filtered = new HashMap<>();
            for (Map.Entry<Long, Double> entry : radiation.entrySet()) {
                if (loadedChunks.contains(chunkOf(entry.getKey()))) {
                    filtered.put(entry.getKey(), entry.getValue());
                }
            }
            return filtered.isEmpty() ? Map.of() : Map.copyOf(filtered);
        }

        private static Set<Long> loadedSections(Set<Long> sections, Set<Long> loadedChunks) {
            if (sections.isEmpty() || loadedChunks.isEmpty()) {
                return Set.of();
            }
            Set<Long> loaded = new HashSet<>();
            for (long sectionKey : sections) {
                if (loadedChunks.contains(chunkOf(sectionKey))) {
                    loaded.add(sectionKey);
                }
            }
            return loaded;
        }

        private void pruneResistanceCache() {
            long cutoff = tickCounter - RESISTANCE_CACHE_MAX_IDLE_TICKS;
            resistanceCache.entrySet().removeIf(entry -> !resistanceDemand.containsKey(entry.getKey())
                    && entry.getValue().lastUsedTick() < cutoff);
            dirtyResistance.removeIf(key -> !resistanceCache.containsKey(key));
        }

        private static long chunkOf(long sectionKey) {
            return net.minecraft.world.level.ChunkPos.asLong(SectionPos.x(sectionKey), SectionPos.z(sectionKey));
        }

        private final class WorkerState {
            private Map<Long, Double> radiation = new HashMap<>();
            private final Map<Long, SectionResistance> resistance = new HashMap<>();
            private final Set<Long> loadedChunks = new HashSet<>();
            private Set<Long> publishedKeys = Set.of();

            void apply(ConcurrentLinkedQueue<RadiationEvent> queue) {
                RadiationEvent event;
                while ((event = queue.poll()) != null) {
                    switch (event) {
                        case RadiationFlush flush -> applyRadiation(flush.cutoff());
                        case LoadChunk load -> {
                            loadedChunks.add(load.chunkKey());
                            radiation.putAll(load.radiation());
                        }
                        case UnloadChunk unload -> {
                            loadedChunks.remove(unload.chunkKey());
                            radiation.keySet().removeIf(sectionKey -> chunkOf(sectionKey) == unload.chunkKey());
                            resistance.keySet().removeIf(sectionKey -> chunkOf(sectionKey) == unload.chunkKey());
                        }
                        case ResistanceUpdate update -> {
                            if (loadedChunks.contains(chunkOf(update.sectionKey()))) {
                                resistance.put(update.sectionKey(), update.resistance());
                            }
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
                    long sectionKey = entry.getKey();
                    if (!loadedChunks.contains(chunkOf(sectionKey))) {
                        continue;
                    }
                    double value = entry.getValue().radiation();
                    if (value > HbmRadiationConstants.RAD_EPSILON) {
                        radiation.put(sectionKey, value);
                    } else {
                        radiation.remove(sectionKey);
                    }
                }
            }
        }
    }

    private record CachedResistance(SectionResistance resistance, long lastUsedTick) {
        CachedResistance touch(long tick) {
            return lastUsedTick == tick ? this : new CachedResistance(resistance, tick);
        }
    }

    private interface RadiationEvent {
    }

    private record LoadChunk(long chunkKey, Map<Long, Double> radiation) implements RadiationEvent {
    }

    private record UnloadChunk(long chunkKey) implements RadiationEvent {
    }

    private record RadiationFlush(long cutoff) implements RadiationEvent {
    }

    private record QueuedRadiation(long sequence, double radiation) {
    }

    private record ResistanceUpdate(long sectionKey, SectionResistance resistance) implements RadiationEvent {
    }

    private record SolvedSnapshot(long revision, Map<Long, Double> sections, Set<Long> updatedSections,
                                  Map<Long, Double> chunks) {
    }

    private record ExposureRequest(java.util.UUID entityId, long sectionKey, int entityTick, long worldTime) {
    }

    private record ExposureResult(java.util.UUID entityId, long sectionKey, int entityTick, long worldTime,
                                  long sourceRevision, double radiation) {
    }

    private static Map<java.util.UUID, ExposureResult> calculateExposure(List<ExposureRequest> requests,
                                                                          Map<Long, Double> radiation,
                                                                          long sourceRevision) {
        Map<java.util.UUID, ExposureResult> result = new HashMap<>(requests.size());
        for (ExposureRequest request : requests) {
            result.put(request.entityId(), new ExposureResult(request.entityId(), request.sectionKey(),
                    request.entityTick(), request.worldTime(), sourceRevision,
                    radiation.getOrDefault(request.sectionKey(), 0.0D)));
        }
        return Map.copyOf(result);
    }

    private static Map<Long, Double> solve(Map<Long, Double> snapshot, Map<Long, SectionResistance> resistance,
                                           Set<Long> loadedSections) {
        double dt = HbmRadiationConstants.RAD_SIMULATION_STEP_SECONDS;
        double retention = Math.exp(Math.log(0.5D) * (dt / HbmRadiationConstants.RAD_HALF_LIFE_SECONDS));
        double exchange = 1.0D - Math.exp(-(HbmRadiationConstants.RAD_DIFFUSIVITY * dt / 128.0D));

        Map<Long, Double> next = new HashMap<>();
        Set<Long> keys = new HashSet<>();
        for (Map.Entry<Long, Double> entry : snapshot.entrySet()) {
            double decayed = entry.getValue() * retention;
            if (decayed > HbmRadiationConstants.RAD_EPSILON) {
                next.put(entry.getKey(), decayed);
                keys.add(entry.getKey());
                if (loadedSections.contains(entry.getKey())) {
                    for (Direction direction : Direction.values()) {
                        long neighbor = SectionPos.offset(entry.getKey(), direction);
                        if (loadedSections.contains(neighbor)) {
                            keys.add(neighbor);
                        }
                    }
                }
            }
        }

        for (long key : keys) {
            if (!loadedSections.contains(key)) {
                continue;
            }
            double current = next.getOrDefault(key, 0.0D);
            for (Direction direction : POSITIVE_DIRECTIONS) {
                long neighbor = SectionPos.offset(key, direction);
                double other = next.getOrDefault(neighbor, 0.0D);
                double attenuation = radiationAttenuation(resistance, key, neighbor, direction);
                double delta = (current - other) * exchange * attenuation * 0.5D;
                if (Math.abs(delta) <= HbmRadiationConstants.RAD_EPSILON) {
                    continue;
                }
                current -= delta;
                if (loadedSections.contains(neighbor)) {
                    next.put(neighbor, other + delta);
                }
            }
            next.put(key, current);
        }

        next.entrySet().removeIf(entry -> entry.getValue() <= HbmRadiationConstants.RAD_EPSILON || !Double.isFinite(entry.getValue()));
        next.replaceAll((key, value) -> Math.min(value, HbmRadiationConstants.CHUNK_RADIATION_MAX));
        return Map.copyOf(next);
    }

    private static SectionResistance scanResistance(ServerLevel level, long sectionKey) {
        int minY = SectionPos.sectionToBlockCoord(SectionPos.y(sectionKey));
        LevelChunk chunk = level.getChunkSource().getChunkNow(SectionPos.x(sectionKey), SectionPos.z(sectionKey));
        if (chunk == null) {
            return SectionResistance.EMPTY;
        }

        int sectionIndex = level.getSectionIndex(minY);
        if (sectionIndex < 0 || sectionIndex >= level.getSectionsCount()) {
            return SectionResistance.EMPTY;
        }
        LevelChunkSection section = chunk.getSection(sectionIndex);
        if (section.hasOnlyAir()) {
            return SectionResistance.EMPTY;
        }

        float[] x = new float[16];
        float[] y = new float[16];
        float[] z = new float[16];
        for (int localX = 0; localX < 16; localX++) {
            for (int localY = 0; localY < 16; localY++) {
                for (int localZ = 0; localZ < 16; localZ++) {
                    BlockState state = section.getBlockState(localX, localY, localZ);
                    if (state.isAir()) {
                        continue;
                    }
                    float blockResistance = Math.min(BLOCK_RESISTANCE_CACHE.computeIfAbsent(
                            state, HbmRadiationWorlds::radiationResistance), 100.0F);
                    if (blockResistance <= 0.0F) {
                        continue;
                    }
                    x[localX] += blockResistance;
                    y[localY] += blockResistance;
                    z[localZ] += blockResistance;
                }
            }
        }
        return new SectionResistance(x, y, z);
    }

    private static float radiationResistance(BlockState state) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (id.getNamespace().equals("reinhardtshbm")) {
            String path = id.getPath();
            if (path.equals("brick_slab") || path.equals("brick_double_slab")) {
                return variantResistance(state, LegacyVariantStrengths.BRICK_SLAB);
            }
            if (path.equals("concrete_brick_slab") || path.equals("concrete_brick_double_slab")) {
                return variantResistance(state, LegacyVariantStrengths.CONCRETE_BRICK_SLAB);
            }
        }
        return state.getBlock().getExplosionResistance();
    }

    private static float variantResistance(BlockState state, LegacyVariantStrengths.Strength[] table) {
        int variant = 0;
        Property<?> property = state.getBlock().getStateDefinition().getProperty("variant");
        if (property != null && state.getValue(property) instanceof Integer value) {
            variant = Math.floorMod(value, table.length);
        }
        return table[variant].resistance();
    }

    private static double radiationAttenuation(Map<Long, SectionResistance> resistance, long from, long to, Direction direction) {
        SectionResistance fromResistance = resistance.getOrDefault(from, SectionResistance.EMPTY);
        SectionResistance toResistance = resistance.getOrDefault(to, SectionResistance.EMPTY);
        double total = fromResistance.value(direction.getOpposite()) + toResistance.value(direction);
        return Math.exp(-total / RESISTANCE_SCALE);
    }

    private record SectionResistance(float[] x, float[] y, float[] z,
                                     float east, float west, float up, float down, float south, float north) {
        static final SectionResistance EMPTY = new SectionResistance(new float[16], new float[16], new float[16]);

        SectionResistance(float[] x, float[] y, float[] z) {
            this(x, y, z,
                    weighted(x, true), weighted(x, false),
                    weighted(y, true), weighted(y, false),
                    weighted(z, true), weighted(z, false));
        }

        float value(Direction movement) {
            return switch (movement) {
                case EAST -> east;
                case WEST -> west;
                case UP -> up;
                case DOWN -> down;
                case SOUTH -> south;
                case NORTH -> north;
            };
        }

        private static float weighted(float[] slices, boolean reverse) {
            float result = 0.0F;
            for (int i = 1; i < 16; i++) {
                int index = reverse ? 15 - i : i;
                result += slices[index] / 15.0F * i;
            }
            return result;
        }
    }
}
