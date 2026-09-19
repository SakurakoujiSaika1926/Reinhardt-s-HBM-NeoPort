package com.reinhardt.hbm.fluid;

import com.reinhardt.hbm.block.CoriumBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

public final class CoriumFlowingFluid {
    private static final Direction[] HORIZONTAL_DIRECTIONS = {
            Direction.NORTH,
            Direction.SOUTH,
            Direction.WEST,
            Direction.EAST
    };
    private static final int LEGACY_DROP_OFF = 2;

    private CoriumFlowingFluid() {
    }

    private static void afterTick(Level level, BlockPos pos, FluidState state) {
        if (level.getBlockState(pos).getBlock() instanceof CoriumBlock) {
            legacyDisplacementTick(level, pos, state);
            CoriumBlock.afterFluidTick(level, pos, level.random);
        }
    }

    private static void legacyDisplacementTick(Level level, BlockPos pos, FluidState state) {
        if (level.isClientSide || !(state.getType() instanceof FlowingFluid flowing)) {
            return;
        }
        if (tryLegacyDisplace(level, pos.below(), flowing.getFlowing(8, true))) {
            return;
        }
        int amount = state.isSource() ? 8 : state.getAmount();
        int spreadAmount = amount - LEGACY_DROP_OFF;
        if (spreadAmount <= 0) {
            return;
        }
        FluidState spreadState = flowing.getFlowing(spreadAmount, false);
        int start = level.random.nextInt(HORIZONTAL_DIRECTIONS.length);
        for (int i = 0; i < HORIZONTAL_DIRECTIONS.length; i++) {
            Direction direction = HORIZONTAL_DIRECTIONS[(start + i) % HORIZONTAL_DIRECTIONS.length];
            if (tryLegacyDisplace(level, pos.relative(direction), spreadState)) {
                return;
            }
        }
    }

    private static boolean tryLegacyDisplace(Level level, BlockPos pos, FluidState spreadState) {
        BlockState target = level.getBlockState(pos);
        if (target.isAir() || !target.getFluidState().isEmpty()) {
            return false;
        }
        float resistance = target.getBlock().getExplosionResistance();
        float displacementResistance = (float) (Math.sqrt(resistance) * 3.0D);
        if (displacementResistance >= 1.0F
                && level.random.nextInt(Math.max(1, (int) displacementResistance)) != 0) {
            return false;
        }
        level.setBlock(pos, spreadState.createLegacyBlock(), 3);
        return true;
    }

    public static final class Source extends BaseFlowingFluid.Source {
        public Source(Properties properties) {
            super(properties);
        }

        @Override
        public void tick(Level level, BlockPos pos, FluidState state) {
            super.tick(level, pos, state);
            afterTick(level, pos, state);
        }
    }

    public static final class Flowing extends BaseFlowingFluid.Flowing {
        public Flowing(Properties properties) {
            super(properties);
        }

        @Override
        public void tick(Level level, BlockPos pos, FluidState state) {
            super.tick(level, pos, state);
            afterTick(level, pos, state);
        }
    }
}
