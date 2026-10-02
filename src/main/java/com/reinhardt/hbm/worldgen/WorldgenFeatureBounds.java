package com.reinhardt.hbm.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;

/**
 * Horizontal write bounds for features generated during Minecraft 1.21.1's
 * FEATURES chunk-status step.
 *
 * <p>That step permits block writes only in the center chunk and its eight
 * immediate neighbours.  {@link WorldGenRegion#ensureCanWrite(BlockPos)} logs
 * every rejected attempt, so callers must reject unbounded/random offsets
 * before they query heightmaps or block states in those chunks.</p>
 */
final class WorldgenFeatureBounds {
    private static final int FEATURES_WRITE_RADIUS_CHUNKS = 1;

    private final int minBlockX;
    private final int maxBlockXExclusive;
    private final int minBlockZ;
    private final int maxBlockZExclusive;

    private WorldgenFeatureBounds(ChunkPos center, int radiusChunks) {
        this.minBlockX = (center.x - radiusChunks) << 4;
        this.maxBlockXExclusive = (center.x + radiusChunks + 1) << 4;
        this.minBlockZ = (center.z - radiusChunks) << 4;
        this.maxBlockZExclusive = (center.z + radiusChunks + 1) << 4;
    }

    static WorldgenFeatureBounds forFeature(WorldGenLevel level, BlockPos origin) {
        return aroundGenerationCenter(level, origin, FEATURES_WRITE_RADIUS_CHUNKS);
    }

    static WorldgenFeatureBounds forCurrentChunk(WorldGenLevel level, BlockPos origin) {
        return aroundGenerationCenter(level, origin, 0);
    }

    private static WorldgenFeatureBounds aroundGenerationCenter(WorldGenLevel level, BlockPos origin, int radiusChunks) {
        ChunkPos center = level instanceof WorldGenRegion region
                ? region.getCenter()
                : new ChunkPos(origin);
        return new WorldgenFeatureBounds(center, radiusChunks);
    }

    boolean contains(int blockX, int blockZ) {
        return blockX >= this.minBlockX
                && blockX < this.maxBlockXExclusive
                && blockZ >= this.minBlockZ
                && blockZ < this.maxBlockZExclusive;
    }

    boolean intersects(int minX, int maxXExclusive, int minZ, int maxZExclusive) {
        return minX < this.maxBlockXExclusive
                && maxXExclusive > this.minBlockX
                && minZ < this.maxBlockZExclusive
                && maxZExclusive > this.minBlockZ;
    }

    int minBlockX() {
        return this.minBlockX;
    }

    int maxBlockXExclusive() {
        return this.maxBlockXExclusive;
    }

    int minBlockZ() {
        return this.minBlockZ;
    }

    int maxBlockZExclusive() {
        return this.maxBlockZExclusive;
    }
}
