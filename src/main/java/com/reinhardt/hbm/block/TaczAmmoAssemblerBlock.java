package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.TaczAmmoAssemblerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

public class TaczAmmoAssemblerBlock extends AssemblyMachineBlock {
    public TaczAmmoAssemblerBlock(Properties properties, Footprint footprint, VoxelShape shape) {
        super(properties, footprint, shape);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TaczAmmoAssemblerBlockEntity(pos, state);
    }
}
