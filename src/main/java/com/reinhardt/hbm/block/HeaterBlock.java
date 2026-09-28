package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.HeaterBlockEntity;
import com.reinhardt.hbm.item.ScrewdriverItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.MenuProvider;

import javax.annotation.Nullable;

public class HeaterBlock extends LargeMachineBlock implements EntityBlock {
    private final HeaterBlockEntity.Kind kind;
    @Nullable
    private final Footprint cleanupFootprint;

    public HeaterBlock(Properties properties, Footprint footprint, VoxelShape shape, HeaterBlockEntity.Kind kind) {
        this(properties, footprint, null, shape, kind);
    }

    public HeaterBlock(Properties properties, Footprint footprint, @Nullable Footprint cleanupFootprint, VoxelShape shape, HeaterBlockEntity.Kind kind) {
        super(properties, footprint, shape, offsetMapper(kind));
        this.kind = kind;
        this.cleanupFootprint = cleanupFootprint;
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return switch (this.kind) {
            case FIREBOX -> fireboxStateForPlacement(context);
            case OVEN -> ovenStateForPlacement(context);
            case OILBURNER -> oilburnerStateForPlacement(context);
            case ELECTRIC -> electricHeaterStateForPlacement(context);
            case HEATEX -> heatExchangerStateForPlacement(context);
        };
    }

