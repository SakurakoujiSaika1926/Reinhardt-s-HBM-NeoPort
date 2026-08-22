package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.EnergyConverterBlockEntity;
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

public final class EnergyConverterBlock extends Block implements EntityBlock {
    public enum Kind { HE_TO_FE, FE_TO_HE }

    private final Kind kind;

    public EnergyConverterBlock(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyConverterBlockEntity(pos, state, kind);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == HbmBlockEntities.ENERGY_CONVERTER.get()
                ? (tickerLevel, pos, tickerState, blockEntity) -> EnergyConverterBlockEntity.tick(
                tickerLevel, pos, tickerState, (EnergyConverterBlockEntity) blockEntity)
                : null;
    }
}
