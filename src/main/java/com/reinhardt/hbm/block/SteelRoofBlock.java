package com.reinhardt.hbm.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The 1.7.10 flat steel roof panel and its exact collision bounds. */
public final class SteelRoofBlock extends Block {
    private static final VoxelShape LEGACY_COLLISION = Block.box(0.0D, 0.0D, 0.0D,
            16.0D, 1.0D, 16.0D);

    public SteelRoofBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return LEGACY_COLLISION;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                            CollisionContext context) {
        return LEGACY_COLLISION;
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }
}
