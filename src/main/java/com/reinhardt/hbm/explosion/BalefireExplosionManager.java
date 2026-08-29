package com.reinhardt.hbm.explosion;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

/** Main-thread port of ExplosionBalefire's expanding square spiral. */
@EventBusSubscriber(modid = ReinhardtsHBM.MOD_ID)
public final class BalefireExplosionManager {
    private static final Map<ResourceLocation, Queue<Task>> TASKS = new HashMap<>();

    private BalefireExplosionManager() {
    }

    public static void schedule(ServerLevel level, BlockPos center, int radius) {
        TASKS.computeIfAbsent(level.dimension().location(), unused -> new ArrayDeque<>())
                .add(new Task(center, radius));
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Queue<Task> queue = TASKS.get(level.dimension().location());
        if (queue == null || queue.isEmpty()) {
            return;
        }
        if (queue.peek().tick(level)) {
            queue.remove();
        }
        if (queue.isEmpty()) {
            TASKS.remove(level.dimension().location());
        }
    }

    private static final class Task {
        private final BlockPos center;
        private final int radius;
        private final int limit;
        private int n = 1;
        private int lastX;
        private int lastZ;
        private int speed = 1;

        private Task(BlockPos center, int radius) {
            this.center = center.immutable();
            this.radius = radius;
            this.limit = radius * radius * 4;
        }

        private boolean tick(ServerLevel level) {
            this.speed++;
            for (int step = 0; step < this.speed; step++) {
                breakColumn(level, this.lastX, this.lastZ);
                int shell = (int) Math.floor((Math.sqrt(this.n) + 1.0D) / 2.0D);
                int shell2 = shell * 2;
                if (shell2 == 0) {
                    return true;
                }
                int leg = (int) Math.floor((this.n - (shell2 - 1) * (shell2 - 1)) / (double) shell2);
                int element = (this.n - (shell2 - 1) * (shell2 - 1)) - shell2 * leg - shell + 1;
                this.lastX = leg == 0 ? shell : leg == 1 ? -element : leg == 2 ? -shell : element;
                this.lastZ = leg == 0 ? element : leg == 1 ? shell : leg == 2 ? -element : -shell;
                this.n++;
                if (this.n > this.limit) {
                    return true;
                }
            }
            return false;
        }

        private void breakColumn(ServerLevel level, int x, int z) {
            int distance = (int) (this.radius - Math.sqrt(x * x + z * z));
            if (distance <= 0) {
                return;
            }
            int worldX = this.center.getX() + x;
            int worldZ = this.center.getZ() + z;
            BlockPos surface = new BlockPos(worldX, this.center.getY(), worldZ);
            if (!level.hasChunkAt(surface)) {
                return;
            }
            int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, worldX, worldZ);
            int maxDepth = (int) (10.0D + this.radius * 0.25D);
            int depth = (int) (maxDepth * distance / (double) this.radius + Math.sin(distance * 0.15D + 2.0D) * 2.0D);
            depth = Math.max(y - depth, level.getMinBuildHeight());
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(worldX, y, worldZ);
            while (cursor.getY() > depth) {
                BlockState state = level.getBlockState(cursor);
                if (state.is(HbmBlocks.BLOCK_SCHRABIDIUM_CLUSTER.get())) {
                    if (level.random.nextInt(10) == 0) {
                        level.setBlock(cursor.above(), HbmBlocks.BALEFIRE.get().defaultBlockState(), 2);
                        level.setBlock(cursor, HbmBlocks.BLOCK_EUPHEMIUM_CLUSTER.get().defaultBlockState(), 3);
                    }
                    return;
                }
                level.setBlock(cursor, Blocks.AIR.defaultBlockState(), 2);
                cursor.move(0, -1, 0);
            }
            if (level.random.nextInt(10) == 0) {
                level.setBlock(new BlockPos(worldX, depth + 1, worldZ), HbmBlocks.BALEFIRE.get().defaultBlockState(), 2);
                if (level.getBlockState(cursor).is(HbmBlocks.BLOCK_SCHRABIDIUM_CLUSTER.get())) {
                    level.setBlock(cursor, HbmBlocks.BLOCK_EUPHEMIUM_CLUSTER.get().defaultBlockState(), 3);
                }
            }
            for (int yOffset = depth; yOffset > depth - 5; yOffset--) {
                cursor.set(worldX, yOffset, worldZ);
                if (level.getBlockState(cursor).is(Blocks.STONE)) {
                    level.setBlock(cursor, HbmBlocks.SELLAFIELD_SLAKED.get().defaultBlockState(), 3);
                }
            }
        }
    }
}
