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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class HbmRadiationWorlds {
    private static final Direction[] POSITIVE_DIRECTIONS = {Direction.UP, Direction.SOUTH, Direction.EAST};
    private static final double RESISTANCE_SCALE = 10_000.0D;
    private static final Map<BlockState, Float> BLOCK_RESISTANCE_CACHE = new ConcurrentHashMap<>();
    private static final ExecutorService SOLVER = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "RHbm-RadiationSolver");
        thread.setDaemon(true);
        return thread;
    });
    private static final Map<ResourceKey<Level>, WorldSolver> SOLVERS = new ConcurrentHashMap<>();
    private static final Map<ResourceKey<Level>, Set<Long>> LOADED_CHUNKS = new ConcurrentHashMap<>();
    private static final Set<ResourceKey<Level>> ACTIVE_WORLDS = ConcurrentHashMap.newKeySet();

    private HbmRadiationWorlds() {
    }

    public static void tick(ServerLevel level) {
        ChunkRadiationData data = ChunkRadiationData.get(level);
        if (data.isEmpty()) {
            markInactive(level);
            return;
        }
        markActive(level);
        solver(level).tick(level, data);
        RadiationWorldEffects.tick(level, data);
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
        LOADED_CHUNKS.computeIfAbsent(level.dimension(), ignored -> ConcurrentHashMap.newKeySet())
                .add(chunkPos.toLong());
    }

    static void markChunkUnloaded(ServerLevel level, net.minecraft.world.level.ChunkPos chunkPos) {
        Set<Long> loaded = LOADED_CHUNKS.get(level.dimension());
        if (loaded == null) {
            return;
        }
        loaded.remove(chunkPos.toLong());
        if (loaded.isEmpty()) {
            LOADED_CHUNKS.remove(level.dimension(), loaded);
        }
    }

    static Set<Long> loadedChunks(ServerLevel level) {
        return LOADED_CHUNKS.getOrDefault(level.dimension(), Set.of());
    }

    static void unload(ServerLevel level) {
        ResourceKey<Level> dimension = level.dimension();
        LOADED_CHUNKS.remove(dimension);
        ACTIVE_WORLDS.remove(dimension);
        SOLVERS.remove(dimension);
        RadiationWorldEffects.unload(dimension.location());
    }

    public static void invalidateResistance(ServerLevel level, BlockPos pos) {
        WorldSolver solver = SOLVERS.get(level.dimension());
        if (solver != null) {
            solver.invalidateResistance(pos);
        }
    }

    public static void invalidateResistanceSection(ServerLevel level, long sectionKey) {
        WorldSolver solver = SOLVERS.get(level.dimension());
        if (solver != null) {
            solver.invalidateResistanceSection(sectionKey);
        }
    }

    public static void clear() {
        SOLVERS.clear();
        LOADED_CHUNKS.clear();
        ACTIVE_WORLDS.clear();
        RadiationWorldEffects.clear();
    }

    private static WorldSolver solver(ServerLevel level) {
        return SOLVERS.computeIfAbsent(level.dimension(), key -> new WorldSolver());
    }

    private static final class WorldSolver {
        private static final int RESISTANCE_CACHE_PRUNE_INTERVAL = 20 * 30;
        private static final int RESISTANCE_CACHE_MAX_IDLE_TICKS = 20 * 120;

        private CompletableFuture<SolvedSnapshot> inFlight;
        private long tickCounter;
        private final Map<Long, CachedResistance> resistanceCache = new HashMap<>();
        private final Set<Long> dirtyResistance = new HashSet<>();

        void tick(ServerLevel level, ChunkRadiationData data) {
            tickCounter++;
            if (inFlight != null && inFlight.isDone()) {
                SolvedSnapshot solved = inFlight.join();
                Set<Long> currentLoadedChunks = HbmRadiationWorlds.loadedChunks(level);
                Set<Long> currentSections = loadedSections(solved.updatedSections(), currentLoadedChunks);
                data.applySolvedSnapshot(solved.sections(), solved.revision(), currentSections);
                inFlight = null;
            }
            if (inFlight != null || tickCounter % HbmRadiationConstants.RAD_SOLVE_INTERVAL_TICKS != 0) {
                return;
            }

            // Publish once per legacy one-second simulation step, after all source
            // writes from the preceding game ticks have been coalesced.
            data.refreshSnapshots();
            Set<Long> loadedChunks = HbmRadiationWorlds.loadedChunks(level);
            Map<Long, Double> snapshot = data.loadedSectionSnapshot(loadedChunks);
            if (snapshot.isEmpty()) {
                return;
            }
            Set<Long> loadedSections = loadedSections(snapshot, loadedChunks);
            long revision = data.revision();
            Map<Long, SectionResistance> resistance = resistanceSnapshot(level, snapshot, loadedSections);
            inFlight = CompletableFuture.supplyAsync(
                    () -> new SolvedSnapshot(revision, solve(snapshot, resistance, loadedSections),
                            Set.copyOf(loadedSections)), SOLVER);
        }

        void invalidateResistance(BlockPos pos) {
            invalidateResistanceSection(SectionPos.asLong(pos));
        }

        void invalidateResistanceSection(long sectionKey) {
            dirtyResistance.add(sectionKey);
        }

        private Map<Long, SectionResistance> resistanceSnapshot(ServerLevel level, Map<Long, Double> radiation,
                                                                  Set<Long> loadedSections) {
            Set<Long> neededKeys = resistanceKeys(radiation, loadedSections);
            Map<Long, SectionResistance> resistance = new HashMap<>(neededKeys.size());
            for (long key : neededKeys) {
                CachedResistance cached = resistanceCache.get(key);
                if (cached == null || dirtyResistance.remove(key)) {
                    cached = new CachedResistance(scanResistance(level, key), tickCounter);
                    resistanceCache.put(key, cached);
                } else {
                    cached = cached.touch(tickCounter);
                    resistanceCache.put(key, cached);
                }
                resistance.put(key, cached.resistance());
            }

            if (tickCounter % RESISTANCE_CACHE_PRUNE_INTERVAL == 0) {
                pruneResistanceCache();
            }
            return Map.copyOf(resistance);
        }

        private Set<Long> resistanceKeys(Map<Long, Double> radiation, Set<Long> loadedSections) {
            Set<Long> keys = new HashSet<>();
            for (long key : radiation.keySet()) {
                if (!loadedSections.contains(key)) {
                    continue;
                }
                keys.add(key);
                for (Direction direction : Direction.values()) {
                    long neighbor = SectionPos.offset(key, direction);
                    if (loadedSections.contains(neighbor)) {
                        keys.add(neighbor);
                    }
                }
            }
            return keys;
        }

        private Set<Long> loadedSections(Map<Long, Double> radiation, Set<Long> loadedChunks) {
            Set<Long> loaded = new HashSet<>();
            for (long key : radiation.keySet()) {
                long chunkKey = net.minecraft.world.level.ChunkPos.asLong(SectionPos.x(key), SectionPos.z(key));
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

        private Set<Long> loadedSections(Set<Long> sections, Set<Long> loadedChunks) {
            Set<Long> loaded = new HashSet<>();
            for (long sectionKey : sections) {
                long chunkKey = net.minecraft.world.level.ChunkPos.asLong(
                        SectionPos.x(sectionKey), SectionPos.z(sectionKey));
                if (loadedChunks.contains(chunkKey)) {
                    loaded.add(sectionKey);
                }
            }
            return loaded;
        }

        private void pruneResistanceCache() {
            long cutoff = tickCounter - RESISTANCE_CACHE_MAX_IDLE_TICKS;
            resistanceCache.entrySet().removeIf(entry -> entry.getValue().lastUsedTick() < cutoff);
            dirtyResistance.removeIf(key -> !resistanceCache.containsKey(key));
        }
    }

    private record CachedResistance(SectionResistance resistance, long lastUsedTick) {
        CachedResistance touch(long tick) {
            return lastUsedTick == tick ? this : new CachedResistance(resistance, tick);
        }
    }

    private record SolvedSnapshot(long revision, Map<Long, Double> sections, Set<Long> updatedSections) {
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
