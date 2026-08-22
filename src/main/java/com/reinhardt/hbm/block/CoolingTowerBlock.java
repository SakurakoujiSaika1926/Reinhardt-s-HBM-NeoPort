package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.CoolingTowerBlockEntity;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

public class CoolingTowerBlock extends LargeMachineBlock implements EntityBlock {
    private final Kind kind;

    public CoolingTowerBlock(Properties properties, Footprint footprint, VoxelShape shape, Kind kind) {
        super(properties, footprint, shape, RotationBasis.MODERN_NORTH);
        this.kind = kind;
    }

    public Kind kind() {
        return this.kind;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : state;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CoolingTowerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (blockEntityType != HbmBlockEntities.COOLING_TOWER.get()) {
            return null;
        }
        if (level.isClientSide) {
            return (tickerLevel, pos, tickerState, blockEntity) -> CoolingTowerBlockEntity.clientTick(
                    tickerLevel,
                    pos,
                    tickerState,
                    (CoolingTowerBlockEntity) blockEntity
            );
        }
        return (tickerLevel, pos, tickerState, blockEntity) -> CoolingTowerBlockEntity.tick(
                tickerLevel,
                pos,
                tickerState,
                (CoolingTowerBlockEntity) blockEntity
        );
    }

    public enum Kind {
        SMALL(1_000, 1_000, 2, 19, 2),
        LARGE(10_000, 10_000, 4, 13, 4);

        private final int inputCapacity;
        private final int outputCapacity;
        private final int radiusX;
        private final int height;
        private final int radiusZ;

        Kind(int inputCapacity, int outputCapacity, int radiusX, int height, int radiusZ) {
            this.inputCapacity = inputCapacity;
            this.outputCapacity = outputCapacity;
            this.radiusX = radiusX;
            this.height = height;
            this.radiusZ = radiusZ;
        }

        public int inputCapacity() {
            return this.inputCapacity;
        }

        public int outputCapacity() {
            return this.outputCapacity;
        }

        public int radiusX() {
            return this.radiusX;
        }

        public int height() {
            return this.height;
        }

        public int radiusZ() {
            return this.radiusZ;
        }
    }
}

