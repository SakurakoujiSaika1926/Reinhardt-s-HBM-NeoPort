package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.IcfAssembledLaserBlockEntity;
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

/** Invisible-to-recipes assembled replacement for ICF laser parts. It restores the original part when broken. */
public final class IcfAssembledLaserBlock extends LegacyVariantBlock implements EntityBlock {
    public IcfAssembledLaserBlock(Properties properties) {
        super(properties, 1);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new IcfAssembledLaserBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != HbmBlockEntities.ICF_ASSEMBLED_LASER.get()) {
            return null;
        }
        return (tickerLevel, tickerPos, tickerState, entity) ->
                IcfAssembledLaserBlockEntity.tick(tickerLevel, tickerPos, tickerState, (IcfAssembledLaserBlockEntity) entity);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean movedByPiston) {
        if (!state.is(replacement.getBlock()) && level.getBlockEntity(pos) instanceof IcfAssembledLaserBlockEntity assembled) {
            assembled.restoreOriginal();
            return;
        }
        super.onRemove(state, level, pos, replacement, movedByPiston);
    }
}
