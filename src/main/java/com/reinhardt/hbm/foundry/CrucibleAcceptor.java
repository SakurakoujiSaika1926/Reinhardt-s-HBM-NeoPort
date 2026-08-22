package com.reinhardt.hbm.foundry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

public interface CrucibleAcceptor {
    boolean canAcceptPartialPour(Level level, BlockPos pos, double x, double y, double z, Direction side, FoundryMaterialStack stack);

    FoundryMaterialStack pour(Level level, BlockPos pos, double x, double y, double z, Direction side, FoundryMaterialStack stack);

    default boolean canAcceptPartialFlow(Level level, BlockPos pos, Direction side, FoundryMaterialStack stack) {
        return false;
    }

    default FoundryMaterialStack flow(Level level, BlockPos pos, Direction side, FoundryMaterialStack stack) {
        return stack;
    }
}
