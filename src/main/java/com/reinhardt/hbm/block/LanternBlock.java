package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.LanternBlockEntity;
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

/** 1.7.10 lantern core; the four upper blocks are managed by LargeMachineBlock. */
public class LanternBlock extends LargeMachineBlock implements EntityBlock {
    public LanternBlock(Properties properties) {
        super(properties,
                Footprint.legacySouthBox(4, 0, 0, 0, 0, 0),
                RotationBasis.HBM_LEGACY_SOUTH);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LanternBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (type != HbmBlockEntities.LANTERN.get()) {
            return null;
        }
        return (tickerLevel, tickerPos, tickerState, entity) ->
                LanternBlockEntity.tick(tickerLevel, tickerPos, tickerState, (LanternBlockEntity) entity);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }
}
