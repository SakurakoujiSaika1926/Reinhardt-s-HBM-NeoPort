package com.reinhardt.hbm.worldgen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.MachineDummyBlock;
import com.reinhardt.hbm.config.HbmConfig;
import com.reinhardt.hbm.radiation.ChunkRadiationData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@EventBusSubscriber(modid = ReinhardtsHBM.MOD_ID)
public final class NuclearFalloutTerrainEffects {
    private static final int DEFAULT_TICK_DELAY = 4;
    private static final int MAX_CHUNKS_PER_BATCH = 1;
    private static final int WOOD_EFFECT_RANGE_PERCENT = 65;
    private static final Map<ResourceLocation, Deque<FalloutTask>> TASKS = new HashMap<>();

    private NuclearFalloutTerrainEffects() {
    }

    public static void scheduleRbmkMeltdown(ServerLevel level, BlockPos center, boolean digamma) {
        int scale = rbmkFalloutRange();
        if (scale <= 0) {
            return;
        }
        schedule(level, center, scale, rbmkFalloutDelay());
    }

    public static void schedule(ServerLevel level, BlockPos center, int scale) {
        schedule(level, center, scale, DEFAULT_TICK_DELAY);
    }

    public static void schedule(ServerLevel level, BlockPos center, int scale, int tickDelay) {
        TASKS.computeIfAbsent(level.dimension().location(), unused -> new ArrayDeque<>())
                .addLast(FalloutTask.create(center, Math.max(1, scale), Math.max(0, tickDelay)));
    }

    public static void scheduleDeferred(ServerLevel level, BlockPos center, int scale, int startDelay) {
        TASKS.computeIfAbsent(level.dimension().location(), unused -> new ArrayDeque<>())
                .addLast(FalloutTask.createDeferred(center, Math.max(1, scale), DEFAULT_TICK_DELAY, Math.max(0, startDelay)));
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Deque<FalloutTask> tasks = TASKS.get(level.dimension().location());
        if (tasks == null || tasks.isEmpty()) {
            return;
        }
        FalloutTask task = tasks.peekFirst();
        if (task.tick(level)) {
            tasks.removeFirst();
        }
        if (tasks.isEmpty()) {
            TASKS.remove(level.dimension().location());
        }
    }

    private static void processChunk(ServerLevel level, FalloutTask task, long chunkKey, boolean outer) {
        int chunkX = unpackX(chunkKey);
        int chunkZ = unpackZ(chunkKey);
        LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
        if (chunk == null) {
            return;
        }
        int minX = chunkX << 4;
        int minZ = chunkZ << 4;
        for (int x = minX; x < minX + 16; x++) {
            for (int z = minZ; z < minZ + 16; z++) {
                double distance = Math.hypot(x - (task.center.getX() + 0.5D), z - (task.center.getZ() + 0.5D));
                if (outer && distance > task.scale) {
                    continue;
                }
                stomp(level, chunk, task, x, z, distance * 100.0D / task.scale);
            }
        }
    }

