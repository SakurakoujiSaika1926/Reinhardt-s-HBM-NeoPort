package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.CableDiodeBlockEntity;
import com.reinhardt.hbm.item.ScrewdriverItem;
import com.reinhardt.hbm.power.PowerNetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** The one-way red-copper diode from the 1.7.10 network implementation. */
public final class CableDiodeBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;

    public CableDiodeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(NORTH, false)
                .setValue(EAST, false)
                .setValue(SOUTH, false)
                .setValue(WEST, false)
                .setValue(UP, false)
                .setValue(DOWN, false));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return withNeighborConnections(defaultBlockState().setValue(FACING, context.getHorizontalDirection()),
                context.getLevel(), context.getClickedPos());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        PowerNetworkManager.markDirty(level);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return state.setValue(propertyFor(direction), canConnectTo(level, pos, direction));
    }

    /**
     * Refresh the diode's rendered connection mask when a capability-backed
     * neighbour is created or removed after the initial shape update.
     */
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos,
                                   Block neighborBlock, BlockPos neighborPos,
                                   boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        refreshConnections(level, pos);
    }

    /** Refresh when a neighbour block entity appears/disappears in place. */
    @Override
    public void onNeighborChange(BlockState state, LevelReader level, BlockPos pos, BlockPos neighborPos) {
        if (level instanceof Level actual) {
            refreshConnections(actual, pos);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, NORTH, EAST, SOUTH, WEST, UP, DOWN);
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
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, net.minecraft.world.InteractionHand hand,
                                              BlockHitResult hitResult) {
        if (ScrewdriverItem.isScrewdriver(stack)) {
            if (!level.isClientSide) {
                CableDiodeBlockEntity diode = (CableDiodeBlockEntity) level.getBlockEntity(pos);
                if (diode != null) {
                    diode.increaseLevel();
                    ScrewdriverItem.damageTool(stack, level, player, hand);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (ScrewdriverItem.isHandDrill(stack)) {
            if (!level.isClientSide) {
                CableDiodeBlockEntity diode = (CableDiodeBlockEntity) level.getBlockEntity(pos);
                if (diode != null) {
                    diode.decreaseLevel();
                    ScrewdriverItem.damageTool(stack, level, player, hand);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (ScrewdriverItem.isDefuser(stack)) {
            if (!level.isClientSide) {
                CableDiodeBlockEntity diode = (CableDiodeBlockEntity) level.getBlockEntity(pos);
                if (diode != null) {
                    diode.cyclePriority();
                    ScrewdriverItem.damageTool(stack, level, player, hand);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            PowerNetworkManager.markDirty(level);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CableDiodeBlockEntity(pos, state);
    }

    public static boolean canConnectTo(LevelAccessor level, BlockPos pos, Direction direction) {
        BlockState neighbor = level.getBlockState(pos.relative(direction));
        return neighbor.getBlock() instanceof EnergyCableBlock
                || neighbor.getBlock() instanceof CableDiodeBlock
                || PowerNetworkManager.canCableConnectTo(level, pos, direction);
    }

    private static BlockState withNeighborConnections(BlockState state, LevelAccessor level, BlockPos pos) {
        BlockState updated = state;
        for (Direction direction : Direction.values()) {
            updated = updated.setValue(propertyFor(direction), canConnectTo(level, pos, direction));
        }
        return updated;
    }

    public static void refreshConnections(LevelAccessor level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof CableDiodeBlock diode)) {
            return;
        }
        BlockState updated = withNeighborConnections(state, level, pos);
        if (updated != state) {
            level.setBlock(pos, updated, Block.UPDATE_CLIENTS);
        }
    }

    private static BooleanProperty propertyFor(Direction direction) {
        return switch (direction) {
            case NORTH -> NORTH;
            case EAST -> EAST;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case UP -> UP;
            case DOWN -> DOWN;
        };
    }
}
