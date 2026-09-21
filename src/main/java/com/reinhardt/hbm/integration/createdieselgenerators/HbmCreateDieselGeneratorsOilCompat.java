package com.reinhardt.hbm.integration.createdieselgenerators;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;

public final class HbmCreateDieselGeneratorsOilCompat {
    private static final String MOD_ID = "createdieselgenerators";
    private static final String OIL_CHUNKS_SAVED_DATA_CLASS =
            "com.jesz.createdieselgenerators.world.OilChunksSavedData";

    private static boolean initialized;
    private static Method getChunkOilAmount;
    private static Method setChunkOilAmount;
    private static Method removeChunk;

    private HbmCreateDieselGeneratorsOilCompat() {
    }

    public static boolean hasOil(ServerLevel level, BlockPos pos) {
        return getChunkOilAmount(level, pos) > 0;
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    public static int getChunkOilAmount(ServerLevel level, BlockPos pos) {
        return level == null || pos == null ? 0 : getChunkOilAmount(level, new ChunkPos(pos));
    }

    public static int getChunkOilAmount(ServerLevel level, ChunkPos chunk) {
        if (level == null || chunk == null || !initialize()) {
            return 0;
        }
        try {
            Object result = getChunkOilAmount.invoke(null, level, chunk);
            return result instanceof Integer amount ? amount : 0;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            return 0;
        }
    }

    /**
     * Reads a chunk's oil amount and stores the result in Create Diesel Generators' own saved data.
     * CDG otherwise recalculates untouched chunks by scanning their biomes on every read.
     */
    public static int materializeChunkOilAmount(ServerLevel level, ChunkPos chunk) {
        if (level == null || chunk == null || !initialize()) {
            return 0;
        }
        try {
            Object result = getChunkOilAmount.invoke(null, level, chunk);
            if (!(result instanceof Integer amount)) {
                return 0;
            }
            setChunkOilAmount.invoke(null, level, chunk, Math.max(0, amount));
            return amount;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            return 0;
        }
    }

    public static boolean setChunkOilAmount(ServerLevel level, BlockPos pos, int amount) {
        return level != null && pos != null && setChunkOilAmount(level, new ChunkPos(pos), amount);
    }

    public static boolean setChunkOilAmount(ServerLevel level, ChunkPos chunk, int amount) {
        if (level == null || chunk == null || !initialize()) {
            return false;
        }
        try {
            setChunkOilAmount.invoke(null, level, chunk, Math.max(0, amount));
            return true;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            return false;
        }
    }

    public static boolean removeChunk(ServerLevel level, ChunkPos chunk) {
        if (level == null || chunk == null || !initialize()) {
            return false;
        }
        try {
            removeChunk.invoke(null, level, chunk);
            return true;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            return false;
        }
    }

    public static boolean isBedrock(BlockState state) {
        return state != null && state.is(Blocks.BEDROCK);
    }

    private static boolean initialize() {
        if (initialized) {
            return getChunkOilAmount != null && setChunkOilAmount != null && removeChunk != null;
        }
        initialized = true;
        if (!ModList.get().isLoaded(MOD_ID)) {
            return false;
        }
        try {
            Class<?> savedData = Class.forName(OIL_CHUNKS_SAVED_DATA_CLASS);
            getChunkOilAmount = savedData.getMethod("getChunkOilAmount", ServerLevel.class, ChunkPos.class);
            setChunkOilAmount = savedData.getMethod("setChunkOilAmount", ServerLevel.class, ChunkPos.class, int.class);
            removeChunk = savedData.getMethod("removeChunk", ServerLevel.class, ChunkPos.class);
            return true;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            getChunkOilAmount = null;
            setChunkOilAmount = null;
            removeChunk = null;
            return false;
        }
    }
}
