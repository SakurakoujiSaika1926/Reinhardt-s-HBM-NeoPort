package com.reinhardt.hbm.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class FlammableGasBlock extends HbmGasBlock {
    public FlammableGasBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected Direction firstDirection(Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0) {
            return random.nextBoolean() ? Direction.DOWN : Direction.UP;
        }
        return randomHorizontal(random);
    }

    @Override
    protected Direction secondDirection(Level level, BlockPos pos, RandomSource random) {
        return randomHorizontal(random);
    }

    @Override
    protected int delay(Level level, RandomSource random) {
        return random.nextInt(5) + 16;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        for (Direction direction : Direction.values()) {
            if (isFireSource(level.getBlockState(pos.relative(direction)))) {
                combust(level, pos);
                return;
            }
        }
        if (random.nextInt(20) == 0 && level.isEmptyBlock(pos.below())) {
            level.removeBlock(pos, false);
            return;
        }
        super.tick(state, level, pos, random);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
        if (!level.isClientSide && entity.isOnFire()) {
            combust(level, pos);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (!level.isClientSide) {
            for (Direction direction : Direction.values()) {
                if (isFireSource(level.getBlockState(pos.relative(direction)))) {
                    level.scheduleTick(pos, this, 2);
                    return;
                }
            }
        }
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (!level.isClientSide() && isFireSource(neighborState)) {
            level.scheduleTick(pos, this, 2);
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    protected void combust(Level level, BlockPos pos) {
        level.setBlock(pos, Blocks.FIRE.defaultBlockState(), 3);
    }

    protected boolean isFireSource(BlockState state) {
        return state.is(Blocks.FIRE)
                || state.is(Blocks.LAVA)
                || state.is(Blocks.TORCH)
                || state.is(Blocks.WALL_TORCH)
                || state.is(Blocks.JACK_O_LANTERN);
    }
}
