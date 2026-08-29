package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.SpotlightBlockEntity;
import com.reinhardt.hbm.blockentity.SpotlightBeamBlockEntity;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;

import java.util.List;

/**
 * The six-sided, redstone-inverted 1.7.10 spotlight blocks.
 *
 * The block has no collision, while its selection box is the exact oriented
 * half-size box from Spotlight#setBlockBoundsBasedOnState.
 */
public final class SpotlightBlock extends Block implements EntityBlock {
    public enum Kind {
        INCANDESCENT(2, 0.25F, 0.20F, 0.15F),
        FLUORESCENT(8, 0.50F, 0.50F, 0.10F),
        HALOGEN(32, 0.35F, 0.25F, 0.20F);

        private final int beamLength;
        private final float xHalf;
        private final float yHalf;
        private final float zHalf;

        Kind(int beamLength, float xHalf, float yHalf, float zHalf) {
            this.beamLength = beamLength;
            this.xHalf = xHalf;
            this.yHalf = yHalf;
            this.zHalf = zHalf;
        }

        public int beamLength() {
            return beamLength;
        }

        public boolean fluorescent() {
            return this == FLUORESCENT;
        }
    }

    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    private final Kind kind;
    private final boolean lit;

    public SpotlightBlock(Properties properties, Kind kind, boolean lit) {
        super(properties);
        this.kind = kind;
        this.lit = lit;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public Kind kind() {
        return kind;
    }

    public boolean isLit() {
        return lit;
    }

    public int beamLength() {
        return kind.beamLength();
    }

    public boolean canConnectTo(BlockState state) {
        return state.getBlock() == this;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getClickedFace();
        BlockState state = defaultBlockState().setValue(FACING, facing);
        return canSurvive(state, context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos supportPos = pos.relative(facing.getOpposite());
        return level.getBlockState(supportPos).isFaceSturdy(level, supportPos, facing);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide && !state.is(oldState.getBlock())) {
            if (updatePower(level, pos, state)) {
                return;
            }
            updateBeam(level, pos, state);
        }
    }

    private boolean updatePower(Level level, BlockPos pos, BlockState state) {
        boolean powered = level.hasNeighborSignal(pos);
        if (lit && powered) {
            level.scheduleTick(pos, this, 4);
            return true;
        }
        if (!lit && !powered) {
            level.setBlock(pos, onBlock().defaultBlockState().setValue(FACING, state.getValue(FACING)), Block.UPDATE_ALL);
            return true;
        }
        return false;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (lit && level.hasNeighborSignal(pos)) {
            level.setBlock(pos, offBlock().defaultBlockState().setValue(FACING, state.getValue(FACING)), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
                                   BlockPos fromPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, fromPos, movedByPiston);
        if (level.isClientSide || block instanceof SpotlightBeamBlock) {
            return;
        }
        if (!canSurvive(state, level, pos)) {
            popResource(level, pos, new ItemStack(onBlock()));
            level.removeBlock(pos, false);
            return;
        }
        if (updatePower(level, pos, state)) {
            return;
        }
        updateBeam(level, pos, state);
    }

    private void updateBeam(Level level, BlockPos pos, BlockState state) {
        if (lit) {
            SpotlightBeamBlockEntity.propagate(level, pos, state.getValue(FACING), beamLength());
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide && lit) {
            SpotlightBeamBlockEntity.unpropagate(level, pos, state.getValue(FACING));
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    private Block onBlock() {
        return switch (kind) {
            case INCANDESCENT -> HbmBlocks.SPOTLIGHT_INCANDESCENT.get();
            case FLUORESCENT -> HbmBlocks.SPOTLIGHT_FLUORO.get();
            case HALOGEN -> HbmBlocks.SPOTLIGHT_HALOGEN.get();
        };
    }

    private Block offBlock() {
        return switch (kind) {
            case INCANDESCENT -> HbmBlocks.SPOTLIGHT_INCANDESCENT_OFF.get();
            case FLUORESCENT -> HbmBlocks.SPOTLIGHT_FLUORO_OFF.get();
            case HALOGEN -> HbmBlocks.SPOTLIGHT_HALOGEN_OFF.get();
        };
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(new ItemStack(onBlock()));
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(onBlock());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        float[] bounds = switch (kind) {
            case FLUORESCENT -> new float[]{0.50F, 0.50F, 0.10F};
            case HALOGEN -> new float[]{0.35F, 0.25F, 0.20F};
            case INCANDESCENT -> new float[]{0.25F, 0.20F, 0.15F};
        };
        Direction direction = state.getValue(FACING);
        float x = bounds[0];
        float y = bounds[1];
        float z = bounds[2];
        if (direction == Direction.EAST || direction == Direction.WEST) {
            float swap = x;
            x = z;
            z = swap;
        } else if (direction == Direction.UP || direction == Direction.DOWN) {
            float swap = x;
            x = z;
            z = y;
            y = swap;
        }
        double ox = 0.5D - direction.getStepX() * (0.5D - x);
        double oy = 0.5D - direction.getStepY() * (0.5D - y);
        double oz = 0.5D - direction.getStepZ() * (0.5D - z);
        return Shapes.box(ox - x, oy - y, oz - z, ox + x, oy + y, oz + z);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SpotlightBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return null;
    }
}
