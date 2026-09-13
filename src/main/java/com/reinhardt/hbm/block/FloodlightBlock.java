package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.FloodlightBlockEntity;
import com.reinhardt.hbm.blockentity.FloodlightDummyBlockEntity;
import com.reinhardt.hbm.item.ScrewdriverItem;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

import javax.annotation.Nullable;

public final class FloodlightBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty FLIPPED = BooleanProperty.create("flipped");

    /*
     * The legacy OBJ occupies x=-0.5..0.5, y=0..0.75 and z=-1.1875..1.1875
     * before RenderFloodlight applies its metadata rotation.  A plain Block
     * uses Shapes.block(), which made the modern hit/collision box stay cubic
     * while the OBJ moved.  Keep the complete collision/selection volume in
     * the same orientation as the rendered assembly.  The two invisible
     * footprint blocks still provide the three-block support line in-world.
     */
    private static final VoxelShape MODEL_DOWN = Shapes.box(0.0D, 0.25D, -11.0D / 16.0D,
            1.0D, 1.0D, 27.0D / 16.0D);
    private static final VoxelShape MODEL_UP = Shapes.box(0.0D, 0.0D, -11.0D / 16.0D,
            1.0D, 0.75D, 27.0D / 16.0D);
    private static final VoxelShape MODEL_NORTH = Shapes.box(-11.0D / 16.0D, 0.0D, 0.25D,
            27.0D / 16.0D, 1.0D, 1.0D);
    private static final VoxelShape MODEL_SOUTH = Shapes.box(-11.0D / 16.0D, 0.0D, 0.0D,
            27.0D / 16.0D, 1.0D, 0.75D);
    private static final VoxelShape MODEL_WEST = Shapes.box(0.25D, 0.0D, -11.0D / 16.0D,
            1.0D, 1.0D, 27.0D / 16.0D);
    private static final VoxelShape MODEL_EAST = Shapes.box(0.0D, 0.0D, -11.0D / 16.0D,
            0.75D, 1.0D, 27.0D / 16.0D);
    private static final VoxelShape MODEL_FLIPPED_VERTICAL = Shapes.box(-11.0D / 16.0D, 0.25D, 0.0D,
            27.0D / 16.0D, 1.0D, 1.0D);
    private static final VoxelShape MODEL_FLIPPED_UP = Shapes.box(-11.0D / 16.0D, 0.0D, 0.0D,
            27.0D / 16.0D, 0.75D, 1.0D);
    private static final VoxelShape MODEL_FLIPPED_HORIZONTAL = MODEL_FLIPPED_UP;

    public FloodlightBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP).setValue(FLIPPED, false));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getClickedFace();
        int quadrant = Mth.floor(context.getRotation() * 4.0F / 360.0F + 0.5D) & 3;
        boolean flipped = (facing == Direction.DOWN || facing == Direction.UP)
                && (quadrant == 0 || quadrant == 2);
        BlockState state = defaultBlockState().setValue(FACING, facing).setValue(FLIPPED, flipped);
        return canPlaceFootprint(context, state) ? state : null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer != null) {
            FloodlightBlockEntity.setAngle(level, pos, state, placer, true);
        }
        if (!level.isClientSide) {
            // setAngle may change FLIPPED for floor/ceiling placement.  The
            // legacy renderer turns the model's long axis by 90 degrees in
            // that state, so build the collision footprint from the final
            // world state instead of the stale pre-placement argument.
            BlockState placedState = level.getBlockState(pos);
            if (placedState.is(this)) {
                placeDummies(level, pos, placedState);
            }
        }
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return collisionShape(state);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                            CollisionContext context) {
        return collisionShape(state);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FloodlightBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != HbmBlockEntities.FLOODLIGHT.get()) {
            return null;
        }
        return (tickerLevel, tickerPos, tickerState, blockEntity) ->
                FloodlightBlockEntity.tick(tickerLevel, tickerPos, tickerState, (FloodlightBlockEntity) blockEntity);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (ScrewdriverItem.isScrewdriver(stack) && level.getBlockEntity(pos) instanceof FloodlightBlockEntity light) {
            light.setAngle(player, false);
            ScrewdriverItem.damageTool(stack, level, player, hand);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof FloodlightBlockEntity light) {
            light.destroyLights();
            if (!level.isClientSide) {
                removeDummies(level, pos);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** The OBJ's long axis is local Z; the legacy metadata renderer may rotate it onto world X. */
    public static Direction longAxis(BlockState state) {
        if (state.getValue(FLIPPED)) {
            return Direction.EAST;
        }
        Direction facing = state.getValue(FACING);
        return facing == Direction.NORTH || facing == Direction.SOUTH ? Direction.EAST : Direction.SOUTH;
    }

    /** Complete model bounds in the local coordinate system of the core block. */
    static VoxelShape collisionShape(BlockState state) {
        Direction facing = state.getValue(FACING);
        if (state.getValue(FLIPPED)) {
            return switch (facing) {
                case DOWN -> MODEL_FLIPPED_VERTICAL;
                case UP -> MODEL_FLIPPED_UP;
                // Legacy metadata 8..11 has no additional face rotation in
                // RenderFloodlight; all four horizontal variants share this
                // same quarter-turned base shape.
                case NORTH, SOUTH, WEST, EAST -> MODEL_FLIPPED_HORIZONTAL;
            };
        }
        return switch (facing) {
            case DOWN -> MODEL_DOWN;
            case UP -> MODEL_UP;
            case NORTH -> MODEL_NORTH;
            case SOUTH -> MODEL_SOUTH;
            case WEST -> MODEL_WEST;
            case EAST -> MODEL_EAST;
        };
    }

    private static List<BlockPos> footprint(BlockPos corePos, BlockState state) {
        Direction axis = longAxis(state);
        return List.of(corePos.relative(axis.getOpposite()), corePos, corePos.relative(axis));
    }

    private static boolean canPlaceFootprint(BlockPlaceContext context, BlockState state) {
        for (BlockPos partPos : footprint(context.getClickedPos(), state)) {
            if (partPos.equals(context.getClickedPos())) {
                continue;
            }
            if (!context.getLevel().getBlockState(partPos).canBeReplaced(context)) {
                return false;
            }
        }
        return true;
    }

    private static void placeDummies(Level level, BlockPos corePos, BlockState state) {
        for (BlockPos partPos : footprint(corePos, state)) {
            if (partPos.equals(corePos)) {
                continue;
            }
            level.setBlock(partPos, HbmBlocks.FLOODLIGHT_DUMMY.get().defaultBlockState(), Block.UPDATE_ALL);
            if (level.getBlockEntity(partPos) instanceof FloodlightDummyBlockEntity dummy) {
                dummy.setCorePos(corePos);
            }
        }
    }

    private static void removeDummies(Level level, BlockPos corePos) {
        FloodlightDummyBlock.runWithoutCoreDestroy(() -> {
            // Check both horizontal axes so floodlights placed by the old
            // stale-state code can still be removed cleanly after upgrading.
            for (Direction direction : List.of(Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST)) {
                BlockPos partPos = corePos.relative(direction);
                if (level.getBlockState(partPos).is(HbmBlocks.FLOODLIGHT_DUMMY.get())
                        && level.getBlockEntity(partPos) instanceof FloodlightDummyBlockEntity dummy
                        && dummy.corePos().equals(corePos)) {
                    level.removeBlock(partPos, false);
                }
            }
        });
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FLIPPED);
    }
}
