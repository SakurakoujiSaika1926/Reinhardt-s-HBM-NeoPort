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

    private WorldgenFeatureBounds(ChunkPos center) {
        this.minBlockX = (center.x - FEATURES_WRITE_RADIUS_CHUNKS) << 4;
        this.maxBlockXExclusive = (center.x + FEATURES_WRITE_RADIUS_CHUNKS + 1) << 4;
        this.minBlockZ = (center.z - FEATURES_WRITE_RADIUS_CHUNKS) << 4;
        this.maxBlockZExclusive = (center.z + FEATURES_WRITE_RADIUS_CHUNKS + 1) << 4;
    }

    static WorldgenFeatureBounds forFeature(WorldGenLevel level, BlockPos origin) {
        ChunkPos center = level instanceof WorldGenRegion region
                ? region.getCenter()
                : new ChunkPos(origin);
        return new WorldgenFeatureBounds(center);
    }

    boolean contains(int blockX, int blockZ) {
        return blockX >= this.minBlockX
                && blockX < this.maxBlockXExclusive
                && blockZ >= this.minBlockZ
                && blockZ < this.maxBlockZExclusive;
    }
}
