package com.reinhardt.hbm.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/** 1.7.10 BlockReeds: a surface plant that only survives directly above vanilla water and drops a stick. */
public final class ReedsBlock extends Block {
    private static final VoxelShape EMPTY = Shapes.empty();

    public ReedsBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return below.is(Blocks.WATER);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return EMPTY;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(new ItemStack(Items.STICK));
    }
}
