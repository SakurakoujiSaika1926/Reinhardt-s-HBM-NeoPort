package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.AirCompressorBlockEntity;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class AirCompressorBlock extends LargeMachineBlock implements EntityBlock {
    public static final Footprint FOOTPRINT = Footprint.box(-1, 0, 0, 0, -1, 0);

    public AirCompressorBlock(Properties properties, VoxelShape shape) {
        super(properties, FOOTPRINT, shape, RotationBasis.HBM_LEGACY_SOUTH);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AirCompressorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == HbmBlockEntities.AIR_COMPRESSOR.get() ? AirCompressorBlockEntity::tick : null;
    }
}

