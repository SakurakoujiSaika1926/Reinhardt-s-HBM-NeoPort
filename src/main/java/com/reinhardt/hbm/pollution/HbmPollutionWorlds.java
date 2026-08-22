package com.reinhardt.hbm.pollution;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class HbmPollutionWorlds {
    private static final int[][] NEIGHBOR_OFFSETS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    private static final ExecutorService SOLVER = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "RHbm-PollutionSolver");
        thread.setDaemon(true);
        return thread;
    });
    private static final Map<ResourceKey<Level>, WorldSolver> SOLVERS = new ConcurrentHashMap<>();

    private HbmPollutionWorlds() {
    }

    public static void tick(ServerLevel level) {
        solver(level).tick(HbmPollutionData.get(level));
    }

    public static double get(ServerLevel level, net.minecraft.core.BlockPos pos, HbmPollutionType type) {
        return HbmPollutionData.get(level).get(pos, type);
    }

    public static void clear() {
        SOLVERS.clear();
    }

    private static WorldSolver solver(ServerLevel level) {
        return SOLVERS.computeIfAbsent(level.dimension(), key -> new WorldSolver());
    }

    private static final class WorldSolver {
        private CompletableFuture<SolvedSnapshot> inFlight;
        private long tickCounter;

        void tick(HbmPollutionData data) {
            tickCounter++;
            if (inFlight != null && inFlight.isDone()) {
                SolvedSnapshot solved = inFlight.join();
                data.applySolvedSnapshot(solved.baseline(), solved.regions());
                inFlight = null;
            }
            if (inFlight != null || tickCounter % HbmPollutionConstants.SOLVE_RATE_TICKS != 0) {
                return;
            }
            Map<Long, HbmPollutionData.PollutionValues> snapshot = data.snapshot();
            if (snapshot.isEmpty()) {
                return;
            }
            inFlight = CompletableFuture.supplyAsync(() -> new SolvedSnapshot(snapshot, solve(snapshot)), SOLVER);
        }
    }

    private record SolvedSnapshot(
            Map<Long, HbmPollutionData.PollutionValues> baseline,
            Map<Long, HbmPollutionData.PollutionValues> regions
    ) {
    }

    private static Map<Long, HbmPollutionData.PollutionValues> solve(Map<Long, HbmPollutionData.PollutionValues> snapshot) {
        Map<Long, HbmPollutionData.PollutionValues> next = new HashMap<>();
        for (Map.Entry<Long, HbmPollutionData.PollutionValues> entry : snapshot.entrySet()) {
            long key = entry.getKey();
            HbmPollutionData.PollutionValues current = entry.getValue().copy();
            double[] spread = new double[HbmPollutionType.values().length];

            double soot = current.get(HbmPollutionType.SOOT);
            if (soot > 10.0D) {
                spread[HbmPollutionType.SOOT.ordinal()] = soot * 0.05D;
                soot *= 0.8D;
            }
            current.set(HbmPollutionType.SOOT, soot * 0.99D);
            current.set(HbmPollutionType.HEAVYMETAL, current.get(HbmPollutionType.HEAVYMETAL) * 0.9995D);

            double poison = current.get(HbmPollutionType.POISON);
            if (poison > 10.0D) {
                spread[HbmPollutionType.POISON.ordinal()] = poison * 0.025D;
                poison *= 0.9D;
            } else {
                poison *= 0.995D;
            }
            current.set(HbmPollutionType.POISON, poison);

            if (!current.isEmpty()) {
                add(next, key, current);
            }
            if (!isZero(spread)) {
                for (int[] offset : NEIGHBOR_OFFSETS) {
                    add(next, offset(key, offset[0], offset[1]), spread);
                }
            }
        }
        return Map.copyOf(next);
    }

    private static long offset(long key, int dx, int dz) {
        return HbmPollutionData.key(HbmPollutionData.regionX(key) + dx, HbmPollutionData.regionZ(key) + dz);
    }

    private static void add(Map<Long, HbmPollutionData.PollutionValues> values, long key, HbmPollutionData.PollutionValues addition) {
        HbmPollutionData.PollutionValues target = values.computeIfAbsent(key, unused -> new HbmPollutionData.PollutionValues());
        for (HbmPollutionType type : HbmPollutionType.values()) {
            target.add(type, addition.get(type));
        }
        if (target.isEmpty()) {
            values.remove(key);
        }
    }

    private static void add(Map<Long, HbmPollutionData.PollutionValues> values, long key, double[] addition) {
        HbmPollutionData.PollutionValues target = values.computeIfAbsent(key, unused -> new HbmPollutionData.PollutionValues());
        for (HbmPollutionType type : HbmPollutionType.values()) {
            target.add(type, addition[type.ordinal()]);
        }
        if (target.isEmpty()) {
            values.remove(key);
        }
    }

    private static boolean isZero(double[] values) {
        if (values == null) {
            return true;
        }
        for (double value : values) {
            if (Math.abs(value) > HbmPollutionConstants.EPSILON) {
                return false;
            }
        }
        return true;
    }
}
