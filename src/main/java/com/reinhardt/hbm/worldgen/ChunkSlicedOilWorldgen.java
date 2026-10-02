package com.reinhardt.hbm.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;

/**
 * Deterministic seed and chunk-slicing helpers for the large oil deposits.
 *
 * <p>A deposit is owned by a source chunk, but every generated chunk rebuilds
 * the same source-chunk candidates and writes only its own horizontal slice.
 * This preserves deposits wider than Minecraft's feature write radius without
 * requesting or mutating a chunk outside the active world-generation region.</p>
 */
final class ChunkSlicedOilWorldgen {
    static final long OIL_DEPOSIT_SALT = 0x6A09E667F3BCC909L;
    static final long OIL_SAND_DEPOSIT_SALT = 0xBB67AE8584CAA73BL;
    static final long OIL_SURFACE_SALT = 0x3C6EF372FE94F82BL;
    static final long OIL_SAND_FUZZ_SALT = 0xA54FF53A5F1D36F1L;

    private ChunkSlicedOilWorldgen() {
    }

    static RandomSource sourceRandom(long worldSeed, int chunkX, int chunkZ, long salt) {
        long chunkKey = ChunkPos.asLong(chunkX, chunkZ);
        return RandomSource.create(mix64(worldSeed ^ salt ^ mix64(chunkKey)));
    }

    static int sourceChunkSearchRadius(int maximumBlockRadius) {
        return Math.floorDiv(Math.max(0, maximumBlockRadius) + 15, 16);
    }

    static int boundedCoordinateRandom(long seed, int x, int y, int z, int bound) {
        if (bound <= 1) {
            return 0;
        }
        long coordinateKey = BlockPos.asLong(x, y, z);
        return (int) Long.remainderUnsigned(mix64(seed ^ coordinateKey), bound);
    }

    static long indexedSeed(long seed, long salt, int index) {
        return mix64(seed ^ salt ^ mix64(Integer.toUnsignedLong(index)));
    }

    static boolean horizontallyIntersects(WorldgenFeatureBounds bounds, int centerX, int centerZ, int radius) {
        return bounds.intersects(
                centerX - radius,
                centerX + radius + 1,
                centerZ - radius,
                centerZ + radius + 1
        );
    }

    static int minConfiguredRadius(int first, int second) {
        return Math.min(first, second);
    }

    static int maxConfiguredRadius(int first, int second) {
        return Math.max(first, second);
    }

    private static long mix64(long value) {
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }

    record Deposit(BlockPos center, int radius, long detailSeed) {
        Deposit {
            if (radius < 1) {
                throw new IllegalArgumentException("Deposit radius must be positive");
            }
        }
    }
}
