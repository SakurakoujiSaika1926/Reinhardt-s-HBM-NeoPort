package com.reinhardt.hbm.block;

import com.mojang.serialization.MapCodec;
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

import java.util.ArrayList;
import java.util.List;

/** The complete CD0-CD4 mustard-willow chain, without legacy metadata aliases. */
public final class MustardWillowPlantBlock extends BushBlock implements BonemealableBlock {
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 4);
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final int CD0 = 0;
    public static final int CD1 = 1;
    public static final int CD2 = 2;
    public static final int CD3 = 3;
    public static final int CD4 = 4;

    private static final MapCodec<MustardWillowPlantBlock> CODEC = simpleCodec(MustardWillowPlantBlock::new);
    private static final VoxelShape SMALL_SHAPE = box(2.0D, 0.0D, 2.0D, 14.0D, 13.0D, 14.0D);
    private static final VoxelShape TALL_SHAPE = box(2.0D, 0.0D, 2.0D, 14.0D, 16.0D, 14.0D);

    public MustardWillowPlantBlock(Properties properties) {
        super(properties.randomTicks());
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(AGE, CD0)
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
        int age = state.getValue(AGE);
        if (state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            BlockState lower = level.getBlockState(pos.below());
            return age >= CD2
                    && lower.is(this)
                    && lower.getValue(HALF) == DoubleBlockHalf.LOWER
                    && lower.getValue(AGE) == age;
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
                && state.getValue(AGE) >= CD2
                && direction == Direction.UP
                && !isMatchingUpper(neighborState, state.getValue(AGE))) {
            return this.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(HALF) == DoubleBlockHalf.UPPER) {
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
        if (!lower.is(this) || lower.getValue(HALF) != DoubleBlockHalf.LOWER) {
            return false;
        }
        int age = lower.getValue(AGE);
        if (age == CD0) {
            return HbmPlantBlock.hasAdjacentWater(level, lowerPos);
        }
        if (age == CD1) {
            return HbmPlantBlock.hasAdjacentWater(level, lowerPos)
                    && level.getBlockState(lowerPos.above()).isAir();
        }
        if (age == CD2) {
            return HbmPlantBlock.hasAdjacentWater(level, lowerPos);
        }
        if (age == CD3) {
            return HbmPlantBlock.hasAdjacentWater(level, lowerPos)
                    && HbmPlantBlock.isDeadOrOily(level.getBlockState(lowerPos.below()));
        }
        return false;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        BlockPos lowerPos = lowerPos(pos, state);
        BlockState lower = level.getBlockState(lowerPos);
        return lower.is(this)
                && (lower.getValue(AGE) == CD3 || random.nextFloat() < 0.33F);
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        BlockPos lowerPos = lowerPos(pos, state);
        BlockState lower = level.getBlockState(lowerPos);
        if (!lower.is(this) || lower.getValue(HALF) != DoubleBlockHalf.LOWER) {
            return;
        }
        int age = lower.getValue(AGE);
        if (age == CD0) {
            level.setBlock(lowerPos, lower.setValue(AGE, CD1), Block.UPDATE_ALL);
            return;
        }
        if (age == CD1) {
            if (level.getBlockState(lowerPos.above()).isAir()) {
                setTallAge(level, lowerPos, CD2);
            }
            return;
        }
        if (age != CD2 && age != CD3) {
            return;
        }
        setTallAge(level, lowerPos, age + 1);
        if (age == CD3) {
            level.setBlock(lowerPos.below(), Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    private void setTallAge(ServerLevel level, BlockPos lowerPos, int age) {
        BlockState lower = this.defaultBlockState().setValue(AGE, age).setValue(HALF, DoubleBlockHalf.LOWER);
        BlockState upper = lower.setValue(HALF, DoubleBlockHalf.UPPER);
        level.setBlock(lowerPos.above(), upper, Block.UPDATE_CLIENTS);
        level.setBlock(lowerPos, lower, Block.UPDATE_ALL);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(AGE) < CD2 ? SMALL_SHAPE : TALL_SHAPE;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide
                && state.getValue(HALF) == DoubleBlockHalf.LOWER
                && state.getValue(AGE) >= CD2) {
            BlockPos upperPos = pos.above();
            BlockState upper = level.getBlockState(upperPos);
            if (isMatchingUpper(upper, state.getValue(AGE))) {
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
        List<ItemStack> drops = new ArrayList<>();
        drops.add(new ItemStack(HbmItems.PLANT_MUSTARD_WILLOW.get()));
        if (state.getValue(AGE) == CD4 && state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            ItemStack leaves = HbmItems.variantStack(HbmItems.PLANT_ITEMS, "mustardwillow");
            leaves.setCount(3 + params.getLevel().random.nextInt(4));
            drops.add(leaves);
        }
        return drops;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(HbmItems.PLANT_MUSTARD_WILLOW.get());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE, HALF);
    }

    private boolean isMatchingUpper(BlockState state, int age) {
        return state.is(this)
                && state.getValue(AGE) == age
                && state.getValue(HALF) == DoubleBlockHalf.UPPER;
    }

    private static BlockPos lowerPos(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
    }
}
