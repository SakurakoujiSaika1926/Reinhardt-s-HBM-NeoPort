package com.reinhardt.hbm.block;

import com.mojang.serialization.MapCodec;
import com.reinhardt.hbm.radiation.ChunkRadiationData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;

public class FalloutFallingBlock extends FallingBlock {
    private static final MapCodec<FalloutFallingBlock> CODEC = simpleCodec(FalloutFallingBlock::new);

    public FalloutFallingBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends FallingBlock> codec() {
        return CODEC;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide) {
            level.scheduleTick(pos, this, 20);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        ChunkRadiationData.get(level).incrementRadiation(pos, 1.0D, 10_000.0D);
        super.tick(state, level, pos, random);
        if (level.getBlockState(pos).is(this)) {
            level.scheduleTick(pos, this, 20);
        }
    }
}
