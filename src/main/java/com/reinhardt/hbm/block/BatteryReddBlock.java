package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.BatteryReddBlockEntity;
import com.reinhardt.hbm.blockentity.MachineInventory;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
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
import org.jetbrains.annotations.Nullable;

public class BatteryReddBlock extends LargeMachineBlock implements EntityBlock {
    @Override
    protected BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.setValue(FACING, mirror.getRotation(state.getValue(FACING)).rotate(state.getValue(FACING)));
    }

    // Latest 1.7.10 MachineBatteryREDD dimensions are
    // {up=9, down=0, north=2, south=2, west=4, east=4}.  The renderer's
    // unrotated model faces west, so NORTH/SOUTH must occupy 9 blocks on X
    // and 5 blocks on Z; swapping these axes turns the collision footprint
    // 90 degrees away from the rendered FEnSU.
    public static final Footprint FOOTPRINT = Footprint.centered(4, 10, 2);

    public BatteryReddBlock(Properties properties, VoxelShape shape) {
        super(properties, FOOTPRINT, shape, BatteryReddBlock::mapBatteryReddOffset);
    }

    private static BlockPos mapBatteryReddOffset(Direction facing, BlockPos offset) {
        return switch (facing) {
            case NORTH -> new BlockPos(offset.getX(), offset.getY(), offset.getZ());
            case EAST -> new BlockPos(-offset.getZ(), offset.getY(), offset.getX());
            case SOUTH -> new BlockPos(-offset.getX(), offset.getY(), -offset.getZ());
            case WEST -> new BlockPos(offset.getZ(), offset.getY(), -offset.getX());
            case UP, DOWN -> throw new IllegalArgumentException("REDD battery requires a horizontal facing: " + facing);
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
        return new BatteryReddBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (blockEntityType != HbmBlockEntities.BATTERY_REDD.get()) {
            return null;
        }
        return (tickerLevel, pos, tickerState, blockEntity) ->
                BatteryReddBlockEntity.tick(tickerLevel, pos, tickerState, (BatteryReddBlockEntity) blockEntity);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player.isCrouching()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof MenuProvider menuProvider && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(menuProvider, buffer -> buffer.writeBlockPos(pos));
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        boolean removed = !state.is(newState.getBlock());
        if (removed && level.getBlockEntity(pos) instanceof MachineInventory inventory) {
            inventory.dropContents(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}

