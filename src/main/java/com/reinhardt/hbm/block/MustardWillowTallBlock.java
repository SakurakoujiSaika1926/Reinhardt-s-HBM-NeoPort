package com.reinhardt.hbm.block;

import com.reinhardt.hbm.item.LegacyVariantItem;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MustardWillowTallBlock extends Block implements BonemealableBlock {
    public static final IntegerProperty META = IntegerProperty.create("meta", 0, 11);
    public static final int WEED = 0;
    public static final int CD2 = 1;
    public static final int CD3 = 2;
    public static final int CD4 = 3;

    private static final VoxelShape SHAPE = box(2.0D, 0.0D, 2.0D, 14.0D, 16.0D, 14.0D);

    public MustardWillowTallBlock(Properties properties) {
        super(properties.randomTicks());
        this.registerDefaultState(this.stateDefinition.any().setValue(META, CD2));
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        if (pos.getY() < context.getLevel().getMaxBuildHeight() - 1 && context.getLevel().getBlockState(pos.above()).canBeReplaced(context)) {
            return this.defaultBlockState().setValue(META, CD2);
        }
        return null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (!isUpper(state)) {
            level.setBlock(pos.above(), state.setValue(META, state.getValue(META) + 8), 3);
        }
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        int meta = state.getValue(META);
        if (isUpper(meta)) {
            BlockState below = level.getBlockState(pos.below());
            return below.is(this) && below.getValue(META) == baseMeta(meta);
        }
        BlockState below = level.getBlockState(pos.below());
        return MustardWillowFlowerBlock.isLegacyPlantableSoil(below);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (!state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        int meta = state.getValue(META);
        if (!isUpper(meta) && direction == Direction.UP && (!neighborState.is(this) || neighborState.getValue(META) != meta + 8)) {
            BlockState flower = HbmBlocks.PLANT_FLOWER.get().defaultBlockState()
                    .setValue(MustardWillowFlowerBlock.META, meta == WEED ? MustardWillowFlowerBlock.WEED : MustardWillowFlowerBlock.CD0);
            return flower.canSurvive(level, pos) ? flower : Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (isUpper(state)) {
            return;
        }
        int meta = state.getValue(META);
        if (meta == WEED) {
            if (MustardWillowFlowerBlock.isDeadOrOily(level.getBlockState(pos.below()))) {
                level.setBlock(pos, HbmBlocks.PLANT_DEAD.get().defaultBlockState().setValue(DeadPlantBlock.KIND, DeadPlantBlock.Kind.BIG_FLOWER), 3);
                level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 3);
            }
            return;
        }
        if (this.isValidBonemealTarget(level, pos, state) && this.isBonemealSuccess(level, random, pos, state) && random.nextInt(3) == 0) {
            this.performBonemeal(level, random, pos, state);
        }
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        BlockPos lowerPos = lowerPos(pos, state);
        BlockState lower = level.getBlockState(lowerPos);
        if (!lower.is(this)) {
            return false;
        }
        int meta = baseMeta(lower.getValue(META));
        if (meta == CD2) {
            return MustardWillowFlowerBlock.hasAdjacentWater(level, lowerPos);
        }
        if (meta == CD3) {
            return MustardWillowFlowerBlock.hasAdjacentWater(level, lowerPos)
                    && MustardWillowFlowerBlock.isDeadOrOily(level.getBlockState(lowerPos.below()));
        }
        return false;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        int meta = baseMeta(state.getValue(META));
        return meta == CD3 || random.nextFloat() < 0.33F;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        BlockPos lowerPos = lowerPos(pos, state);
        BlockState lower = level.getBlockState(lowerPos);
        if (!lower.is(this)) {
            return;
        }
        int meta = baseMeta(lower.getValue(META));
        if (meta != CD2 && meta != CD3) {
            return;
        }
        int next = meta + 1;
        level.setBlock(lowerPos, lower.setValue(META, next), 3);
        level.setBlock(lowerPos.above(), lower.setValue(META, next + 8), 3);
        if (meta == CD3) {
            level.setBlock(lowerPos.below(), Blocks.DIRT.defaultBlockState(), 3);
        }
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && !isUpper(state)) {
            BlockPos upperPos = pos.above();
            BlockState upperState = level.getBlockState(upperPos);
            if (upperState.is(this) && upperState.getValue(META) == state.getValue(META) + 8) {
                if (!player.isCreative()) {
                    dropResources(upperState, level, upperPos, null, player, player.getMainHandItem());
                }
                level.setBlock(upperPos, Blocks.AIR.defaultBlockState(), 35);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = new ArrayList<>();
        drops.add(new ItemStack(HbmBlocks.PLANT_FLOWER.get()));
        if (state.getValue(META) == CD4 + 8) {
            ItemStack leaves = LegacyVariantItem.stackFor(HbmItems.PLANT_ITEM, "mustardwillow");
            leaves.setCount(3 + params.getLevel().random.nextInt(4));
            drops.add(leaves);
        }
        return drops;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(META);
    }

    public static boolean isUpper(BlockState state) {
        return isUpper(state.getValue(META));
    }

    private static boolean isUpper(int meta) {
        return meta >= 8;
    }

    private static int baseMeta(int meta) {
        return meta & 7;
    }

    private static BlockPos lowerPos(BlockPos pos, BlockState state) {
        return isUpper(state) ? pos.below() : pos;
    }
}
