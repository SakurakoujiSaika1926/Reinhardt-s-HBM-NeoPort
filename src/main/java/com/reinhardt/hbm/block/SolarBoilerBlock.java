package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.SolarBoilerBlockEntity;
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

public class SolarBoilerBlock extends LargeMachineBlock implements EntityBlock {
    public static final Footprint FOOTPRINT = Footprint.centered(1, 3, 1);

    public SolarBoilerBlock(Properties properties, VoxelShape shape) {
        super(properties, FOOTPRINT, shape, RotationBasis.MODERN_NORTH);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SolarBoilerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (blockEntityType != HbmBlockEntities.SOLAR_BOILER.get()) {
            return null;
        }
        return (tickerLevel, pos, tickerState, blockEntity) -> SolarBoilerBlockEntity.tick(
                tickerLevel,
                pos,
                tickerState,
                (SolarBoilerBlockEntity) blockEntity
        );
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!state.is(oldState.getBlock())) {
            refreshPorts(level, pos);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        boolean removed = !state.is(newState.getBlock());
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (removed) {
            refreshPorts(level, pos);
        }
    }

    private static void refreshPorts(Level level, BlockPos corePos) {
        if (level.isClientSide) {
            return;
        }
        for (SolarBoilerBlockEntity.Port port : SolarBoilerBlockEntity.portsFor(corePos)) {
            CatalyticCrackerBlock.refreshDuctsAround(level, port.connectorPos());
        }
    }
}

