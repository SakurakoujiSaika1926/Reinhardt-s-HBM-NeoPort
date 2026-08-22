package com.reinhardt.hbm.block;

import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class OutgassingRadiatingBlock extends RadiatingBlock {
    public OutgassingRadiatingBlock(Properties properties, double radiation) {
        super(properties, radiation);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide && oldState.getBlock() != state.getBlock()) {
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.tick(state, level, pos, random);
        emitRadon(level, pos, Direction.values()[random.nextInt(Direction.values().length)]);
        level.scheduleTick(pos, this, 1);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, net.minecraft.world.level.block.Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (level.isClientSide || level.random.nextInt(3) != 0) {
            return;
        }
        for (Direction direction : Direction.values()) {
            emitRadon(level, pos, direction);
        }
    }

    private static void emitRadon(Level level, BlockPos pos, Direction direction) {
        BlockPos target = pos.relative(direction);
        if (level.isEmptyBlock(target)) {
            level.setBlock(target, HbmBlocks.GAS_RADON.get().defaultBlockState(), 3);
        }
    }
}
