package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.FoundryFlowBlockEntity;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

public class FoundryChannelBlock extends Block implements EntityBlock {
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST = BooleanProperty.create("west");

    private static final VoxelShape CENTER = Shapes.box(0.3125D, 0.0D, 0.3125D, 0.6875D, 0.5D, 0.6875D);
    private static final VoxelShape NORTH_ARM = Shapes.box(0.3125D, 0.0D, 0.0D, 0.6875D, 0.5D, 0.3125D);
    private static final VoxelShape EAST_ARM = Shapes.box(0.6875D, 0.0D, 0.3125D, 1.0D, 0.5D, 0.6875D);
    private static final VoxelShape SOUTH_ARM = Shapes.box(0.3125D, 0.0D, 0.6875D, 0.6875D, 0.5D, 1.0D);
    private static final VoxelShape WEST_ARM = Shapes.box(0.0D, 0.0D, 0.3125D, 0.3125D, 0.5D, 0.6875D);

    public FoundryChannelBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, false)
                .setValue(EAST, false)
                .setValue(SOUTH, false)
                .setValue(WEST, false));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return withConnections(context.getLevel(), context.getClickedPos(), this.defaultBlockState());
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            Direction direction,
            BlockState neighborState,
            LevelAccessor level,
            BlockPos pos,
            BlockPos neighborPos
    ) {
        return direction.getAxis().isHorizontal()
                ? state.setValue(property(direction), canConnectTo(level, pos, direction))
                : state;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FoundryFlowBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return level.isClientSide ? null : createTicker(blockEntityType, HbmBlockEntities.FOUNDRY_FLOW.get(), FoundryFlowBlockEntity::tick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!stack.is(ItemTags.SHOVELS) || !(level.getBlockEntity(pos) instanceof FoundryFlowBlockEntity channel)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide && channel.scrape(player)) {
            level.playSound(null, pos, SoundEvents.AXE_SCRAPE, SoundSource.BLOCKS, 0.8F, 1.0F);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof FoundryFlowBlockEntity channel) {
            channel.dropContents(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state);
    }

    private static BlockState withConnections(BlockGetter level, BlockPos pos, BlockState state) {
        return state
                .setValue(NORTH, canConnectTo(level, pos, Direction.NORTH))
                .setValue(EAST, canConnectTo(level, pos, Direction.EAST))
                .setValue(SOUTH, canConnectTo(level, pos, Direction.SOUTH))
                .setValue(WEST, canConnectTo(level, pos, Direction.WEST));
    }

    public static boolean canConnectTo(BlockGetter level, BlockPos pos, Direction direction) {
        if (!direction.getAxis().isHorizontal()) {
            return false;
        }
        BlockPos neighborPos = pos.relative(direction);
        BlockState neighbor = level.getBlockState(neighborPos);
        if (neighbor.is(HbmBlocks.FOUNDRY_CHANNEL.get()) || neighbor.is(HbmBlocks.FOUNDRY_MOLD.get())) {
            return true;
        }
        if ((neighbor.is(HbmBlocks.FOUNDRY_OUTLET.get()) || neighbor.is(HbmBlocks.FOUNDRY_SLAGTAP.get()))
                && neighbor.hasProperty(FoundryOutletBlock.FACING)) {
            return neighbor.getValue(FoundryOutletBlock.FACING) == direction;
        }
        return false;
    }

    private static VoxelShape shapeFor(BlockState state) {
        VoxelShape shape = CENTER;
        if (state.getValue(NORTH)) {
            shape = Shapes.or(shape, NORTH_ARM);
        }
        if (state.getValue(EAST)) {
            shape = Shapes.or(shape, EAST_ARM);
        }
        if (state.getValue(SOUTH)) {
            shape = Shapes.or(shape, SOUTH_ARM);
        }
        if (state.getValue(WEST)) {
            shape = Shapes.or(shape, WEST_ARM);
        }
        return shape;
    }

    private static BooleanProperty property(Direction direction) {
        return switch (direction) {
            case EAST -> EAST;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            default -> NORTH;
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST);
    }

    @Nullable
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTicker(
            BlockEntityType<A> actual,
            BlockEntityType<E> expected,
            BlockEntityTicker<? super E> ticker
    ) {
        return actual == expected ? (BlockEntityTicker<A>) ticker : null;
    }
}
