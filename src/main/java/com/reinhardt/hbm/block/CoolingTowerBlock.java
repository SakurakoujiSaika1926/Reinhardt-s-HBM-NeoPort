package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.CoolingTowerBlockEntity;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

public class CoolingTowerBlock extends LargeMachineBlock implements EntityBlock {
    private final Kind kind;

    public CoolingTowerBlock(Properties properties, Footprint footprint, VoxelShape shape, Kind kind) {
        super(properties, footprint, shape, kind == Kind.SMALL
                ? CoolingTowerBlock::mapSmallTowerOffset
                : CoolingTowerBlock::mapLargeTowerOffset);
        this.kind = kind;
    }

    private static BlockPos mapSmallTowerOffset(Direction facing, BlockPos offset) {
        return switch (facing) {
            case NORTH -> new BlockPos(offset.getX(), offset.getY(), offset.getZ());
            case EAST -> new BlockPos(-offset.getZ(), offset.getY(), offset.getX());
            case SOUTH -> new BlockPos(-offset.getX(), offset.getY(), -offset.getZ());
            case WEST -> new BlockPos(offset.getZ(), offset.getY(), -offset.getX());
            default -> throw new IllegalArgumentException("Small cooling tower facing must be horizontal: " + facing);
        };
    }

    private static BlockPos mapLargeTowerOffset(Direction facing, BlockPos offset) {
        return switch (facing) {
            case NORTH -> new BlockPos(offset.getX(), offset.getY(), offset.getZ());
            case EAST -> new BlockPos(-offset.getZ(), offset.getY(), offset.getX());
            case SOUTH -> new BlockPos(-offset.getX(), offset.getY(), -offset.getZ());
            case WEST -> new BlockPos(offset.getZ(), offset.getY(), -offset.getX());
            default -> throw new IllegalArgumentException("Large cooling tower facing must be horizontal: " + facing);
        };
    }

    public Kind kind() {
        return this.kind;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return switch (this.kind) {
            case SMALL -> smallTowerStateForPlacement(context);
            case LARGE -> largeTowerStateForPlacement(context);
        };
    }

    private BlockState smallTowerStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        if (!this.canPlaceMachineFootprintAt(context.getLevel(), context.getClickedPos(), facing, context)) {
            return null;
        }
        return this.defaultBlockState().setValue(FACING, facing);
    }

    private BlockState largeTowerStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        if (!this.canPlaceMachineFootprintAt(context.getLevel(), context.getClickedPos(), facing, context)) {
            return null;
        }
        return this.defaultBlockState().setValue(FACING, facing);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return switch (this.kind) {
            case SMALL -> rotateSmallTower(state, rotation);
            case LARGE -> rotateLargeTower(state, rotation);
        };
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return switch (this.kind) {
            case SMALL -> mirrorSmallTower(state, mirror);
            case LARGE -> mirrorLargeTower(state, mirror);
        };
    }

    private static BlockState rotateSmallTower(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateLargeTower(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState mirrorSmallTower(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.getRotation(state.getValue(FACING)).rotate(state.getValue(FACING)));
    }

    private static BlockState mirrorLargeTower(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.getRotation(state.getValue(FACING)).rotate(state.getValue(FACING)));
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

