package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.WatzPumpBlockEntity;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class WatzPumpBlock extends Block implements EntityBlock {
    public WatzPumpBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WatzPumpBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos upper = context.getClickedPos().above();
        return context.getLevel().getBlockState(upper).canBeReplaced(context) ? defaultBlockState() : null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            BlockPos upper = pos.above();
            level.setBlock(upper, HbmBlocks.MACHINE_DUMMY.get().defaultBlockState(), Block.UPDATE_ALL);
            if (level.getBlockEntity(upper) instanceof com.reinhardt.hbm.blockentity.MachineDummyBlockEntity dummy) {
                dummy.setCorePos(pos);
            }
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) != null) {
            com.reinhardt.hbm.block.MachineDummyBlock.runWithoutCoreDestroy(() -> {
                BlockPos upper = pos.above();
                if (level.getBlockState(upper).is(HbmBlocks.MACHINE_DUMMY.get())
                        && level.getBlockEntity(upper) instanceof com.reinhardt.hbm.blockentity.MachineDummyBlockEntity dummy
                        && dummy.getCorePos().equals(pos)) {
                    level.removeBlock(upper, false);
                }
            });
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return false;
    }

    @Override
    public boolean canSurvive(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos) {
        return true;
    }
}
