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
        INCANDESCENT(2),
        FLUORESCENT(8),
        HALOGEN(32);

        private final int beamLength;

        Kind(int beamLength) {
            this.beamLength = beamLength;
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
        return switch (this.kind) {
            case INCANDESCENT -> incandescentStateForPlacement(context);
            case FLUORESCENT -> fluorescentStateForPlacement(context);
            case HALOGEN -> halogenStateForPlacement(context);
        };
    }

    private BlockState incandescentStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getClickedFace();
        BlockState state = defaultBlockState().setValue(FACING, facing);
        return canSurvive(state, context.getLevel(), context.getClickedPos()) ? state : null;
    }

    private BlockState fluorescentStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getClickedFace();
        BlockState state = defaultBlockState().setValue(FACING, facing);
        return canSurvive(state, context.getLevel(), context.getClickedPos()) ? state : null;
    }

    private BlockState halogenStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getClickedFace();
        BlockState state = defaultBlockState().setValue(FACING, facing);
        return canSurvive(state, context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        return switch (kind) {
            case INCANDESCENT -> incandescentCanSurvive(level, pos, facing);
            case FLUORESCENT -> fluorescentCanSurvive(level, pos, facing);
            case HALOGEN -> halogenCanSurvive(level, pos, facing);
        };
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
        if (level.isClientSide) {
            if (level.getBlockEntity(pos) instanceof SpotlightBlockEntity spotlight) {
                spotlight.refreshClientRenderSnapshot();
            }
            return;
        }
        if (block instanceof SpotlightBeamBlock) {
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
        if (!lit) {
            return;
        }
        Direction facing = state.getValue(FACING);
        switch (kind) {
            case INCANDESCENT -> propagateIncandescentBeam(level, pos, facing);
            case FLUORESCENT -> propagateFluorescentBeam(level, pos, facing);
            case HALOGEN -> propagateHalogenBeam(level, pos, facing);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide && lit) {
            Direction facing = state.getValue(FACING);
            switch (kind) {
                case INCANDESCENT -> unpropagateIncandescentBeam(level, pos, facing);
                case FLUORESCENT -> unpropagateFluorescentBeam(level, pos, facing);
                case HALOGEN -> unpropagateHalogenBeam(level, pos, facing);
            }
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
        Direction facing = state.getValue(FACING);
        return switch (kind) {
            case INCANDESCENT -> incandescentShape(facing);
            case FLUORESCENT -> fluorescentShape(facing);
            case HALOGEN -> halogenShape(facing);
        };
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
        return switch (this.kind) {
            case INCANDESCENT -> rotateIncandescent(state, rotation);
            case FLUORESCENT -> rotateFluorescent(state, rotation);
            case HALOGEN -> rotateHalogen(state, rotation);
        };
    }

    @Override
    public BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return switch (this.kind) {
            case INCANDESCENT -> mirrorIncandescent(state, mirror);
            case FLUORESCENT -> mirrorFluorescent(state, mirror);
            case HALOGEN -> mirrorHalogen(state, mirror);
        };
    }

    private static BlockState rotateIncandescent(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateFluorescent(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateHalogen(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState mirrorIncandescent(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorFluorescent(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorHalogen(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
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

    private static boolean incandescentCanSurvive(LevelReader level, BlockPos pos, Direction facing) {
        BlockPos support = switch (facing) {
            case DOWN -> pos.above();
            case UP -> pos.below();
            case NORTH -> pos.south();
            case SOUTH -> pos.north();
            case WEST -> pos.east();
            case EAST -> pos.west();
        };
        return level.getBlockState(support).isFaceSturdy(level, support, facing);
    }

    private static boolean fluorescentCanSurvive(LevelReader level, BlockPos pos, Direction facing) {
        BlockPos support = switch (facing) {
            case DOWN -> pos.above();
            case UP -> pos.below();
            case NORTH -> pos.south();
            case SOUTH -> pos.north();
            case WEST -> pos.east();
            case EAST -> pos.west();
        };
        return level.getBlockState(support).isFaceSturdy(level, support, facing);
    }

    private static boolean halogenCanSurvive(LevelReader level, BlockPos pos, Direction facing) {
        BlockPos support = switch (facing) {
            case DOWN -> pos.above();
            case UP -> pos.below();
            case NORTH -> pos.south();
            case SOUTH -> pos.north();
            case WEST -> pos.east();
            case EAST -> pos.west();
        };
        return level.getBlockState(support).isFaceSturdy(level, support, facing);
    }

    private static void propagateIncandescentBeam(Level level, BlockPos pos, Direction facing) {
        switch (facing) {
            case DOWN -> SpotlightBeamBlockEntity.propagate(level, pos, Direction.DOWN, 2);
            case UP -> SpotlightBeamBlockEntity.propagate(level, pos, Direction.UP, 2);
            case NORTH -> SpotlightBeamBlockEntity.propagate(level, pos, Direction.NORTH, 2);
            case SOUTH -> SpotlightBeamBlockEntity.propagate(level, pos, Direction.SOUTH, 2);
            case WEST -> SpotlightBeamBlockEntity.propagate(level, pos, Direction.WEST, 2);
            case EAST -> SpotlightBeamBlockEntity.propagate(level, pos, Direction.EAST, 2);
        }
    }

    private static void propagateFluorescentBeam(Level level, BlockPos pos, Direction facing) {
        switch (facing) {
            case DOWN -> SpotlightBeamBlockEntity.propagate(level, pos, Direction.DOWN, 8);
            case UP -> SpotlightBeamBlockEntity.propagate(level, pos, Direction.UP, 8);
            case NORTH -> SpotlightBeamBlockEntity.propagate(level, pos, Direction.NORTH, 8);
            case SOUTH -> SpotlightBeamBlockEntity.propagate(level, pos, Direction.SOUTH, 8);
            case WEST -> SpotlightBeamBlockEntity.propagate(level, pos, Direction.WEST, 8);
            case EAST -> SpotlightBeamBlockEntity.propagate(level, pos, Direction.EAST, 8);
        }
    }

    private static void propagateHalogenBeam(Level level, BlockPos pos, Direction facing) {
        switch (facing) {
            case DOWN -> SpotlightBeamBlockEntity.propagate(level, pos, Direction.DOWN, 32);
            case UP -> SpotlightBeamBlockEntity.propagate(level, pos, Direction.UP, 32);
            case NORTH -> SpotlightBeamBlockEntity.propagate(level, pos, Direction.NORTH, 32);
            case SOUTH -> SpotlightBeamBlockEntity.propagate(level, pos, Direction.SOUTH, 32);
            case WEST -> SpotlightBeamBlockEntity.propagate(level, pos, Direction.WEST, 32);
            case EAST -> SpotlightBeamBlockEntity.propagate(level, pos, Direction.EAST, 32);
        }
    }

    private static void unpropagateIncandescentBeam(Level level, BlockPos pos, Direction facing) {
        switch (facing) {
            case DOWN -> SpotlightBeamBlockEntity.unpropagate(level, pos, Direction.DOWN);
            case UP -> SpotlightBeamBlockEntity.unpropagate(level, pos, Direction.UP);
            case NORTH -> SpotlightBeamBlockEntity.unpropagate(level, pos, Direction.NORTH);
            case SOUTH -> SpotlightBeamBlockEntity.unpropagate(level, pos, Direction.SOUTH);
            case WEST -> SpotlightBeamBlockEntity.unpropagate(level, pos, Direction.WEST);
            case EAST -> SpotlightBeamBlockEntity.unpropagate(level, pos, Direction.EAST);
        }
    }

    private static void unpropagateFluorescentBeam(Level level, BlockPos pos, Direction facing) {
        switch (facing) {
            case DOWN -> SpotlightBeamBlockEntity.unpropagate(level, pos, Direction.DOWN);
            case UP -> SpotlightBeamBlockEntity.unpropagate(level, pos, Direction.UP);
            case NORTH -> SpotlightBeamBlockEntity.unpropagate(level, pos, Direction.NORTH);
            case SOUTH -> SpotlightBeamBlockEntity.unpropagate(level, pos, Direction.SOUTH);
            case WEST -> SpotlightBeamBlockEntity.unpropagate(level, pos, Direction.WEST);
            case EAST -> SpotlightBeamBlockEntity.unpropagate(level, pos, Direction.EAST);
        }
    }

    private static void unpropagateHalogenBeam(Level level, BlockPos pos, Direction facing) {
        switch (facing) {
            case DOWN -> SpotlightBeamBlockEntity.unpropagate(level, pos, Direction.DOWN);
            case UP -> SpotlightBeamBlockEntity.unpropagate(level, pos, Direction.UP);
            case NORTH -> SpotlightBeamBlockEntity.unpropagate(level, pos, Direction.NORTH);
            case SOUTH -> SpotlightBeamBlockEntity.unpropagate(level, pos, Direction.SOUTH);
            case WEST -> SpotlightBeamBlockEntity.unpropagate(level, pos, Direction.WEST);
            case EAST -> SpotlightBeamBlockEntity.unpropagate(level, pos, Direction.EAST);
        }
    }

    private static VoxelShape incandescentShape(Direction facing) {
        // Spotlight#getBounds + swizzleBounds from 1.7.10, expanded explicitly.
        float x = 0.25F;
        float y = 0.20F;
        float z = 0.15F;
        return switch (facing) {
            case DOWN -> Shapes.box(0.5D - y, 1.0D - 2.0D * z, 0.5D - x,
                    0.5D + y, 1.0D, 0.5D + x);
            case UP -> Shapes.box(0.5D - y, 0.0D, 0.5D - x,
                    0.5D + y, 2.0D * z, 0.5D + x);
            case NORTH -> Shapes.box(0.5D - x, 0.5D - y, 1.0D - 2.0D * z,
                    0.5D + x, 0.5D + y, 1.0D);
            case SOUTH -> Shapes.box(0.5D - x, 0.5D - y, 0.0D,
                    0.5D + x, 0.5D + y, 2.0D * z);
            case WEST -> Shapes.box(1.0D - 2.0D * z, 0.5D - y, 0.5D - x,
                    1.0D, 0.5D + y, 0.5D + x);
            case EAST -> Shapes.box(0.0D, 0.5D - y, 0.5D - x,
                    2.0D * z, 0.5D + y, 0.5D + x);
        };
    }

    private static VoxelShape fluorescentShape(Direction facing) {
        float x = 0.50F;
        float y = 0.50F;
        float z = 0.10F;
        return switch (facing) {
            case DOWN -> Shapes.box(0.5D - y, 1.0D - 2.0D * z, 0.5D - x,
                    0.5D + y, 1.0D, 0.5D + x);
            case UP -> Shapes.box(0.5D - y, 0.0D, 0.5D - x,
                    0.5D + y, 2.0D * z, 0.5D + x);
            case NORTH -> Shapes.box(0.5D - x, 0.5D - y, 1.0D - 2.0D * z,
                    0.5D + x, 0.5D + y, 1.0D);
            case SOUTH -> Shapes.box(0.5D - x, 0.5D - y, 0.0D,
                    0.5D + x, 0.5D + y, 2.0D * z);
            case WEST -> Shapes.box(1.0D - 2.0D * z, 0.5D - y, 0.5D - x,
                    1.0D, 0.5D + y, 0.5D + x);
            case EAST -> Shapes.box(0.0D, 0.5D - y, 0.5D - x,
                    2.0D * z, 0.5D + y, 0.5D + x);
        };
    }

    private static VoxelShape halogenShape(Direction facing) {
        float x = 0.35F;
        float y = 0.25F;
        float z = 0.20F;
        return switch (facing) {
            case DOWN -> Shapes.box(0.5D - y, 1.0D - 2.0D * z, 0.5D - x,
                    0.5D + y, 1.0D, 0.5D + x);
            case UP -> Shapes.box(0.5D - y, 0.0D, 0.5D - x,
                    0.5D + y, 2.0D * z, 0.5D + x);
            case NORTH -> Shapes.box(0.5D - x, 0.5D - y, 1.0D - 2.0D * z,
                    0.5D + x, 0.5D + y, 1.0D);
            case SOUTH -> Shapes.box(0.5D - x, 0.5D - y, 0.0D,
                    0.5D + x, 0.5D + y, 2.0D * z);
            case WEST -> Shapes.box(1.0D - 2.0D * z, 0.5D - y, 0.5D - x,
                    1.0D, 0.5D + y, 0.5D + x);
            case EAST -> Shapes.box(0.0D, 0.5D - y, 0.5D - x,
                    2.0D * z, 0.5D + y, 0.5D + x);
        };
    }
}
