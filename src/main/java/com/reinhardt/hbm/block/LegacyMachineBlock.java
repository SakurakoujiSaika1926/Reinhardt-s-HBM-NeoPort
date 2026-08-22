package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.LegacyMachineBlockEntity;
import com.reinhardt.hbm.blockentity.MachineInventory;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Shared structural block for legacy 1.7.10 machines whose complete geometry
 * is supplied by an OBJ model. The footprint is the old BlockDummyable volume,
 * not an approximation of the rendered mesh.
 */
public class LegacyMachineBlock extends LargeMachineBlock implements EntityBlock {
    public LegacyMachineBlock(BlockBehaviour.Properties properties, Footprint footprint, VoxelShape coreShape) {
        super(properties, footprint, coreShape, RotationBasis.HBM_LEGACY_SOUTH);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LegacyMachineBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != HbmBlockEntities.LEGACY_MACHINE.get()) {
            return null;
        }
        return (tickerLevel, tickerPos, tickerState, entity) -> {
            if (entity instanceof LegacyMachineBlockEntity machine) {
                LegacyMachineBlockEntity.tick(tickerLevel, tickerPos, tickerState, machine);
            }
        };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isCrouching()) {
            return InteractionResult.PASS;
        }
        if (!(level.getBlockEntity(pos) instanceof LegacyMachineBlockEntity machine)) {
            return InteractionResult.PASS;
        }
        if (machine.handleEmptyHandInteraction(player)) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!machine.hasMenu()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof MenuProvider menu && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(menu, buffer -> buffer.writeBlockPos(pos));
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, net.minecraft.core.Direction direction) {
        return level.getBlockEntity(pos) instanceof LegacyMachineBlockEntity machine
                && (machine.machineId().equals("machine_radar") || machine.machineId().equals("machine_radar_large"))
                ? machine.radarRedPower() : 0;
    }

    @Override
    protected int getDirectSignal(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, net.minecraft.core.Direction direction) {
        return getSignal(state, level, pos, direction);
    }

    @Override
    protected ItemInteractionResult useItemOn(
            net.minecraft.world.item.ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit
    ) {
        if (level.getBlockEntity(pos) instanceof LegacyMachineBlockEntity machine
                && machine.handleItemInteraction(player, stack)) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof MachineInventory inventory) {
            inventory.dropContents(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
