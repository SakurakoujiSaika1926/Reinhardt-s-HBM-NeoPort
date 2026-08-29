package com.reinhardt.hbm.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Biome stone with the exposed side layer behavior from BlockBiomeStone. */
public final class BiomeStoneBlock extends Block {
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 1);
    public static final BooleanProperty EXPOSED = BooleanProperty.create("exposed");
    private static final VoxelShape FULL = Shapes.block();

    public BiomeStoneBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(VARIANT, 0).setValue(EXPOSED, true));
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return updateExposure(this.defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return direction == Direction.UP ? updateExposure(state, level, pos) : state;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return FULL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(VARIANT, EXPOSED);
    }

    private static BlockState updateExposure(BlockState state, BlockGetter level, BlockPos pos) {
        BlockState above = level.getBlockState(pos.above());
        boolean sameVariant = above.getBlock() == state.getBlock()
                && above.hasProperty(VARIANT)
                && above.getValue(VARIANT).equals(state.getValue(VARIANT));
        return state.setValue(EXPOSED, !sameVariant);
    }
}
