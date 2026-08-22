package com.reinhardt.hbm.block;

import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** The sealed scrap core from an ancient tomb. Its gas release is part of the block behavior. */
public final class AncientScrapBlock extends RadiatingBlock {
    public AncientScrapBlock(Properties properties, double radiation) {
        super(properties, radiation);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        emitRadon(level, pos, Direction.values()[random.nextInt(6)]);
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block block,
            BlockPos fromPos,
            boolean isMoving
    ) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (level.isClientSide || level.random.nextInt(3) != 0) {
            return;
        }
        for (Direction direction : Direction.values()) {
            emitRadon(level, pos, direction);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (level.isClientSide || newState.getBlock() == this) {
            return;
        }

        // Matches BlockOutgas.breakBlock: only the diagonal shell around the broken block is filled.
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    int sum = dx + dy + dz;
                    if (Math.abs(sum) < 5 && Math.abs(sum) > 0) {
                        emitRadon(level, pos.offset(dx, dy, dz));
                    }
                }
            }
        }
    }

    @Override
    public void playerDestroy(
            Level level,
            Player player,
            BlockPos pos,
            BlockState state,
            @Nullable BlockEntity blockEntity,
            ItemStack tool
    ) {
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
        if (!level.isClientSide) {
            level.setBlock(pos, HbmBlocks.GAS_RADON_TOMB.get().defaultBlockState(), 3);
        }
    }

    private static void emitRadon(Level level, BlockPos target) {
        if (level.isEmptyBlock(target)) {
            level.setBlock(target, HbmBlocks.GAS_RADON_TOMB.get().defaultBlockState(), 3);
        }
    }

    private static void emitRadon(Level level, BlockPos pos, Direction direction) {
        emitRadon(level, pos.relative(direction));
    }
}
