package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.ChimneyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import org.jetbrains.annotations.Nullable;

public class ChimneyBlock extends LargeMachineBlock implements EntityBlock {
    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        if (this.industrial) {
            return industrialChimneyStateForPlacement(context);
        } else {
            return brickChimneyStateForPlacement(context);
        }
    }

    private BlockState brickChimneyStateForPlacement(
            net.minecraft.world.item.context.BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        if (!this.canPlaceMachineFootprintAt(context.getLevel(), context.getClickedPos(), facing, context)) {
            return null;
        }
        return this.defaultBlockState().setValue(FACING, facing);
    }

    private BlockState industrialChimneyStateForPlacement(
            net.minecraft.world.item.context.BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        if (!this.canPlaceMachineFootprintAt(context.getLevel(), context.getClickedPos(), facing, context)) {
            return null;
        }
        return this.defaultBlockState().setValue(FACING, facing);
    }

    @Override
    protected BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        if (this.industrial) {
            return rotateIndustrialChimney(state, rotation);
        } else {
            return rotateBrickChimney(state, rotation);
        }
    }

    private static BlockState rotateBrickChimney(
            BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateIndustrialChimney(
            BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        if (this.industrial) {
            return mirrorIndustrialChimney(state, mirror);
        } else {
            return mirrorBrickChimney(state, mirror);
        }
    }

    private static BlockState mirrorBrickChimney(
            BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.setValue(FACING, mirror.getRotation(state.getValue(FACING)).rotate(state.getValue(FACING)));
    }

    private static BlockState mirrorIndustrialChimney(
            BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.setValue(FACING, mirror.getRotation(state.getValue(FACING)).rotate(state.getValue(FACING)));
    }

    private final boolean industrial;

    public ChimneyBlock(Properties properties, boolean industrial) {
        super(properties,
                industrial ? industrialFootprint() : brickFootprint(),
                Shapes.block(),
                industrial ? ChimneyBlock::mapIndustrialChimneyOffset : ChimneyBlock::mapBrickChimneyOffset);
        this.industrial = industrial;
    }

    private static BlockPos mapBrickChimneyOffset(Direction facing, BlockPos offset) {
        return switch (facing) {
            case NORTH -> new BlockPos(offset.getX(), offset.getY(), offset.getZ());
            case EAST -> new BlockPos(-offset.getZ(), offset.getY(), offset.getX());
            case SOUTH -> new BlockPos(-offset.getX(), offset.getY(), -offset.getZ());
            case WEST -> new BlockPos(offset.getZ(), offset.getY(), -offset.getX());
            case UP, DOWN -> throw new IllegalArgumentException("Brick chimney requires a horizontal facing: " + facing);
        };
    }

    private static BlockPos mapIndustrialChimneyOffset(Direction facing, BlockPos offset) {
        return switch (facing) {
            case NORTH -> new BlockPos(offset.getX(), offset.getY(), offset.getZ());
            case EAST -> new BlockPos(-offset.getZ(), offset.getY(), offset.getX());
            case SOUTH -> new BlockPos(-offset.getX(), offset.getY(), -offset.getZ());
            case WEST -> new BlockPos(offset.getZ(), offset.getY(), -offset.getX());
            case UP, DOWN -> throw new IllegalArgumentException("Industrial chimney requires a horizontal facing: " + facing);
        };
    }

    private static Footprint brickFootprint() {
        return Footprint.centered(1, 13, 1);
    }

    private static Footprint industrialFootprint() {
        return Footprint.centered(1, 23, 1);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChimneyBlockEntity(pos, state, industrial);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return (tickerLevel, pos, tickerState, blockEntity) -> {
            if (blockEntity instanceof ChimneyBlockEntity chimney) {
                ChimneyBlockEntity.tick(tickerLevel, pos, tickerState, chimney);
            }
        };
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }
}

