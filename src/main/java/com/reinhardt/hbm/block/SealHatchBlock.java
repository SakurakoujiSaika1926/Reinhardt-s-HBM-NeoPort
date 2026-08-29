package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.SealHatchBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import org.jetbrains.annotations.Nullable;

public final class SealHatchBlock extends Block implements EntityBlock {
    public SealHatchBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SealHatchBlockEntity(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        return !level.isClientSide && type == HbmBlockEntities.SEAL_HATCH.get()
                ? (BlockEntityTicker<T>) (BlockEntityTicker<SealHatchBlockEntity>) SealHatchBlockEntity::serverTick
                : null;
    }
}
