package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.DroneWaypointBlockEntity;
import com.reinhardt.hbm.blockentity.DroneRequestWaypointBlockEntity;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** 1.7.10 DroneWaypoint with face placement, zero collision and adjustable route height. */
public final class DroneWaypointBlock extends Block implements EntityBlock {
    public enum Kind { TRANSPORT, REQUEST }

    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    private final Kind kind;

    public DroneWaypointBlock(Properties properties) {
        this(properties, Kind.TRANSPORT);
    }

    public DroneWaypointBlock(Properties properties, Kind kind) {
        super(properties.noCollission().noOcclusion());
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState placed = defaultBlockState().setValue(FACING, context.getClickedFace());
        return placed.canSurvive(context.getLevel(), context.getClickedPos()) ? placed : null;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return selectionShape(state.getValue(FACING));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos support = pos.relative(facing.getOpposite());
        return level.getBlockState(support).isFaceSturdy(level, support, facing);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == state.getValue(FACING).getOpposite() && !state.canSurvive(level, pos)) {
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hitResult) {
        if (kind == Kind.TRANSPORT && level.getBlockEntity(pos) instanceof DroneWaypointBlockEntity waypoint) {
            if (!level.isClientSide) {
                waypoint.addHeight(player.isCrouching() ? -1 : 1);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, net.minecraft.world.InteractionHand hand,
                                              BlockHitResult hitResult) {
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return kind == Kind.TRANSPORT
                ? new DroneWaypointBlockEntity(pos, state)
                : new DroneRequestWaypointBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                    BlockEntityType<T> type) {
        if (kind == Kind.TRANSPORT && type == HbmBlockEntities.DRONE_WAYPOINT.get()) {
            return (tickLevel, tickPos, tickState, entity) -> DroneWaypointBlockEntity.tick(
                    tickLevel, tickPos, tickState, (DroneWaypointBlockEntity) entity);
        }
        if (kind == Kind.REQUEST && type == HbmBlockEntities.DRONE_WAYPOINT_REQUEST.get()) {
            return (tickLevel, tickPos, tickState, entity) -> DroneRequestWaypointBlockEntity.tick(
                    tickLevel, tickPos, tickState, (DroneRequestWaypointBlockEntity) entity);
        }
        return null;
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

    private static VoxelShape selectionShape(Direction direction) {
        return switch (direction) {
            case DOWN -> Block.box(6.0D, 6.0D, 6.0D, 10.0D, 16.0D, 10.0D);
            case UP -> Block.box(6.0D, 0.0D, 6.0D, 10.0D, 10.0D, 10.0D);
            case NORTH -> Block.box(6.0D, 6.0D, 6.0D, 10.0D, 10.0D, 16.0D);
            case SOUTH -> Block.box(6.0D, 6.0D, 0.0D, 10.0D, 10.0D, 10.0D);
            case WEST -> Block.box(6.0D, 6.0D, 6.0D, 16.0D, 10.0D, 10.0D);
            case EAST -> Block.box(0.0D, 6.0D, 6.0D, 10.0D, 10.0D, 10.0D);
        };
    }
}
