package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.DeuteriumExtractorBlockEntity;
import com.reinhardt.hbm.power.PowerNetworkManager;
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

public class DeuteriumExtractorBlock extends Block implements EntityBlock {
    public DeuteriumExtractorBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DeuteriumExtractorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (blockEntityType != HbmBlockEntities.DEUTERIUM_EXTRACTOR.get()) {
            return null;
        }
        return (tickerLevel, pos, tickerState, blockEntity) -> DeuteriumExtractorBlockEntity.tick(
                tickerLevel,
                pos,
                tickerState,
                (DeuteriumExtractorBlockEntity) blockEntity
        );
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!state.is(oldState.getBlock())) {
            PowerNetworkManager.markDirty(level);
            if (!level.isClientSide) {
                level.invalidateCapabilities(pos);
            }
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        boolean removed = !state.is(newState.getBlock());
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (removed) {
            PowerNetworkManager.markDirty(level);
            if (!level.isClientSide) {
                level.invalidateCapabilities(pos);
            }
        }
    }
}
