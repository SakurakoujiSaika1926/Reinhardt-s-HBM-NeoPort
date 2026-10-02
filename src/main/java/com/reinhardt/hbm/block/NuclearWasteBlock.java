package com.reinhardt.hbm.block;

import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

/** Radiation source plus the dense-radon scheduled emission from 1.7.10 BlockNuclearWaste. */
public final class NuclearWasteBlock extends RadiatingBlock {
    public NuclearWasteBlock(Properties properties, double radiation) {
        super(properties, radiation);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        Direction direction = Direction.values()[random.nextInt(Direction.values().length)];
        BlockPos target = pos.relative(direction);
        if (random.nextBoolean() && level.isEmptyBlock(target)) {
            level.setBlock(target, HbmBlocks.GAS_RADON_DENSE.get().defaultBlockState(), 3);
        }
        super.tick(state, level, pos, random);
    }
}
