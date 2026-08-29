package com.reinhardt.hbm.block;

import com.reinhardt.hbm.config.HbmConfig;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Exact 1.7.10 CrystalVirus behavior, including the legacy six-neighbor spread. */
public final class CrystalVirusBlock extends Block {
    public CrystalVirusBlock(Properties properties) {
        super(properties.randomTicks());
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!HbmConfig.ENABLE_VIRUS.get()) {
            return;
        }

        for (BlockPos neighbor : neighbors(pos)) {
            BlockState neighborState = level.getBlockState(neighbor);
            if (!isCrystal(neighborState) && !neighborState.is(Blocks.AIR)) {
                level.setBlock(neighbor, HbmBlocks.CRYSTAL_VIRUS.get().defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        level.setBlock(pos, HbmBlocks.CRYSTAL_HARDENED.get().defaultBlockState(), Block.UPDATE_ALL);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                    BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (level.isClientSide) {
            return;
        }

        for (BlockPos neighbor : neighbors(pos)) {
            BlockState neighborState = level.getBlockState(neighbor);
            if (!neighborState.is(Blocks.AIR) && !isCrystal(neighborState)) {
                return;
            }
        }
        level.setBlock(pos, HbmBlocks.CRYSTAL_HARDENED.get().defaultBlockState(), Block.UPDATE_ALL);
    }

    public static boolean isCrystal(BlockState state) {
        return state.is(HbmBlocks.CRYSTAL_VIRUS.get())
                || state.is(HbmBlocks.CRYSTAL_HARDENED.get())
                || state.is(HbmBlocks.CRYSTAL_PULSAR.get());
    }

    public static void hardenVirus(ServerLevel level, BlockPos center, int radius) {
        int radiusSquaredHalf = radius * radius / 2;
        for (int x = -radius; x < radius; x++) {
            int xSquared = x * x;
            for (int y = -radius; y < radius; y++) {
                int xySquared = xSquared + y * y;
                for (int z = -radius; z < radius; z++) {
                    if (xySquared + z * z < radiusSquaredHalf) {
                        BlockPos target = center.offset(x, y, z);
                        if (level.getBlockState(target).is(HbmBlocks.CRYSTAL_VIRUS.get())) {
                            level.setBlock(target, HbmBlocks.CRYSTAL_HARDENED.get().defaultBlockState(), Block.UPDATE_ALL);
                        }
                    }
                }
            }
        }
    }

    private static BlockPos[] neighbors(BlockPos pos) {
        return new BlockPos[]{
                pos.east(), pos.above(), pos.south(), pos.west(), pos.below(), pos.north()
        };
    }
}
