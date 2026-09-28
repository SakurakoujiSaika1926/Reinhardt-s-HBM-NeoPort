package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.IndustrialTurbineBlockEntity;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

public class IndustrialTurbineBlock extends LargeMachineBlock implements EntityBlock {
    @Override
    protected BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.setValue(FACING, mirror.getRotation(state.getValue(FACING)).rotate(state.getValue(FACING)));
    }

    public static final Footprint FOOTPRINT = Footprint.box(-1, 1, 0, 2, -3, 3);

    public IndustrialTurbineBlock(Properties properties, VoxelShape shape) {
        super(properties, FOOTPRINT, shape, IndustrialTurbineBlock::mapIndustrialTurbineOffset);
    }

    private static BlockPos mapIndustrialTurbineOffset(Direction facing, BlockPos offset) {
        return switch (facing) {
            case SOUTH -> new BlockPos(offset.getX(), offset.getY(), offset.getZ());
            case EAST -> new BlockPos(offset.getZ(), offset.getY(), -offset.getX());
            case NORTH -> new BlockPos(-offset.getX(), offset.getY(), -offset.getZ());
            case WEST -> new BlockPos(-offset.getZ(), offset.getY(), offset.getX());
            case UP, DOWN -> throw new IllegalArgumentException("Industrial turbine requires a horizontal facing: " + facing);
        };
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        net.minecraft.core.Direction facing = context.getHorizontalDirection().getOpposite();
        if (!this.canPlaceMachineFootprintAt(context.getLevel(), context.getClickedPos(), facing, context)) {
            return null;
        }
        return this.defaultBlockState().setValue(FACING, facing);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new IndustrialTurbineBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (blockEntityType != HbmBlockEntities.INDUSTRIAL_TURBINE.get()) {
            return null;
        }
        return (tickerLevel, pos, tickerState, blockEntity) -> IndustrialTurbineBlockEntity.tick(
                tickerLevel,
                pos,
                tickerState,
                (IndustrialTurbineBlockEntity) blockEntity
        );
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return useOnPart(level, pos, pos, player);
    }

    public static InteractionResult useOnPart(Level level, BlockPos clickedPos, BlockPos corePos, Player player) {
        if (player.isCrouching()) {
            return InteractionResult.PASS;
        }
        BlockEntity blockEntity = level.getBlockEntity(corePos);
        if (!(blockEntity instanceof IndustrialTurbineBlockEntity turbine)) {
            return InteractionResult.PASS;
        }

        BlockState state = level.getBlockState(corePos);
        Direction facing = state.hasProperty(FACING) ? state.getValue(FACING) : Direction.NORTH;
        BlockPos leverPos = corePos.relative(facing, 3).above();
        if (!clickedPos.equals(leverPos)) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide) {
            if (turbine.cycleCompression(player)) {
                level.playSound(null, clickedPos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 1.5F, 1.0F);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!state.is(oldState.getBlock())) {
            PowerNetworkManager.markDirty(level);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        boolean removed = !state.is(newState.getBlock());
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (removed) {
            PowerNetworkManager.markDirty(level);
        }
    }
}

