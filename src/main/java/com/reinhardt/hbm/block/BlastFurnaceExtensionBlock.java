package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.BlastFurnaceBlockEntity;
import com.reinhardt.hbm.blockentity.MachineDummyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

public class BlastFurnaceExtensionBlock extends Block implements EntityBlock {
    public BlastFurnaceExtensionBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        MachineDummyBlockEntity dummy = new MachineDummyBlockEntity(pos, state);
        dummy.setCorePos(pos.below());
        return dummy;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide) {
            configureProxy(level, pos);
            BlastFurnaceBlock.refreshExtensionState(level, pos.below());
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (!level.isClientSide && neighborPos.equals(pos.below())) {
            configureProxy(level, pos);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player.isCrouching()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockPos corePos = pos.below();
        if (level.getBlockEntity(corePos) instanceof BlastFurnaceBlockEntity furnace && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(furnace, buffer -> buffer.writeBlockPos(corePos));
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            BlastFurnaceBlock.refreshExtensionState(level, pos.below());
        }
    }

    private static void configureProxy(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof MachineDummyBlockEntity dummy) {
            dummy.setCorePos(pos.below());
        }
    }
}
