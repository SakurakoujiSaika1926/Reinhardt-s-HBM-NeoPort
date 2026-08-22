package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.SteamCondenserBlockEntity;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public class SteamCondenserBlock extends Block implements EntityBlock {
    public SteamCondenserBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SteamCondenserBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide || blockEntityType != HbmBlockEntities.STEAM_CONDENSER.get()) {
            return null;
        }
        return (tickerLevel, pos, tickerState, blockEntity) -> SteamCondenserBlockEntity.tick(
                tickerLevel,
                pos,
                tickerState,
                (SteamCondenserBlockEntity) blockEntity
        );
    }
}
