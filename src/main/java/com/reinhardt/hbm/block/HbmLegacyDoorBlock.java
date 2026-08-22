package com.reinhardt.hbm.block;

import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;

public class HbmLegacyDoorBlock extends DoorBlock {
    public HbmLegacyDoorBlock(Properties properties) {
        super(BlockSetType.IRON, properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        toggleOldDoor(state, level, pos, player);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        toggleOldDoor(state, level, pos, player);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void toggleOldDoor(BlockState state, Level level, BlockPos pos, Player player) {
        BlockPos lowerPos = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
        BlockState lowerState = level.getBlockState(lowerPos);
        if (!(lowerState.getBlock() instanceof HbmLegacyDoorBlock)) {
            return;
        }
        boolean open = lowerState.getValue(OPEN);
        level.setBlock(lowerPos, lowerState.setValue(OPEN, !open), 2);
        BlockPos upperPos = lowerPos.above();
        BlockState upperState = level.getBlockState(upperPos);
        if (upperState.getBlock() instanceof HbmLegacyDoorBlock) {
            level.sendBlockUpdated(upperPos, upperState, upperState, 2);
        }
        level.playSound(player, pos, (!open ? HbmSoundEvents.OPEN_DOOR : HbmSoundEvents.CLOSE_DOOR).get(), SoundSource.BLOCKS, 1.0F, level.random.nextFloat() * 0.1F + 0.9F);
    }

    public void setOpen(Player player, Level level, BlockState state, BlockPos pos, boolean open) {
        BlockPos lowerPos = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
        BlockState lowerState = level.getBlockState(lowerPos);
        if (!(lowerState.getBlock() instanceof HbmLegacyDoorBlock) || lowerState.getValue(OPEN) == open) {
            return;
        }
        level.setBlock(lowerPos, lowerState.setValue(OPEN, open), 2);
        level.playSound(player, lowerPos, (open ? HbmSoundEvents.OPEN_DOOR : HbmSoundEvents.CLOSE_DOOR).get(), SoundSource.BLOCKS, 1.0F, level.random.nextFloat() * 0.1F + 0.9F);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, net.minecraft.world.level.block.Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (level.isClientSide || state.getValue(HALF) != DoubleBlockHalf.LOWER || neighborBlock == this) {
            return;
        }
        boolean powered = level.hasNeighborSignal(pos) || level.hasNeighborSignal(pos.above());
        if (powered != state.getValue(OPEN)) {
            setOpen(null, level, state, pos, powered);
        }
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return false;
    }

    public static Direction facingFromOldMeta(int meta) {
        return switch (meta & 3) {
            case 0 -> Direction.NORTH;
            case 1 -> Direction.EAST;
            case 2 -> Direction.SOUTH;
            default -> Direction.WEST;
        };
    }
}
