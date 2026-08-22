package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.DeuteriumExtractorBlockEntity;
import com.reinhardt.hbm.blockentity.DeuteriumTowerBlockEntity;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class DeuteriumTowerBlock extends LargeMachineBlock implements EntityBlock {
    public static final Footprint FOOTPRINT = Footprint.legacySouthBox(9, 0, 1, 0, 0, 1);

    public DeuteriumTowerBlock(Properties properties, VoxelShape shape) {
        super(properties, FOOTPRINT, shape, RotationBasis.HBM_LEGACY_SOUTH);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DeuteriumTowerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (blockEntityType != HbmBlockEntities.DEUTERIUM_TOWER.get()) {
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
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        refreshPorts(level, pos, state);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!state.is(oldState.getBlock())) {
            PowerNetworkManager.markDirty(level);
            refreshPorts(level, pos, state);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        boolean removed = !state.is(newState.getBlock());
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (removed) {
            PowerNetworkManager.markDirty(level);
            refreshPorts(level, pos, state);
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return super.getStateForPlacement(context);
    }

    private static void refreshPorts(Level level, BlockPos corePos, BlockState state) {
        if (level.isClientSide) {
            return;
        }
        for (DeuteriumExtractorBlockEntity.Port port : DeuteriumExtractorBlockEntity.towerPorts(corePos, state)) {
            level.invalidateCapabilities(port.pos());
            level.invalidateCapabilities(port.connectorPos());
            EnergyCableBlock.refreshConnections(level, port.connectorPos());
            CatalyticCrackerBlock.refreshDuctsAtPort(level, port.pos(), port.connectorPos());
        }
    }
}