    private static void stomp(ServerLevel level, LevelChunk chunk, FalloutTask task, int x, int z, double distancePercent) {
        int depth = 0;
        int minY = level.getMinBuildHeight();
        int maxY = Math.min(level.getMaxBuildHeight() - 2, chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, x & 15, z & 15) + 1);
        if (maxY < minY) {
            return;
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos above = new BlockPos.MutableBlockPos();
        RandomSource random = level.random;

        for (int y = maxY; y >= minY; y--) {
            if (depth >= 3) {
                return;
            }

            pos.set(x, y, z);
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || state.is(block("fallout")) || state.getFluidState().getType() != Fluids.EMPTY) {
                continue;
            }

            above.set(x, y + 1, z);
            BlockState aboveState = level.getBlockState(above);
            if (depth == 0 && canReceiveFallout(level, above, aboveState)) {
                double d = distancePercent / 100.0D;
                double chance = 0.1D - Math.pow(d - 0.7D, 2.0D);
                if (chance >= random.nextDouble()) {
                    setIfPresent(level, above, "fallout");
                    ChunkRadiationData.get(level).incrementRadiation(above, 0.25D, 10_000.0D);
                }
            }

            if (distancePercent < WOOD_EFFECT_RANGE_PERCENT && state.ignitedByLava()) {
                if (random.nextInt(5) == 0 && aboveState.isAir()) {
                    level.setBlock(above, Blocks.FIRE.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }

            boolean converted = convert(level, pos, state, distancePercent, random);
            if (converted) {
                if (isSolidFalloutTarget(level, pos, level.getBlockState(pos))) {
                    depth++;
                }
                continue;
            }

            if (state.isCollisionShapeFullBlock(level, pos)) {
                depth++;
            }
        }
    }

    private static boolean convert(ServerLevel level, BlockPos pos, BlockState state, double distancePercent, RandomSource random) {
        if (distancePercent <= WOOD_EFFECT_RANGE_PERCENT) {
            if (state.is(BlockTags.LOGS)) {
                return setIfPresent(level, pos, "waste_log");
            }
            if (state.is(BlockTags.PLANKS)) {
                return setIfPresent(level, pos, "waste_planks");
            }
            if (state.is(BlockTags.LEAVES) || state.getBlock() instanceof LeavesBlock || state.is(BlockTags.FLOWERS)
                || state.is(BlockTags.SAPLINGS) || state.is(BlockTags.REPLACEABLE_BY_TREES)
                    || state.is(Blocks.VINE) || state.is(Blocks.SNOW) || state.is(Blocks.SHORT_GRASS)
                    || state.is(Blocks.TALL_GRASS) || state.is(Blocks.FERN) || state.is(Blocks.LARGE_FERN)) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                return true;
            }
        } else if (state.is(BlockTags.LEAVES) || state.getBlock() instanceof LeavesBlock) {
            return setIfPresent(level, pos, "waste_leaves");
        }

        if (state.is(Blocks.MYCELIUM)) {
            return setIfPresent(level, pos, "waste_mycelium");
        }
        if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.ROOTED_DIRT)) {
            return setIfPresent(level, pos, "waste_earth");
        }
        if (state.is(Blocks.SAND) && random.nextDouble() < 0.05D) {
            return setIfPresent(level, pos, "waste_trinitite");
        }
        if (state.is(Blocks.RED_SAND) && random.nextDouble() < 0.05D) {
            return setIfPresent(level, pos, "waste_trinitite_red");
        }
        if (state.is(Blocks.CLAY)) {
            level.setBlock(pos, Blocks.TERRACOTTA.defaultBlockState(), Block.UPDATE_CLIENTS);
            return true;
        }

        if (distancePercent <= 50.0D) {
            if (state.is(Blocks.COAL_ORE) || state.is(Blocks.DEEPSLATE_COAL_ORE)) {
                return weightedSet(level, pos, random, "ore_sellafield_diamond", 3, "ore_sellafield_emerald", 2, "sellafield_slaked", 5);
            }
            if (state.is(Blocks.DIAMOND_ORE) || state.is(Blocks.DEEPSLATE_DIAMOND_ORE)) {
                return setIfPresent(level, pos, "ore_sellafield_radgem");
            }
            if (state.is(Blocks.EMERALD_ORE) || state.is(Blocks.DEEPSLATE_EMERALD_ORE)) {
                return setIfPresent(level, pos, "ore_sellafield_emerald");
            }
            if (state.is(block("ore_lignite")) || state.is(block("ore_deepslate_lignite"))) {
                return weightedSet(level, pos, random, "ore_sellafield_diamond", 1, "sellafield_slaked", 4);
            }
            if (state.is(block("ore_beryllium")) || state.is(block("ore_deepslate_beryllium"))) {
                return setIfPresent(level, pos, "ore_sellafield_emerald");
            }
            if (state.is(block("ore_uranium"))) {
                return weightedSet(level, pos, random, "ore_sellafield_schrabidium", 1, "ore_sellafield_uranium_scorched", 9);
            }
            if (state.is(block("ore_deepslate_uranium"))) {
                return weightedSet(level, pos, random, "ore_deepslate_schrabidium", 1, "ore_deepslate_uranium_scorched", 9);
            }
            if (state.is(Blocks.BEDROCK) || state.is(block("ore_bedrock_block")) || state.is(block("ore_bedrock_oil"))) {
                return setIfPresent(level, pos, "sellafield_bedrock");
            }
            if (state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.BASE_STONE_NETHER)
                    || state.is(BlockTags.SAND) || state.is(BlockTags.DIRT)) {
                return setIfPresent(level, pos, "sellafield_slaked");
            }
        }

        if (state.is(block("ore_nether_uranium"))) {
            return weightedSet(level, pos, random, "ore_nether_schrabidium", 1, "ore_nether_uranium_scorched", 99);
        }
        if (state.is(block("ore_gneiss_uranium"))) {
            return setIfPresent(level, pos, "ore_gneiss_uranium_scorched");
        }
        return false;
    }

    private static boolean weightedSet(ServerLevel level, BlockPos pos, RandomSource random, String first, int firstWeight, String second, int secondWeight) {
        return weightedSet(level, pos, random, first, firstWeight, second, secondWeight, null, 0);
    }

    private static boolean weightedSet(ServerLevel level, BlockPos pos, RandomSource random, String first, int firstWeight, String second, int secondWeight, String third, int thirdWeight) {
        int total = firstWeight + secondWeight + Math.max(0, thirdWeight);
        int value = random.nextInt(total);
        if (value < firstWeight) {
            return setIfPresent(level, pos, first);
        }
        if (value < firstWeight + secondWeight) {
            return setIfPresent(level, pos, second);
        }
        return third != null && setIfPresent(level, pos, third);
    }

    private static boolean canReceiveFallout(ServerLevel level, BlockPos pos, BlockState state) {
        if (!(state.isAir() || state.canBeReplaced())) {
            return false;
        }
        Block fallout = block("fallout");
        return fallout != Blocks.AIR && fallout.defaultBlockState().canSurvive(level, pos);
    }

    private static boolean setIfPresent(ServerLevel level, BlockPos pos, String id) {
        Block block = block(id);
        if (block == Blocks.AIR) {
            return false;
        }
        level.setBlock(pos, block.defaultBlockState(), Block.UPDATE_CLIENTS);
        return true;
    }

    private static Block block(String id) {
        return BuiltInRegistries.BLOCK.get(ReinhardtsHBM.id(id));
    }

    private static boolean isSolidFalloutTarget(ServerLevel level, BlockPos pos, BlockState state) {
        return state.isCollisionShapeFullBlock(level, pos) && !(state.getBlock() instanceof FireBlock) && !(state.getBlock() instanceof MachineDummyBlock);
    }

    private static long pack(int x, int z) {
        return (x & 0xffffffffL) | ((long) z << 32);
    }

    private static int unpackX(long packed) {
        return (int) (packed & 0xffffffffL);
    }

    private static int unpackZ(long packed) {
        return (int) (packed >> 32);
    }

    private static int rbmkFalloutRange() {
        try {
            return Math.max(0, Math.min(1024, HbmConfig.RBMK_FALLOUT_RANGE.get()));
        } catch (IllegalStateException ignored) {
            return 100;
        }
    }

    private static int rbmkFalloutDelay() {
        try {
            return Math.max(0, Math.min(200, HbmConfig.RBMK_FALLOUT_DELAY.get()));
        } catch (IllegalStateException ignored) {
            return DEFAULT_TICK_DELAY;
        }
    }

    private static final class FalloutTask {
        private final BlockPos center;
        private final int scale;
        private final int configuredTickDelay;
        private final Deque<Long> innerChunks;
        private final Deque<Long> outerChunks;
        private int tickDelay;
        private int startDelay;

        private FalloutTask(BlockPos center, int scale, int configuredTickDelay, int startDelay, Deque<Long> innerChunks, Deque<Long> outerChunks) {
            this.center = center;
            this.scale = scale;
            this.configuredTickDelay = configuredTickDelay;
            this.tickDelay = configuredTickDelay;
            this.startDelay = startDelay;
            this.innerChunks = innerChunks;
            this.outerChunks = outerChunks;
        }

        static FalloutTask create(BlockPos center, int scale, int tickDelay) {
            return createDeferred(center, scale, tickDelay, 0);
        }

        static FalloutTask createDeferred(BlockPos center, int scale, int tickDelay, int startDelay) {
            List<Long> inner = new ArrayList<>();
            List<Long> outer = new ArrayList<>();
            gatherChunks(center, scale, inner, outer);
            return new FalloutTask(center.immutable(), scale, tickDelay, startDelay, new ArrayDeque<>(inner), new ArrayDeque<>(outer));
        }

        boolean tick(ServerLevel level) {
            if (startDelay-- > 0) {
                return false;
            }
            if (tickDelay-- > 0) {
                return false;
            }
            tickDelay = configuredTickDelay;
            int processed = 0;
            while (processed < MAX_CHUNKS_PER_BATCH) {
                if (!innerChunks.isEmpty()) {
                    processChunk(level, this, innerChunks.removeLast(), false);
                    processed++;
                    continue;
                }
                if (!outerChunks.isEmpty()) {
                    processChunk(level, this, outerChunks.removeLast(), true);
                    processed++;
                    continue;
                }
                return true;
            }
            return false;
        }

        private static void gatherChunks(BlockPos center, int scale, List<Long> inner, List<Long> outer) {
            Set<Long> innerSet = new LinkedHashSet<>();
            Set<Long> outerSet = new LinkedHashSet<>();
            int adjustedMaxAngle = Math.max(1, 20 * scale / 32);
            for (int angle = 0; angle <= adjustedMaxAngle; angle++) {
                double radians = angle * Math.PI / 180.0D / (adjustedMaxAngle / 360.0D);
                int x = (int) Math.floor(center.getX() + Math.cos(radians) * scale) >> 4;
                int z = (int) Math.floor(center.getZ() + Math.sin(radians) * scale) >> 4;
                outerSet.add(pack(x, z));
            }
            for (int distance = 0; distance <= scale; distance += 8) {
                for (int angle = 0; angle <= adjustedMaxAngle; angle++) {
                    double radians = angle * Math.PI / 180.0D / (adjustedMaxAngle / 360.0D);
                    int x = (int) Math.floor(center.getX() + Math.cos(radians) * distance) >> 4;
                    int z = (int) Math.floor(center.getZ() + Math.sin(radians) * distance) >> 4;
                    long key = pack(x, z);
                    if (!outerSet.contains(key)) {
                        innerSet.add(key);
                    }
                }
            }
            inner.addAll(innerSet);
            outer.addAll(outerSet);
        }
    }
}
