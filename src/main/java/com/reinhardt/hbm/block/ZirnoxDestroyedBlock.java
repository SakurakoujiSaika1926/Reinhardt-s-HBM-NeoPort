package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.ZirnoxDestroyedBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;

import javax.annotation.Nullable;

public class ZirnoxDestroyedBlock extends LargeMachineBlock implements EntityBlock {
    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        if (!this.canPlaceMachineFootprintAt(context.getLevel(), context.getClickedPos(), facing, context)) {
            return null;
        }
        return this.defaultBlockState().setValue(FACING, facing);
    }

    @Override
    protected BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.setValue(FACING, mirror.getRotation(state.getValue(FACING)).rotate(state.getValue(FACING)));
    }

    public static final Footprint FOOTPRINT = Footprint.centered(2, 2, 2);

    public ZirnoxDestroyedBlock(Properties properties, VoxelShape shape) {
        super(properties, FOOTPRINT, shape, ZirnoxDestroyedBlock::mapDestroyedZirnoxOffset);
    }

    public static BlockPos mapDestroyedZirnoxOffset(Direction facing, BlockPos offset) {
        return switch (facing) {
            case NORTH -> new BlockPos(offset.getX(), offset.getY(), offset.getZ());
            case EAST -> new BlockPos(-offset.getZ(), offset.getY(), offset.getX());
            case SOUTH -> new BlockPos(-offset.getX(), offset.getY(), -offset.getZ());
            case WEST -> new BlockPos(offset.getZ(), offset.getY(), -offset.getX());
            default -> throw new IllegalArgumentException("Destroyed ZIRNOX facing must be horizontal: " + facing);
        };
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        return new ZirnoxDestroyedBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> blockEntityType) {
        if (level.isClientSide || blockEntityType != HbmBlockEntities.ZIRNOX_DESTROYED.get()) {
            return null;
        }
        return (tickerLevel, pos, tickerState, blockEntity) ->
                ZirnoxDestroyedBlockEntity.tick(tickerLevel, pos, tickerState,
                        (ZirnoxDestroyedBlockEntity) blockEntity);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide && !oldState.is(state.getBlock())) {
            level.scheduleTick(pos, this, 2);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // In 1.7.10 all fifty multiblock segments were this block and ticked
        // independently every 100-119 ticks. Sampling one of them every two
        // ticks preserves the same aggregate rate without fifty ticking BEs.
        BlockPos sampled = pos.offset(FOOTPRINT.offsets().get(random.nextInt(FOOTPRINT.offsets().size())));
        BlockPos above = sampled.above();
        BlockState aboveState = level.getBlockState(above);
        if (aboveState.isAir()) {
            if (random.nextInt(10) == 0) {
                level.setBlock(above, HbmBlocks.GAS_MELTDOWN.get().defaultBlockState(), Block.UPDATE_ALL);
            }
        } else if ((aboveState.is(HbmBlocks.FOAM_LAYER.get()) || aboveState.is(HbmBlocks.BLOCK_FOAM.get()))
                && random.nextInt(25) == 0
                && level.getBlockEntity(pos) instanceof ZirnoxDestroyedBlockEntity destroyed) {
            destroyed.extinguish();
        }
        if (level.getBlockState(above).isAir() && random.nextInt(10) == 0) {
            level.setBlock(above, HbmBlocks.GAS_MELTDOWN.get().defaultBlockState(), Block.UPDATE_ALL);
        }
        level.scheduleTick(pos, this, 2);
    }
}

