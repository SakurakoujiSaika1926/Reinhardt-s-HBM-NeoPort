package com.reinhardt.hbm.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class LauncherDummyBlock extends MachineDummyBlock {
    private final VoxelShape shape;
    private final boolean port;

    public LauncherDummyBlock(Properties properties, VoxelShape shape, boolean port) {
        super(properties);
        this.shape = shape;
        this.port = port;
    }

    public boolean isPort() {
        return this.port;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.shape;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.shape;
    }
}
