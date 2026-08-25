package com.reinhardt.hbm.block;

import com.reinhardt.hbm.ReinhardtsHBM;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.MushroomBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;

public class WasteEarthBlock extends Block {
    public enum Kind {
        WASTE,
        FROZEN,
        BURNING
    }

    private final Kind kind;

    public WasteEarthBlock(Properties properties) {
        this(properties, Kind.WASTE);
    }

    public WasteEarthBlock(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (kind == Kind.BURNING) {
            tickBurningEarth(state, level, pos, random);
            return;
        }
        if (kind != Kind.WASTE) {
            return;
        }
        BlockPos above = pos.above();
        BlockState aboveState = level.getBlockState(above);
        if (level.getRawBrightness(above, 0) < 4 && aboveState.getLightBlock(level, above) > 2) {
            level.setBlock(pos, Blocks.DIRT.defaultBlockState(), 3);
            return;
        }
        if (aboveState.getBlock() instanceof MushroomBlock) {
            Block mush = BuiltInRegistries.BLOCK.get(ReinhardtsHBM.id("mush"));
            if (mush != Blocks.AIR) {
                level.setBlock(above, mush.defaultBlockState(), 3);
            }
        }
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        super.stepOn(level, pos, state, entity);
        if (level.isClientSide || !(entity instanceof LivingEntity living)) {
            return;
        }
        if (kind == Kind.FROZEN) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 2 * 60 * 20, 2));
        } else if (kind == Kind.BURNING) {
            living.igniteForSeconds(5.0F);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        if (kind == Kind.BURNING) {
            double x = pos.getX() + random.nextDouble();
            double z = pos.getZ() + random.nextDouble();
            level.addParticle(ParticleTypes.FLAME, x, pos.getY() + 1.1D, z, 0.0D, 0.0D, 0.0D);
            level.addParticle(ParticleTypes.SMOKE, x, pos.getY() + 1.1D, z, 0.0D, 0.0D, 0.0D);
        }
    }

    @Override
    protected BlockState updateShape(BlockState state, net.minecraft.core.Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (kind == Kind.BURNING && direction == net.minecraft.core.Direction.UP
                && (neighborState.liquid() || neighborState.isCollisionShapeFullBlock(level, neighborPos))) {
            return Blocks.DIRT.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (kind == Kind.FROZEN) {
            return List.of(new ItemStack(Items.SNOWBALL));
        }
        return List.of(new ItemStack(Blocks.DIRT));
    }

    private void tickBurningEarth(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(5) == 0 && !level.isRainingAt(pos.above())) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockPos target = pos.offset(dx, dy, dz);
                        BlockPos targetAbove = target.above();
                        BlockState targetState = level.getBlockState(target);
                        BlockState aboveState = level.getBlockState(targetAbove);
                        if (!aboveState.isCollisionShapeFullBlock(level, targetAbove) && isBurnableEarth(targetState)) {
                            level.setBlock(target, state, Block.UPDATE_ALL);
                        }
                        if (targetState.getBlock() instanceof LeavesBlock || targetState.getBlock() instanceof BushBlock) {
                            level.removeBlock(target, false);
                        }
                        if (targetState.is(legacyBlock("frozen_dirt"))) {
                            level.setBlock(target, Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
                        }
                        if (aboveState.isFlammable(level, targetAbove, net.minecraft.core.Direction.UP)
                                && !(aboveState.getBlock() instanceof LeavesBlock || aboveState.getBlock() instanceof BushBlock)
                                && level.isEmptyBlock(pos.above())) {
                            level.setBlock(pos.above(), Blocks.FIRE.defaultBlockState(), Block.UPDATE_ALL);
                        }
                    }
                }
            }
        }

        Block impactDirt = legacyBlock("impact_dirt");
        level.setBlock(pos, impactDirt == Blocks.AIR ? Blocks.DIRT.defaultBlockState() : impactDirt.defaultBlockState(), Block.UPDATE_ALL);
    }

    private static boolean isBurnableEarth(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.MYCELIUM)
                || state.is(legacyBlock("waste_earth"))
                || state.is(legacyBlock("frozen_grass"))
                || state.is(legacyBlock("waste_mycelium"));
    }

    private static Block legacyBlock(String id) {
        Block block = BuiltInRegistries.BLOCK.get(ReinhardtsHBM.id(id));
        return block == null ? Blocks.AIR : block;
    }

}