    private BlockState fireboxStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        if (!this.canPlaceMachineFootprintAt(context.getLevel(), context.getClickedPos(), facing, context)) {
            return null;
        }
        return this.defaultBlockState().setValue(FACING, facing);
    }

    private BlockState ovenStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        if (!this.canPlaceMachineFootprintAt(context.getLevel(), context.getClickedPos(), facing, context)) {
            return null;
        }
        return this.defaultBlockState().setValue(FACING, facing);
    }

    private BlockState oilburnerStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        if (!this.canPlaceMachineFootprintAt(context.getLevel(), context.getClickedPos(), facing, context)) {
            return null;
        }
        return this.defaultBlockState().setValue(FACING, facing);
    }

    private BlockState electricHeaterStateForPlacement(
            net.minecraft.world.item.context.BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        if (!this.canPlaceMachineFootprintAt(context.getLevel(), context.getClickedPos(), facing, context)) {
            return null;
        }
        return this.defaultBlockState().setValue(FACING, facing);
    }

    private BlockState heatExchangerStateForPlacement(
            net.minecraft.world.item.context.BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        if (!this.canPlaceMachineFootprintAt(context.getLevel(), context.getClickedPos(), facing, context)) {
            return null;
        }
        return this.defaultBlockState().setValue(FACING, facing);
    }

    @Override
    protected BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return switch (this.kind) {
            case FIREBOX -> rotateFirebox(state, rotation);
            case OVEN -> rotateOven(state, rotation);
            case OILBURNER -> rotateOilburner(state, rotation);
            case ELECTRIC -> rotateElectricHeater(state, rotation);
            case HEATEX -> rotateHeatExchanger(state, rotation);
        };
    }

    private static BlockState rotateFirebox(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateOven(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateOilburner(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateElectricHeater(
            BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateHeatExchanger(
            BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return switch (this.kind) {
            case FIREBOX -> mirrorFirebox(state, mirror);
            case OVEN -> mirrorOven(state, mirror);
            case OILBURNER -> mirrorOilburner(state, mirror);
            case ELECTRIC -> mirrorElectricHeater(state, mirror);
            case HEATEX -> mirrorHeatExchanger(state, mirror);
        };
    }

    private static BlockState mirrorFirebox(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.setValue(FACING, mirror.getRotation(state.getValue(FACING)).rotate(state.getValue(FACING)));
    }

    private static BlockState mirrorOven(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.setValue(FACING, mirror.getRotation(state.getValue(FACING)).rotate(state.getValue(FACING)));
    }

    private static BlockState mirrorOilburner(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.setValue(FACING, mirror.getRotation(state.getValue(FACING)).rotate(state.getValue(FACING)));
    }

    private static BlockState mirrorElectricHeater(
            BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.setValue(FACING, mirror.getRotation(state.getValue(FACING)).rotate(state.getValue(FACING)));
    }

    private static BlockState mirrorHeatExchanger(
            BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.setValue(FACING, mirror.getRotation(state.getValue(FACING)).rotate(state.getValue(FACING)));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HeaterBlockEntity(pos, state, this.kind);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide && this.cleanupFootprint != null) {
            removeDummies(level, pos, state.getValue(FACING), this.cleanupFootprint, offsetMapper(this.kind));
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    private static OffsetMapper offsetMapper(HeaterBlockEntity.Kind kind) {
        return switch (kind) {
            case FIREBOX -> HeaterBlock::mapFireboxOffset;
            case OVEN -> HeaterBlock::mapOvenOffset;
            case OILBURNER -> HeaterBlock::mapOilburnerOffset;
            case ELECTRIC -> HeaterBlock::mapElectricHeaterOffset;
            case HEATEX -> HeaterBlock::mapHeatExchangerOffset;
        };
    }

    private static BlockPos mapFireboxOffset(Direction facing, BlockPos offset) {
        return switch (facing) {
            case NORTH -> new BlockPos(offset.getX(), offset.getY(), offset.getZ());
            case EAST -> new BlockPos(-offset.getZ(), offset.getY(), offset.getX());
            case SOUTH -> new BlockPos(-offset.getX(), offset.getY(), -offset.getZ());
            case WEST -> new BlockPos(offset.getZ(), offset.getY(), -offset.getX());
            case UP, DOWN -> throw new IllegalArgumentException("Firebox requires a horizontal facing: " + facing);
        };
    }

    private static BlockPos mapOvenOffset(Direction facing, BlockPos offset) {
        return switch (facing) {
            case NORTH -> new BlockPos(offset.getX(), offset.getY(), offset.getZ());
            case EAST -> new BlockPos(-offset.getZ(), offset.getY(), offset.getX());
            case SOUTH -> new BlockPos(-offset.getX(), offset.getY(), -offset.getZ());
            case WEST -> new BlockPos(offset.getZ(), offset.getY(), -offset.getX());
            case UP, DOWN -> throw new IllegalArgumentException("Oven requires a horizontal facing: " + facing);
        };
    }

    private static BlockPos mapOilburnerOffset(Direction facing, BlockPos offset) {
        return switch (facing) {
            case NORTH -> new BlockPos(offset.getX(), offset.getY(), offset.getZ());
            case EAST -> new BlockPos(-offset.getZ(), offset.getY(), offset.getX());
            case SOUTH -> new BlockPos(-offset.getX(), offset.getY(), -offset.getZ());
            case WEST -> new BlockPos(offset.getZ(), offset.getY(), -offset.getX());
            case UP, DOWN -> throw new IllegalArgumentException("Oilburner requires a horizontal facing: " + facing);
        };
    }

    private static BlockPos mapElectricHeaterOffset(Direction facing, BlockPos offset) {
        return switch (facing) {
            case NORTH -> new BlockPos(offset.getX(), offset.getY(), offset.getZ());
            case EAST -> new BlockPos(-offset.getZ(), offset.getY(), offset.getX());
            case SOUTH -> new BlockPos(-offset.getX(), offset.getY(), -offset.getZ());
            case WEST -> new BlockPos(offset.getZ(), offset.getY(), -offset.getX());
            case UP, DOWN -> throw new IllegalArgumentException("Electric heater requires a horizontal facing: " + facing);
        };
    }

    private static BlockPos mapHeatExchangerOffset(Direction facing, BlockPos offset) {
        return switch (facing) {
            case NORTH -> new BlockPos(offset.getX(), offset.getY(), offset.getZ());
            case EAST -> new BlockPos(-offset.getZ(), offset.getY(), offset.getX());
            case SOUTH -> new BlockPos(-offset.getX(), offset.getY(), -offset.getZ());
            case WEST -> new BlockPos(offset.getZ(), offset.getY(), -offset.getX());
            case UP, DOWN -> throw new IllegalArgumentException("Heat exchanger requires a horizontal facing: " + facing);
        };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof HeaterBlockEntity heater) {
            if (heater.hasMenu() && player instanceof ServerPlayer serverPlayer && heater instanceof MenuProvider menuProvider) {
                serverPlayer.openMenu(menuProvider, buffer -> {
                    buffer.writeBlockPos(pos);
                    buffer.writeVarInt(heater.kind().ordinal());
                });
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        if (ScrewdriverItem.isScrewdriver(stack)) {
            return ScrewdriverItem.useItemOnHeater(stack, level, player, hand, pos);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return (tickerLevel, pos, tickerState, blockEntity) -> {
            if (blockEntity instanceof HeaterBlockEntity heater) {
                HeaterBlockEntity.tick(tickerLevel, pos, tickerState, heater);
            }
        };
    }
}

