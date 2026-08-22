package com.reinhardt.hbm.fluid;

import com.reinhardt.hbm.block.LegacyHazardLiquidBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

public final class LegacyHazardFlowingFluid {
    private LegacyHazardFlowingFluid() {
    }

    private static void afterTick(Level level, BlockPos pos) {
        if (level.getBlockState(pos).getBlock() instanceof LegacyHazardLiquidBlock block) {
            block.afterFluidTick(level, pos, level.random);
        }
    }

    public static final class Source extends BaseFlowingFluid.Source {
        public Source(Properties properties) {
            super(properties);
        }

        @Override
        public void tick(Level level, BlockPos pos, FluidState state) {
            super.tick(level, pos, state);
            afterTick(level, pos);
        }
    }

    public static final class Flowing extends BaseFlowingFluid.Flowing {
        public Flowing(Properties properties) {
            super(properties);
        }

        @Override
        public void tick(Level level, BlockPos pos, FluidState state) {
            super.tick(level, pos, state);
            afterTick(level, pos);
        }
    }
}
