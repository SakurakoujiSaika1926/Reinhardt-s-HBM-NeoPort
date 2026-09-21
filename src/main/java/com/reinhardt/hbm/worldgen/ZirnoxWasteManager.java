package com.reinhardt.hbm.worldgen;

import com.reinhardt.hbm.ReinhardtsHBM;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/** Tick-budgeted form of ExplosionNukeGeneric#waste used by the legacy ZIRNOX meltdown. */
@EventBusSubscriber(modid = ReinhardtsHBM.MOD_ID)
public final class ZirnoxWasteManager {
    private static final int POSITIONS_PER_TICK = 4_096;
    private static final Map<ResourceKey<Level>, Deque<Task>> TASKS = new HashMap<>();

    private ZirnoxWasteManager() {
    }

    public static void schedule(ServerLevel level, BlockPos center, int radius) {
        if (radius <= 0) return;
        TASKS.computeIfAbsent(level.dimension(), unused -> new ArrayDeque<>())
                .addLast(new Task(center.immutable(), radius));
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        Deque<Task> queue = TASKS.get(level.dimension());
        if (queue == null || queue.isEmpty()) return;
        if (queue.peekFirst().tick(level)) queue.removeFirst();
        if (queue.isEmpty()) TASKS.remove(level.dimension());
    }

    private static void waste(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        if (block == Blocks.OAK_DOOR || block == Blocks.IRON_DOOR) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        } else if (block == Blocks.GRASS_BLOCK) {
            set(level, pos, "waste_earth");
        } else if (block == Blocks.MYCELIUM) {
            set(level, pos, "waste_mycelium");
        } else if (block == Blocks.SAND && level.random.nextInt(20) == 1) {
            set(level, pos, "waste_trinitite");
        } else if (block == Blocks.RED_SAND && level.random.nextInt(20) == 1) {
            set(level, pos, "waste_trinitite_red");
        } else if (block == Blocks.CLAY) {
            level.setBlock(pos, Blocks.TERRACOTTA.defaultBlockState(), Block.UPDATE_CLIENTS);
        } else if (block == Blocks.MOSSY_COBBLESTONE) {
            level.setBlock(pos, Blocks.COAL_ORE.defaultBlockState(), Block.UPDATE_CLIENTS);
        } else if (block == Blocks.COAL_ORE) {
            int roll = level.random.nextInt(10);
            if (roll >= 1 && roll <= 3) {
                level.setBlock(pos, Blocks.DIAMOND_ORE.defaultBlockState(), Block.UPDATE_CLIENTS);
            } else if (roll == 9) {
                level.setBlock(pos, Blocks.EMERALD_ORE.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        } else if (state.is(BlockTags.LOGS) || block == Blocks.MUSHROOM_STEM) {
            set(level, pos, "waste_log");
        } else if (block == Blocks.BROWN_MUSHROOM_BLOCK || block == Blocks.RED_MUSHROOM_BLOCK) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        } else if (state.is(BlockTags.PLANKS)) {
            set(level, pos, "waste_planks");
        } else if (is(block, "ore_uranium")) {
            set(level, pos, level.random.nextInt(100) == 1 ? "ore_schrabidium" : "ore_uranium_scorched");
        } else if (is(block, "ore_nether_uranium")) {
            set(level, pos, level.random.nextInt(100) == 1 ? "ore_nether_schrabidium" : "ore_nether_uranium_scorched");
        } else if (is(block, "ore_gneiss_uranium")) {
            set(level, pos, level.random.nextInt(100) == 1 ? "ore_gneiss_schrabidium" : "ore_gneiss_uranium_scorched");
        }
    }

    private static boolean is(Block block, String id) {
        return block == BuiltInRegistries.BLOCK.get(ReinhardtsHBM.id(id));
    }

    private static void set(ServerLevel level, BlockPos pos, String id) {
        Block block = BuiltInRegistries.BLOCK.get(ReinhardtsHBM.id(id));
        if (block != Blocks.AIR) level.setBlock(pos, block.defaultBlockState(), Block.UPDATE_CLIENTS);
    }

    private static final class Task {
        private final BlockPos center;
        private final int radius;
        private final int radiusHalfSquared;
        private int dx;
        private int dy;
        private int dz;

        private Task(BlockPos center, int radius) {
            this.center = center;
            this.radius = radius;
            this.radiusHalfSquared = radius * radius / 2;
            this.dx = this.dy = this.dz = -radius;
        }

        private boolean tick(ServerLevel level) {
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            for (int processed = 0; processed < POSITIONS_PER_TICK; processed++) {
                if (this.dx >= this.radius) return true;
                int distanceSquared = this.dx * this.dx + this.dy * this.dy + this.dz * this.dz;
                int fuzz = Math.max(1, this.radiusHalfSquared / 5);
                if (distanceSquared < this.radiusHalfSquared + level.random.nextInt(fuzz)) {
                    cursor.set(this.center.getX() + this.dx, this.center.getY() + this.dy, this.center.getZ() + this.dz);
                    if (level.isLoaded(cursor) && !level.getBlockState(cursor).isAir()) waste(level, cursor);
                }
                advance();
            }
            return false;
        }

        private void advance() {
            if (++this.dz >= this.radius) {
                this.dz = -this.radius;
                if (++this.dy >= this.radius) {
                    this.dy = -this.radius;
                    this.dx++;
                }
            }
        }
    }
}
