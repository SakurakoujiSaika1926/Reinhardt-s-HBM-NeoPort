package com.reinhardt.hbm.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/** Direct modern representation of 1.7.10 {@code BlockLayering}'s eight metadata levels. */
public final class LayeringBlock extends Block {
    public static final IntegerProperty LAYERS = IntegerProperty.create("layers", 1, 8);
    private static final VoxelShape[] OUTLINE = new VoxelShape[8];
    private static final VoxelShape[] COLLISION = new VoxelShape[8];

    static {
        for (int i = 0; i < 8; i++) {
            OUTLINE[i] = Block.box(0.0D, 0.0D, 0.0D, 16.0D, (i + 1) * 2.0D, 16.0D);
            COLLISION[i] = i == 0
                    ? Shapes.empty()
                    : Block.box(0.0D, 0.0D, 0.0D, 16.0D, i * 2.0D, 16.0D);
        }
    }

    private final boolean freelyReplaceable;

    public LayeringBlock(Properties properties, boolean freelyReplaceable) {
        super(properties);
        this.freelyReplaceable = freelyReplaceable;
        registerDefaultState(stateDefinition.any().setValue(LAYERS, 1));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState existing = context.getLevel().getBlockState(context.getClickedPos());
        if (existing.is(this)) {
            return existing.setValue(LAYERS, Math.min(8, existing.getValue(LAYERS) + 1));
        }
        return defaultBlockState();
    }

    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        if (context.getItemInHand().is(asItem()) && state.getValue(LAYERS) < 8) {
            return !context.replacingClickedOnBlock() || context.getClickedFace() == Direction.UP;
        }
        return freelyReplaceable || state.getValue(LAYERS) < 8;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos belowPos = pos.below();
        BlockState below = level.getBlockState(belowPos);
        if (below.is(Blocks.ICE) || below.is(Blocks.PACKED_ICE)) {
            return false;
        }
        if (below.is(this)) {
            return below.getValue(LAYERS) == 8;
        }
        if (below.is(BlockTags.LEAVES)
                || below.getBlock() instanceof RbmkDebrisBlock
                || below.getBlock() instanceof ZirnoxDestroyedBlock) {
            return true;
        }
        return below.isFaceSturdy(level, belowPos, Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return direction == Direction.DOWN && !canSurvive(state, level, pos)
                ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return OUTLINE[state.getValue(LAYERS) - 1];
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return COLLISION[state.getValue(LAYERS) - 1];
    }

    @Override
    protected List<net.minecraft.world.item.ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LAYERS);
    }
}
