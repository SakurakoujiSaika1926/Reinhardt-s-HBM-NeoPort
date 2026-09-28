package com.reinhardt.hbm.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * The press preheater occupies a centered 3x1x3 square. Its authored offsets
 * are therefore the complete footprint for every horizontal facing; no
 * directional transform is part of this machine's placement contract.
 */
public final class PressPreheaterBlock extends LargeMachineBlock {
    @Override
    public net.minecraft.world.level.block.state.BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        if (!this.canPlaceMachineFootprintAt(context.getLevel(), context.getClickedPos(), facing, context)) {
            return null;
        }
        return this.defaultBlockState().setValue(FACING, facing);
    }

    @Override
    protected net.minecraft.world.level.block.state.BlockState rotate(
            net.minecraft.world.level.block.state.BlockState state,
            net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected net.minecraft.world.level.block.state.BlockState mirror(
            net.minecraft.world.level.block.state.BlockState state,
            net.minecraft.world.level.block.Mirror mirror) {
        return state.setValue(FACING, mirror.getRotation(state.getValue(FACING)).rotate(state.getValue(FACING)));
    }

    private static final Footprint FOOTPRINT = Footprint.centered(1, 1, 1);

    public PressPreheaterBlock(Properties properties) {
        super(properties, FOOTPRINT, PressPreheaterBlock::mapOffset);
    }

    private static BlockPos mapOffset(Direction facing, BlockPos authoredOffset) {
        return switch (facing) {
            case NORTH -> new BlockPos(authoredOffset.getX(), authoredOffset.getY(), authoredOffset.getZ());
            case SOUTH -> new BlockPos(authoredOffset.getX(), authoredOffset.getY(), authoredOffset.getZ());
            case WEST -> new BlockPos(authoredOffset.getX(), authoredOffset.getY(), authoredOffset.getZ());
            case EAST -> new BlockPos(authoredOffset.getX(), authoredOffset.getY(), authoredOffset.getZ());
            default -> throw new IllegalArgumentException("Press preheater facing must be horizontal: " + facing);
        };
    }
}
