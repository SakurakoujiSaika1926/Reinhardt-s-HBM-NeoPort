package com.reinhardt.hbm.block;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;

import java.util.function.Supplier;

public final class VolcanicLavaBlock extends LiquidBlock {
    public VolcanicLavaBlock(Supplier<? extends FlowingFluid> fluid, Properties properties) {
        super(fluid.get(), properties);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean moving) {
        super.neighborChanged(state, level, pos, block, fromPos, moving);
        reactWithNeighbors(level, pos);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
        super.onPlace(state, level, pos, oldState, moving);
        reactWithNeighbors(level, pos);
    }

    private static void reactWithNeighbors(Level level, BlockPos pos) {
        if (level.isClientSide) {
            return;
        }
        for (Direction direction : Direction.values()) {
            BlockPos target = pos.relative(direction);
            BlockState replacement = reaction(level, target);
            if (replacement != null) {
                level.setBlock(target, replacement, Block.UPDATE_ALL);
            }
        }
    }

    private static BlockState reaction(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getFluidState().is(FluidTags.WATER)) {
            return Blocks.STONE.defaultBlockState();
        }
        if (state.is(BlockTags.LOGS)) {
            return HbmBlocks.WASTE_LOG.get().defaultBlockState();
        }
        if (state.is(BlockTags.PLANKS)) {
            return HbmBlocks.WASTE_PLANKS.get().defaultBlockState();
        }
        if (state.is(BlockTags.LEAVES)) {
            return Blocks.FIRE.defaultBlockState();
        }
        if (state.is(Blocks.DIAMOND_ORE)) {
            return HbmBlocks.ORE_BASALT.get().defaultBlockState().setValue(LegacyVariantBlock.VARIANT, 3);
        }
        return null;
    }

    public static void afterFluidTick(Level level, BlockPos pos, RandomSource random) {
        if (level.isClientSide || !(level.getBlockState(pos).getBlock() instanceof VolcanicLavaBlock)) {
            return;
        }
        int lavaCount = 0;
        int basaltCount = 0;
        Block basalt = BuiltInRegistries.BLOCK.get(ReinhardtsHBM.id("basalt"));
        for (Direction direction : Direction.values()) {
            BlockState neighbor = level.getBlockState(pos.relative(direction));
            if (neighbor.getBlock() instanceof VolcanicLavaBlock) {
                lavaCount++;
            }
            if (neighbor.is(basalt)) {
                basaltCount++;
            }
        }
        boolean source = level.getFluidState(pos).isSource();
        if (((!source && lavaCount < 2) || random.nextInt(5) == 0 && lavaCount < 5)
                && !(level.getBlockState(pos.below()).getBlock() instanceof VolcanicLavaBlock)) {
            solidify(level, pos, lavaCount, basaltCount, basalt, random);
        }
    }

    private static void solidify(Level level, BlockPos pos, int lavaCount, int basaltCount, Block basalt, RandomSource random) {
        int roll = random.nextInt(200);
        BlockState above = level.getBlockState(pos.above(10));
        boolean canMakeGem = lavaCount + basaltCount == 6
                && lavaCount < 3
                && (above.is(basalt) || above.getBlock() instanceof VolcanicLavaBlock);
        BlockState result;
        if (roll < 2) {
            result = oreState(0);
        } else if (roll == 2) {
            result = oreState(1);
        } else if (roll == 3) {
            result = oreState(2);
        } else if (roll == 4) {
            result = oreState(4);
        } else if (roll < 15 && canMakeGem) {
            result = oreState(3);
        } else {
            result = basalt.defaultBlockState();
        }
        level.setBlock(pos, result, Block.UPDATE_ALL);
    }

    private static BlockState oreState(int variant) {
        return HbmBlocks.ORE_BASALT.get().defaultBlockState().setValue(LegacyVariantBlock.VARIANT, variant);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (level.getBlockState(pos.above()).isAir() && !level.getBlockState(pos.above()).isSolidRender(level, pos.above())) {
            if (random.nextInt(100) == 0) {
                double x = pos.getX() + random.nextFloat();
                double y = pos.getY() + state.getFluidState().getHeight(level, pos);
                double z = pos.getZ() + random.nextFloat();
                level.addParticle(ParticleTypes.LAVA, x, y, z, 0.0D, 0.0D, 0.0D);
                level.playLocalSound(x, y, z, SoundEvents.LAVA_POP, SoundSource.BLOCKS,
                        0.2F + random.nextFloat() * 0.2F, 0.9F + random.nextFloat() * 0.15F, false);
            }
            if (random.nextInt(200) == 0) {
                level.playLocalSound(pos, SoundEvents.LAVA_AMBIENT, SoundSource.BLOCKS,
                        0.2F + random.nextFloat() * 0.2F, 0.9F + random.nextFloat() * 0.15F, false);
            }
        }
        if (random.nextInt(10) == 0
                && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
                && !level.getBlockState(pos.below(2)).isSolid()) {
            level.addParticle(ParticleTypes.DRIPPING_LAVA,
                    pos.getX() + random.nextFloat(), pos.getY() - 1.05D, pos.getZ() + random.nextFloat(),
                    0.0D, 0.0D, 0.0D);
        }
    }
}
