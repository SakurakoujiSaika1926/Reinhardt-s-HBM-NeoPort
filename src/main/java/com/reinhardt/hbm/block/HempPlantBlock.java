package com.reinhardt.hbm.block;

import com.mojang.serialization.MapCodec;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/** The old WEED metadata states, flattened into one independently registered crop. */
public final class HempPlantBlock extends BushBlock implements BonemealableBlock {
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 1);
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final int SMALL = 0;
    public static final int MATURE = 1;

    private static final MapCodec<HempPlantBlock> CODEC = simpleCodec(HempPlantBlock::new);
    private static final VoxelShape SMALL_SHAPE = box(2.0D, 0.0D, 2.0D, 14.0D, 13.0D, 14.0D);
    private static final VoxelShape TALL_SHAPE = box(2.0D, 0.0D, 2.0D, 14.0D, 16.0D, 14.0D);

    public HempPlantBlock(Properties properties) {
        super(properties.randomTicks());
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(AGE, SMALL)
                .setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return HbmPlantBlock.isPlantableSoil(state);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            BlockState lower = level.getBlockState(pos.below());
            return state.getValue(AGE) == MATURE
                    && lower.is(this)
                    && lower.getValue(HALF) == DoubleBlockHalf.LOWER
                    && lower.getValue(AGE) == MATURE;
        }
        return HbmPlantBlock.isPlantableSoil(level.getBlockState(pos.below()));
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (!state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        if (state.getValue(HALF) == DoubleBlockHalf.LOWER
                && state.getValue(AGE) == MATURE
                && direction == Direction.UP
                && !isMatchingUpper(neighborState)) {
            return this.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            return;
        }
        if (state.getValue(AGE) == MATURE) {
            if (HbmPlantBlock.isDeadOrOily(level.getBlockState(pos.below()))) {
                level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                level.setBlock(pos, HbmBlocks.PLANT_DEAD.get().defaultBlockState()
                        .setValue(DeadPlantBlock.KIND, DeadPlantBlock.Kind.BIG_FLOWER), Block.UPDATE_ALL);
            }
            return;
        }
        if (this.isValidBonemealTarget(level, pos, state)
                && this.isBonemealSuccess(level, random, pos, state)
                && random.nextInt(3) == 0) {
            this.performBonemeal(level, random, pos, state);
        }
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        BlockPos lowerPos = lowerPos(pos, state);
        BlockState lower = level.getBlockState(lowerPos);
        return lower.is(this)
                && lower.getValue(HALF) == DoubleBlockHalf.LOWER
                && lower.getValue(AGE) == SMALL
                && level.getBlockState(lowerPos.above()).isAir();
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return random.nextFloat() < 0.33F;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        BlockPos lowerPos = lowerPos(pos, state);
        BlockState lower = level.getBlockState(lowerPos);
        if (!lower.is(this)
                || lower.getValue(HALF) != DoubleBlockHalf.LOWER
                || lower.getValue(AGE) != SMALL
                || !level.getBlockState(lowerPos.above()).isAir()) {
            return;
        }
        if (HbmPlantBlock.isDeadOrOily(level.getBlockState(lowerPos.below()))) {
            level.setBlock(lowerPos, HbmBlocks.PLANT_DEAD.get().defaultBlockState()
                    .setValue(DeadPlantBlock.KIND, DeadPlantBlock.Kind.GENERIC), Block.UPDATE_ALL);
            return;
        }
        setMature(level, lowerPos);
    }

    private void setMature(ServerLevel level, BlockPos lowerPos) {
        BlockState lower = this.defaultBlockState().setValue(AGE, MATURE).setValue(HALF, DoubleBlockHalf.LOWER);
        BlockState upper = lower.setValue(HALF, DoubleBlockHalf.UPPER);
        level.setBlock(lowerPos.above(), upper, Block.UPDATE_CLIENTS);
        level.setBlock(lowerPos, lower, Block.UPDATE_ALL);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(AGE) == SMALL ? SMALL_SHAPE : TALL_SHAPE;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide
                && state.getValue(HALF) == DoubleBlockHalf.LOWER
                && state.getValue(AGE) == MATURE) {
            BlockPos upperPos = pos.above();
            BlockState upper = level.getBlockState(upperPos);
            if (isMatchingUpper(upper)) {
                if (!player.isCreative()) {
                    dropResources(upper, level, upperPos, null, player, player.getMainHandItem());
                }
                level.setBlock(upperPos, Blocks.AIR.defaultBlockState(), 35);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(new ItemStack(HbmItems.PLANT_HEMP.get()));
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(HbmItems.PLANT_HEMP.get());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE, HALF);
    }

    private boolean isMatchingUpper(BlockState state) {
        return state.is(this)
                && state.getValue(AGE) == MATURE
                && state.getValue(HALF) == DoubleBlockHalf.UPPER;
    }

    private static BlockPos lowerPos(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
    }
}
