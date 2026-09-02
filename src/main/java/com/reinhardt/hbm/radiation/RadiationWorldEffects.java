package com.reinhardt.hbm.radiation;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.registry.HbmParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class RadiationWorldEffects {
    private static final int FOG_THRESHOLD = 100;
    private static final int FOG_CHANCE = 20;
    private static final int FOG_CHUNK_ROLLS = 5;
    private static final int TERRAIN_THRESHOLD = 10;
    private static final int CHUNK_ROLLS_PER_TICK = 5;
    private static final int LOCATION_ROLLS_PER_CHUNK = 10;
    private static final ExecutorService PLANNER = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "RHbm-RadiationWorldEffects");
        thread.setDaemon(true);
        return thread;
    });
    private static final Map<net.minecraft.resources.ResourceLocation, WorldEffects> EFFECTS = new ConcurrentHashMap<>();

    private RadiationWorldEffects() {
    }

    static void tick(ServerLevel level, Map<Long, Double> radiation) {
        WorldEffects effects = EFFECTS.computeIfAbsent(level.dimension().location(), unused -> new WorldEffects());
        effects.applyReady(level);
        effects.plan(radiation, level.getGameTime());
    }

    static void clear() {
        EFFECTS.clear();
    }

    static void unload(net.minecraft.resources.ResourceLocation dimension) {
        EFFECTS.remove(dimension);
    }

    private static final class WorldEffects {
        private CompletableFuture<EffectPlan> inFlight;
        private int fogTimer;

        void applyReady(ServerLevel level) {
            if (inFlight == null || !inFlight.isDone()) {
                return;
            }
            EffectPlan plan = inFlight.join();
            inFlight = null;
            applyPlan(level, plan);
        }

        void plan(Map<Long, Double> radiation, long gameTime) {
            if (inFlight != null) {
                return;
            }
            if (radiation.isEmpty()) {
                return;
            }
            boolean fogTick = ++fogTimer >= 20;
            if (fogTick) {
                fogTimer = 0;
            }
            long seed = gameTime * 31L + radiation.size() * 17L;
            inFlight = CompletableFuture.supplyAsync(() -> createPlan(radiation, fogTick, seed), PLANNER);
        }
    }

    private static EffectPlan createPlan(Map<Long, Double> snapshot, boolean fogTick, long seed) {
        if (snapshot.isEmpty()) {
            return EffectPlan.EMPTY;
        }
        SplittableRandom random = new SplittableRandom(seed);
        List<ChunkRad> entries = chunkEntries(snapshot);
        if (entries.isEmpty()) {
            return EffectPlan.EMPTY;
        }
        List<FogCandidate> fog = Collections.emptyList();
        if (fogTick) {
            fog = new ArrayList<>();
            for (int i = 0; i < FOG_CHUNK_ROLLS; i++) {
                ChunkRad entry = entries.get(random.nextInt(entries.size()));
                if (entry.radiation() > FOG_THRESHOLD && random.nextInt(FOG_CHANCE) == 0) {
                    fog.add(new FogCandidate(
                            entry.chunkX(),
                            entry.chunkZ(),
                            random.nextInt(16),
                            random.nextInt(16),
                            random.nextInt(5)
                    ));
                }
            }
        }

        List<SurfacePass> surfacePasses = new ArrayList<>(CHUNK_ROLLS_PER_TICK * LOCATION_ROLLS_PER_CHUNK);
        for (int c = 0; c < CHUNK_ROLLS_PER_TICK; c++) {
            ChunkRad entry = entries.get(random.nextInt(entries.size()));
            if (entry.radiation() < TERRAIN_THRESHOLD) {
                continue;
            }
            for (int i = 0; i < LOCATION_ROLLS_PER_CHUNK; i++) {
                surfacePasses.add(SurfacePass.create(entry.chunkX(), entry.chunkZ(), random));
            }
        }
        return new EffectPlan(fog, surfacePasses);
    }

    private static List<ChunkRad> chunkEntries(Map<Long, Double> snapshot) {
        List<ChunkRad> result = new ArrayList<>(snapshot.size());
        for (Map.Entry<Long, Double> entry : snapshot.entrySet()) {
            result.add(new ChunkRad(ChunkPos.getX(entry.getKey()), ChunkPos.getZ(entry.getKey()), entry.getValue()));
        }
        return result;
    }

    private static void applyPlan(ServerLevel level, EffectPlan plan) {
        Block wasteEarth = block("waste_earth");
        Block wasteLeaves = block("waste_leaves");
        for (FogCandidate candidate : plan.fog()) {
            spawnRadiationFog(level, candidate);
        }
        for (SurfacePass pass : plan.surfacePasses()) {
            contaminateChunkSurface(level, pass, wasteEarth, wasteLeaves);
        }
    }

    private static void spawnRadiationFog(ServerLevel level, FogCandidate candidate) {
        ChunkPos chunkPos = new ChunkPos(candidate.chunkX(), candidate.chunkZ());
        if (!level.hasChunk(chunkPos.x, chunkPos.z)) {
            return;
        }
        int x = chunkPos.getMinBlockX() + candidate.localX();
        int z = chunkPos.getMinBlockZ() + candidate.localZ();
        int y = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, new BlockPos(x, 0, z)).getY()
                + candidate.yOffset();
        level.sendParticles(HbmParticleTypes.RADIATION_FOG.get(), x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    private static void contaminateChunkSurface(ServerLevel level, SurfacePass pass, Block wasteEarth, Block wasteLeaves) {
        ChunkPos chunkPos = new ChunkPos(pass.chunkX(), pass.chunkZ());
        if (!level.hasChunk(chunkPos.x, chunkPos.z)) {
            return;
        }
        for (int a = 0; a < 16; a++) {
            for (int b = 0; b < 16; b++) {
                int index = a * 16 + b;
                if (!pass.shouldProcess(index)) {
                    continue;
                }
                int x = chunkPos.getMinBlockX() + a;
                int z = chunkPos.getMinBlockZ() + b;
                BlockPos surface = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, new BlockPos(x, 0, z));
                BlockPos pos = surface.below(pass.yDrop(index));
                BlockState state = level.getBlockState(pos);
                if (state.is(Blocks.GRASS_BLOCK)) {
                    level.setBlock(pos, wasteEarth.defaultBlockState(), 3);
                    HbmRadiationWorlds.invalidateResistance(level, pos);
                } else if (state.is(Blocks.SHORT_GRASS) || state.is(Blocks.TALL_GRASS) || state.is(Blocks.FERN) || state.is(Blocks.LARGE_FERN)) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    HbmRadiationWorlds.invalidateResistance(level, pos);
                } else if (isLeaves(state) && !state.is(wasteLeaves)) {
                    if (pass.leafToWaste(index)) {
                        level.setBlock(pos, wasteLeaves.defaultBlockState(), 3);
                    } else {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    }
                    HbmRadiationWorlds.invalidateResistance(level, pos);
                }
            }
        }
    }

    private static boolean isLeaves(BlockState state) {
        return state.is(BlockTags.LEAVES) || state.getBlock() instanceof LeavesBlock;
    }

    private static Block block(String id) {
        return BuiltInRegistries.BLOCK.get(ReinhardtsHBM.id(id));
    }

    private record EffectPlan(List<FogCandidate> fog, List<SurfacePass> surfacePasses) {
        private static final EffectPlan EMPTY = new EffectPlan(List.of(), List.of());
    }

    private record ChunkRad(int chunkX, int chunkZ, double radiation) {
    }

    private record FogCandidate(int chunkX, int chunkZ, int localX, int localZ, int yOffset) {
    }

    private record SurfacePass(int chunkX, int chunkZ, long processLo, long processMidLo, long processMidHi, long processHi,
                               long yDropLo, long yDropMidLo, long yDropMidHi, long yDropHi,
                               long leafWasteLo, long leafWasteMidLo, long leafWasteMidHi, long leafWasteHi) {
        static SurfacePass create(int chunkX, int chunkZ, SplittableRandom random) {
            long processLo = 0L;
            long processMidLo = 0L;
            long processMidHi = 0L;
            long processHi = 0L;
            long yDropLo = 0L;
            long yDropMidLo = 0L;
            long yDropMidHi = 0L;
            long yDropHi = 0L;
            long leafWasteLo = 0L;
            long leafWasteMidLo = 0L;
            long leafWasteMidHi = 0L;
            long leafWasteHi = 0L;

            for (int i = 0; i < 256; i++) {
                if (random.nextInt(3) == 0) {
                    processLo = setBit(processLo, i, 0);
                    processMidLo = setBit(processMidLo, i, 64);
                    processMidHi = setBit(processMidHi, i, 128);
                    processHi = setBit(processHi, i, 192);
                }
                if (random.nextInt(2) != 0) {
                    yDropLo = setBit(yDropLo, i, 0);
                    yDropMidLo = setBit(yDropMidLo, i, 64);
                    yDropMidHi = setBit(yDropMidHi, i, 128);
                    yDropHi = setBit(yDropHi, i, 192);
                }
                if (random.nextInt(7) <= 5) {
                    leafWasteLo = setBit(leafWasteLo, i, 0);
                    leafWasteMidLo = setBit(leafWasteMidLo, i, 64);
                    leafWasteMidHi = setBit(leafWasteMidHi, i, 128);
                    leafWasteHi = setBit(leafWasteHi, i, 192);
                }
            }
            return new SurfacePass(chunkX, chunkZ, processLo, processMidLo, processMidHi, processHi,
                    yDropLo, yDropMidLo, yDropMidHi, yDropHi,
                    leafWasteLo, leafWasteMidLo, leafWasteMidHi, leafWasteHi);
        }

        boolean shouldProcess(int index) {
            return bit(processLo, processMidLo, processMidHi, processHi, index);
        }

        int yDrop(int index) {
            return bit(yDropLo, yDropMidLo, yDropMidHi, yDropHi, index) ? 1 : 0;
        }

        boolean leafToWaste(int index) {
            return bit(leafWasteLo, leafWasteMidLo, leafWasteMidHi, leafWasteHi, index);
        }

        private static long setBit(long bits, int index, int base) {
            if (index < base || index >= base + 64) {
                return bits;
            }
            return bits | (1L << (index - base));
        }

        private static boolean bit(long lo, long midLo, long midHi, long hi, int index) {
            if (index < 64) {
                return (lo & (1L << index)) != 0L;
            }
            if (index < 128) {
                return (midLo & (1L << (index - 64))) != 0L;
            }
            if (index < 192) {
                return (midHi & (1L << (index - 128))) != 0L;
            }
            return (hi & (1L << (index - 192))) != 0L;
        }
    }
}
