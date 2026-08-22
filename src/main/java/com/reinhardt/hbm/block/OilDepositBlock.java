package com.reinhardt.hbm.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

public class OilDepositBlock extends Block {
    private final Supplier<? extends Block> emptyDeposit;

    public OilDepositBlock(Properties properties, Supplier<? extends Block> emptyDeposit) {
        super(properties);
        this.emptyDeposit = emptyDeposit;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        settleIntoEmptyDeposit(level, pos);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!state.is(oldState.getBlock())) {
            settleIntoEmptyDeposit(level, pos);
        }
    }

    private void settleIntoEmptyDeposit(Level level, BlockPos pos) {
        if (level.isClientSide) {
            return;
        }

        Block empty = this.emptyDeposit.get();
        BlockPos below = pos.below();
        if (level.getBlockState(below).is(empty)) {
            level.setBlock(pos, empty.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(below, defaultBlockState(), Block.UPDATE_ALL);
        }
    }
}
