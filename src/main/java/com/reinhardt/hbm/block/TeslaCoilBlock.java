package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.TeslaCoilBlockEntity;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Direct 1.7.10 MachineTesla port: a full-cube collision block with TESR geometry. */
public final class TeslaCoilBlock extends Block implements EntityBlock {
    public TeslaCoilBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TeslaCoilBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == HbmBlockEntities.TESLA_COIL.get()
                ? (tickerLevel, tickerPos, tickerState, blockEntity) -> TeslaCoilBlockEntity.tick(
                tickerLevel, tickerPos, tickerState, (TeslaCoilBlockEntity) blockEntity
        )
                : null;
    }
}
