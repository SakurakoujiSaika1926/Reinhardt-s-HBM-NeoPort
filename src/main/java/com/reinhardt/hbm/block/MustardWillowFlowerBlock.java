package com.reinhardt.hbm.block;

import com.mojang.serialization.MapCodec;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

public class MustardWillowFlowerBlock extends BushBlock implements BonemealableBlock {
    public static final IntegerProperty META = IntegerProperty.create("meta", 0, 5);
    public static final int FOXGLOVE = 0;
    public static final int TOBACCO = 1;
    public static final int NIGHTSHADE = 2;
    public static final int WEED = 3;
    public static final int CD0 = 4;
    public static final int CD1 = 5;

    private static final MapCodec<MustardWillowFlowerBlock> CODEC = simpleCodec(MustardWillowFlowerBlock::new);
    private static final VoxelShape SHAPE = box(2.0D, 0.0D, 2.0D, 14.0D, 13.0D, 14.0D);

    public MustardWillowFlowerBlock(Properties properties) {
        super(properties.randomTicks());
        this.registerDefaultState(this.stateDefinition.any().setValue(META, CD0));
    }

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return isLegacyPlantableSoil(state);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (this.isValidBonemealTarget(level, pos, state) && this.isBonemealSuccess(level, random, pos, state) && random.nextInt(3) == 0) {
            this.performBonemeal(level, random, pos, state);
        }
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        int meta = state.getValue(META);
        if (meta == WEED) {
            return level.getBlockState(pos.above()).isAir();
        }
        if (meta == CD0) {
            return hasAdjacentWater(level, pos);
        }
        if (meta == CD1) {
            return hasAdjacentWater(level, pos) && level.getBlockState(pos.above()).isAir();
        }
        return false;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        int meta = state.getValue(META);
        return (meta == WEED || meta == CD0 || meta == CD1) && random.nextFloat() < 0.33F;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        int meta = state.getValue(META);
        if (meta == WEED) {
            if (isDeadOrOily(level.getBlockState(pos.below()))) {
                level.setBlock(pos, HbmBlocks.PLANT_DEAD.get().defaultBlockState().setValue(DeadPlantBlock.KIND, DeadPlantBlock.Kind.GENERIC), 3);
                return;
            }
            if (level.getBlockState(pos.above()).isAir()) {
                level.setBlock(pos, HbmBlocks.PLANT_TALL.get().defaultBlockState().setValue(MustardWillowTallBlock.META, MustardWillowTallBlock.WEED), 3);
                level.setBlock(pos.above(), HbmBlocks.PLANT_TALL.get().defaultBlockState().setValue(MustardWillowTallBlock.META, MustardWillowTallBlock.WEED + 8), 3);
            }
            return;
        }
        if (meta == CD0) {
            level.setBlock(pos, state.setValue(META, CD1), 3);
            return;
        }
        if (meta == CD1 && level.getBlockState(pos.above()).isAir()) {
            level.setBlock(pos, HbmBlocks.PLANT_TALL.get().defaultBlockState().setValue(MustardWillowTallBlock.META, MustardWillowTallBlock.CD2), 3);
            level.setBlock(pos.above(), HbmBlocks.PLANT_TALL.get().defaultBlockState().setValue(MustardWillowTallBlock.META, MustardWillowTallBlock.CD2 + 8), 3);
        }
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = new ArrayList<>();
        drops.add(new ItemStack(HbmBlocks.PLANT_FLOWER.get()));
        return drops;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(META);
    }

    static boolean hasAdjacentWater(LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (level.getFluidState(below.relative(direction)).is(FluidTags.WATER)) {
                return true;
            }
        }
        return false;
    }

    static boolean isDeadOrOily(BlockState state) {
        return state.is(HbmBlocks.DIRT_DEAD.get()) || state.is(HbmBlocks.DIRT_OILY.get());
    }

    static boolean isLegacyPlantableSoil(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.DIRT)
                || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.PODZOL)
                || state.is(Blocks.FARMLAND)
                || state.is(HbmBlocks.DIRT_DEAD.get())
                || state.is(HbmBlocks.DIRT_OILY.get());
    }
}
