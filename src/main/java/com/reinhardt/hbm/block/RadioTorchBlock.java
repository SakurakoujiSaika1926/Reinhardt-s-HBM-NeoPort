package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.RadioTorchBlockEntity;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Six-sided redstone-over-radio endpoint variants from 1.7.10. The block is
 * attached to a supporting face while its active face points away from that
 * support, matching the old metadata placement convention.
 */
public final class RadioTorchBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public enum Kind {
        SENDER,
        RECEIVER,
        COUNTER,
        LOGIC,
        READER,
        CONTROLLER
    }

    private final Kind kind;

    public RadioTorchBlock(Properties properties, Kind kind) {
        super(properties.noOcclusion().noCollission());
        this.kind = kind;
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.UP)
                .setValue(POWERED, false));
    }

    public Kind kind() {
        return kind;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getClickedFace();
        BlockState placed = defaultBlockState().setValue(FACING, facing);
        return placed.canSurvive(context.getLevel(), context.getClickedPos()) ? placed : null;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos support = pos.relative(state.getValue(FACING).getOpposite());
        BlockState supportState = level.getBlockState(support);
        return supportState.isFaceSturdy(level, support, state.getValue(FACING))
                || !supportState.isAir();
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
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state.getValue(FACING));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return kind == Kind.RECEIVER || kind == Kind.LOGIC;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        if (!isSignalSource(state)) {
            return 0;
        }
        BlockEntity entity = level.getBlockEntity(pos);
        return entity instanceof RadioTorchBlockEntity radio ? radio.signal() : 0;
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return getSignal(state, level, pos, direction);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hitResult) {
        if (level.getBlockEntity(pos) instanceof RadioTorchBlockEntity radio) {
            if (!level.isClientSide) {
                radio.interact(player);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RadioTorchBlockEntity(pos, state, kind);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                    BlockEntityType<T> type) {
        return type == HbmBlockEntities.RADIO_TORCH.get()
                ? (tickLevel, tickPos, tickState, entity) -> RadioTorchBlockEntity.tick(
                tickLevel, tickPos, tickState, (RadioTorchBlockEntity) entity)
                : null;
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
        builder.add(FACING, POWERED);
    }

    private static VoxelShape shapeFor(Direction facing) {
        double minX = facing == Direction.EAST ? 0.0D : 0.375D;
        double maxX = facing == Direction.WEST ? 1.0D : 0.625D;
        double minY = facing == Direction.UP ? 0.0D : 0.375D;
        double maxY = facing == Direction.DOWN ? 1.0D : 0.625D;
        double minZ = facing == Direction.SOUTH ? 0.0D : 0.375D;
        double maxZ = facing == Direction.NORTH ? 1.0D : 0.625D;
        return Shapes.box(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
