package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.MachineInventory;
import com.reinhardt.hbm.blockentity.ZirnoxReactorBlockEntity;
import com.reinhardt.hbm.event.LegacyMobSpawnEvents;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

public class ZirnoxReactorBlock extends LargeMachineBlock implements EntityBlock {
    public static final Footprint FOOTPRINT = Footprint.centered(2, 5, 2);

    public ZirnoxReactorBlock(Properties properties, VoxelShape shape) {
        super(properties, FOOTPRINT, shape, RotationBasis.MODERN_NORTH);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return super.getStateForPlacement(context);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ZirnoxReactorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (blockEntityType != HbmBlockEntities.ZIRNOX_REACTOR.get()) {
            return null;
        }
        if (level.isClientSide) {
            return null;
        }
        return (tickerLevel, pos, tickerState, blockEntity) -> ZirnoxReactorBlockEntity.tick(
                tickerLevel,
                pos,
                tickerState,
                (ZirnoxReactorBlockEntity) blockEntity
        );
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!player.isCrouching() && player instanceof ServerPlayer serverPlayer) {
            LegacyMobSpawnEvents.markFbi(serverPlayer);
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
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof MachineInventory inventory) {
                inventory.dropContents(level, pos);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}

