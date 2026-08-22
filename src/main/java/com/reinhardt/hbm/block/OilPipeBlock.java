package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.OilDerrickBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class OilPipeBlock extends Block {
    public OilPipeBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            notifyDerrickAbove(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    private static void notifyDerrickAbove(Level level, BlockPos pipePos) {
        BlockPos.MutableBlockPos cursor = pipePos.mutable();
        for (int y = pipePos.getY() + 1; y < level.getMaxBuildHeight(); y++) {
            cursor.set(pipePos.getX(), y, pipePos.getZ());
            BlockEntity blockEntity = level.getBlockEntity(cursor);
            if (blockEntity instanceof OilDerrickBlockEntity derrick) {
                derrick.invalidateDrillCursor();
                return;
            }
        }
    }
}
