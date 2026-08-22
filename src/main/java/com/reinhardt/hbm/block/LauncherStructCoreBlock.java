package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.LauncherStructCoreBlockEntity;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class LauncherStructCoreBlock extends Block implements EntityBlock {
    private final boolean large;

    public LauncherStructCoreBlock(Properties properties, boolean large) {
        super(properties);
        this.large = large;
    }

    public boolean isLarge() {
        return this.large;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LauncherStructCoreBlockEntity(pos, state, this.large);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != HbmBlockEntities.LAUNCHER_STRUCT_CORE.get()) {
            return null;
        }
        return (tickerLevel, pos, tickerState, blockEntity) ->
                LauncherStructCoreBlockEntity.tick(tickerLevel, pos, tickerState, (LauncherStructCoreBlockEntity) blockEntity);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
