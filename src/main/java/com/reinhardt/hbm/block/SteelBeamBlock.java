package com.reinhardt.hbm.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The legacy steel beam is a narrow, centered vertical beam, not a solid cube.
 * Its 1.7.10/1.12.2 collision bounds were {@code 7/16..9/16} on X/Z and the
 * full block height on Y.
 */
public final class SteelBeamBlock extends Block {
    /** Kept for parity with the legacy DecoBlock metadata variants. */
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape LEGACY_BEAM = box(7.0D, 0.0D, 7.0D, 9.0D, 16.0D, 9.0D);

    public SteelBeamBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // DecoBlock#getStateForPlacement stored the placer direction itself
        // (the beam's model is then rotated by the blockstate variant).
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return LEGACY_BEAM;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return LEGACY_BEAM;
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        // DecoBlock#getBlockFaceShape returned UNDEFINED for the legacy beam.
        return Shapes.empty();
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    public static Direction fromLegacyMeta(int meta) {
        // EnumFacing.byIndex(meta) in 1.12.2 normalized against the six
        // directions before collapsing vertical values to NORTH.
        return switch (Math.abs(meta % 6)) {
            // DecoBlock#getStateFromMeta used EnumFacing.byIndex(meta):
            // 2=NORTH, 3=SOUTH, 4=WEST, 5=EAST.
            case 3 -> Direction.SOUTH;
            case 4 -> Direction.WEST;
            case 5 -> Direction.EAST;
            default -> Direction.NORTH;
        };
    }

    public static int toLegacyMeta(Direction direction) {
        return switch (direction) {
            case SOUTH -> 3;
            case WEST -> 4;
            case EAST -> 5;
            default -> 2;
        };
    }
}
