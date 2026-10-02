package com.reinhardt.hbm.block;

import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class OutgassingRadiatingBlock extends RadiatingBlock {
    public OutgassingRadiatingBlock(Properties properties, double radiation) {
        super(properties, radiation);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.randomTick(state, level, pos, random);
        emitRadon(level, pos, Direction.values()[random.nextInt(Direction.values().length)]);
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

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state,
                              BlockEntity blockEntity, ItemStack tool) {
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
        replaceWithRadon(level, pos);
    }

    @Override
    public void wasExploded(Level level, BlockPos pos, Explosion explosion) {
        super.wasExploded(level, pos, explosion);
        replaceWithRadon(level, pos);
    }

    private static void emitRadon(Level level, BlockPos pos, Direction direction) {
        BlockPos target = pos.relative(direction);
        if (level.isEmptyBlock(target)) {
            level.setBlock(target, HbmBlocks.GAS_RADON.get().defaultBlockState(), 3);
        }
    }

    private static void replaceWithRadon(Level level, BlockPos pos) {
        if (!level.isClientSide) {
            level.setBlock(pos, HbmBlocks.GAS_RADON.get().defaultBlockState(), Block.UPDATE_ALL);
        }
    }
}
