package com.reinhardt.hbm.block;

import com.reinhardt.hbm.item.ScrewdriverItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SteelWallBlock extends Block {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape WALL_NORTH = box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 2.0D);
    private static final VoxelShape WALL_SOUTH = box(0.0D, 0.0D, 14.0D, 16.0D, 16.0D, 16.0D);
    private static final VoxelShape WALL_WEST = box(0.0D, 0.0D, 0.0D, 2.0D, 16.0D, 16.0D);
    private static final VoxelShape WALL_EAST = box(14.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);

    private static final VoxelShape CORNER_NORTH = Shapes.or(
            box(0.0D, 0.0D, 0.0D, 12.0D, 16.0D, 2.0D),
            box(12.0D, 0.0D, 0.0D, 16.0D, 16.0D, 4.0D),
            box(14.0D, 0.0D, 4.0D, 16.0D, 16.0D, 16.0D)
    );
    private static final VoxelShape CORNER_SOUTH = Shapes.or(
            box(4.0D, 0.0D, 14.0D, 16.0D, 16.0D, 16.0D),
            box(0.0D, 0.0D, 12.0D, 4.0D, 16.0D, 16.0D),
            box(0.0D, 0.0D, 0.0D, 2.0D, 16.0D, 12.0D)
    );
    private static final VoxelShape CORNER_WEST = Shapes.or(
            box(0.0D, 0.0D, 4.0D, 2.0D, 16.0D, 16.0D),
            box(0.0D, 0.0D, 0.0D, 4.0D, 16.0D, 4.0D),
            box(4.0D, 0.0D, 0.0D, 16.0D, 16.0D, 2.0D)
    );
    private static final VoxelShape CORNER_EAST = Shapes.or(
            box(14.0D, 0.0D, 0.0D, 16.0D, 16.0D, 12.0D),
            box(12.0D, 0.0D, 12.0D, 16.0D, 16.0D, 16.0D),
            box(0.0D, 0.0D, 14.0D, 12.0D, 16.0D, 16.0D)
    );

    private final Kind kind;

    public SteelWallBlock(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, facingForLegacyPlacement(context.getRotation()));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (this.kind == Kind.CORNER) {
            return Shapes.block();
        }
        return wallShape(state.getValue(FACING));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.kind == Kind.CORNER ? cornerShape(state.getValue(FACING)) : wallShape(state.getValue(FACING));
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        return this.getCollisionShape(state, level, pos, CollisionContext.empty());
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        if (!ScrewdriverItem.isScrewdriver(stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (!level.isClientSide) {
            Direction facing = state.getValue(FACING);
            Direction rotated = player.isShiftKeyDown() ? facing.getCounterClockWise() : facing.getClockWise();
            level.setBlock(pos, state.setValue(FACING, rotated), 3);
            ScrewdriverItem.damageTool(stack, level, player, hand);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
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
        return switch (meta & 7) {
            case 2 -> Direction.SOUTH;
            case 4 -> Direction.EAST;
            case 5 -> Direction.WEST;
            default -> Direction.NORTH;
        };
    }

    public static int toLegacyMeta(Direction direction) {
        return switch (direction) {
            case SOUTH -> 2;
            case EAST -> 4;
            case WEST -> 5;
            default -> 3;
        };
    }

    /**
     * Exact 1.7.10 DecoBlock placement mapping:
     * 0 -> meta 3 (north), 1 -> meta 4 (east), 2 -> meta 2 (south),
     * 3 -> meta 5 (west).  This is equivalent to the player's opposite
     * horizontal direction for ordinary placement, but keeping the legacy
     * formula here prevents future "facing" refactors from flipping it.
     */
    private static Direction facingForLegacyPlacement(float yaw) {
        int quadrant = Mth.floor((double)(yaw * 4.0F / 360.0F) + 0.5D) & 3;
        return switch (quadrant) {
            case 1 -> Direction.EAST;
            case 2 -> Direction.SOUTH;
            case 3 -> Direction.WEST;
            default -> Direction.NORTH;
        };
    }

    private static VoxelShape wallShape(Direction facing) {
        return switch (facing) {
            case SOUTH -> WALL_SOUTH;
            case EAST -> WALL_EAST;
            case WEST -> WALL_WEST;
            default -> WALL_NORTH;
        };
    }

    private static VoxelShape cornerShape(Direction facing) {
        return switch (facing) {
            case SOUTH -> CORNER_SOUTH;
            case EAST -> CORNER_EAST;
            case WEST -> CORNER_WEST;
            default -> CORNER_NORTH;
        };
    }

    public enum Kind {
        WALL,
        CORNER
    }
}
