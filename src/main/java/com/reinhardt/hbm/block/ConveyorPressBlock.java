package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.ConveyorPressBlockEntity;
import com.reinhardt.hbm.blockentity.MachineInventory;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import org.jetbrains.annotations.Nullable;

/**
 * Direct 1.7.10 MachineConveyorPress layout: one core with two vertical
 * dummy blocks. The OBJ extends through all three blocks; its collision
 * volume is therefore the old BlockDummyable 1 x 1 x 3 footprint.
 */
public final class ConveyorPressBlock extends LargeMachineBlock implements EntityBlock {
    public static final Footprint FOOTPRINT = Footprint.fromOffsets(
            BlockPos.ZERO,
            new BlockPos(0, 1, 0),
            new BlockPos(0, 2, 0)
    );

    public ConveyorPressBlock(Properties properties) {
        super(properties, FOOTPRINT, Shapes.block(), RotationBasis.HBM_LEGACY_SOUTH);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ConveyorPressBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> blockEntityType
    ) {
        if (blockEntityType != HbmBlockEntities.CONVEYOR_PRESS.get()) {
            return null;
        }
        return (tickerLevel, tickerPos, tickerState, blockEntity) -> ConveyorPressBlockEntity.tick(
                tickerLevel,
                tickerPos,
                tickerState,
                (ConveyorPressBlockEntity) blockEntity
        );
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        // The old block has neither a GUI nor an empty-hand action.
        return InteractionResult.PASS;
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
        if (!(level.getBlockEntity(pos) instanceof ConveyorPressBlockEntity press)
                || !press.canHandleItem(stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (!level.isClientSide && press.handleItemInteraction(player, stack)) {
            if (press.lastInteractionInstalledStamp()) {
                level.playSound(null, pos, HbmSoundEvents.UPGRADE_PLUG.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
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
        if (!state.is(newState.getBlock())) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof MachineInventory inventory) {
                inventory.dropContents(level, pos);
            }
            PowerNetworkManager.markDirty(level);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
