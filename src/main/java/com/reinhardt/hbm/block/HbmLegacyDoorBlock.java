package com.reinhardt.hbm.block;

import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

public class HbmLegacyDoorBlock extends DoorBlock {
    public HbmLegacyDoorBlock(Properties properties) {
        super(BlockSetType.IRON, properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            toggleOldDoor(state, level, pos, player);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            toggleOldDoor(state, level, pos, player);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void toggleOldDoor(BlockState state, Level level, BlockPos pos, Player player) {
        DoorPair pair = findPair(state, level, pos);
        if (pair == null) {
            return;
        }
        boolean open = pair.lowerState().getValue(OPEN);
        setPair(level, pair, !open, pair.lowerState().getValue(POWERED));
        level.playSound(player, pos, (!open ? HbmSoundEvents.OPEN_DOOR : HbmSoundEvents.CLOSE_DOOR).get(), SoundSource.BLOCKS, 1.0F, level.random.nextFloat() * 0.1F + 0.9F);
    }

    @Override
    public void setOpen(@Nullable Entity entity, Level level, BlockState state, BlockPos pos, boolean open) {
        DoorPair pair = findPair(state, level, pos);
        if (pair == null || pair.lowerState().getValue(OPEN) == open) {
            return;
        }
        setPair(level, pair, open, pair.lowerState().getValue(POWERED));
        level.playSound(entity, pair.lowerPos(), (open ? HbmSoundEvents.OPEN_DOOR : HbmSoundEvents.CLOSE_DOOR).get(), SoundSource.BLOCKS, 1.0F, level.random.nextFloat() * 0.1F + 0.9F);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, net.minecraft.world.level.block.Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        if (level.isClientSide || state.getValue(HALF) != DoubleBlockHalf.LOWER || neighborBlock == this) {
            return;
        }
        DoorPair pair = findPair(state, level, pos);
        if (pair == null) {
            return;
        }
        boolean powered = level.hasNeighborSignal(pair.lowerPos()) || level.hasNeighborSignal(pair.upperPos());
        if ((powered || level.getBlockState(neighborPos).isSignalSource())
                && powered != pair.lowerState().getValue(OPEN)) {
            setPair(level, pair, powered, powered);
            level.playSound(null, pair.lowerPos(), (powered ? HbmSoundEvents.OPEN_DOOR : HbmSoundEvents.CLOSE_DOOR).get(), SoundSource.BLOCKS, 1.0F, level.random.nextFloat() * 0.1F + 0.9F);
        }
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return false;
    }

    public static Direction facingFromOldMeta(int meta) {
        return switch (meta & 3) {
            case 0 -> Direction.EAST;
            case 1 -> Direction.SOUTH;
            case 2 -> Direction.WEST;
            default -> Direction.NORTH;
        };
    }

    private static void setPair(Level level, DoorPair pair, boolean open, boolean powered) {
        BlockState lower = pair.lowerState()
                .setValue(HALF, DoubleBlockHalf.LOWER)
                .setValue(OPEN, open)
                .setValue(POWERED, powered);
        BlockState upper = pair.upperState()
                .setValue(HALF, DoubleBlockHalf.UPPER)
                .setValue(FACING, lower.getValue(FACING))
                .setValue(HINGE, lower.getValue(HINGE))
                .setValue(OPEN, open)
                .setValue(POWERED, powered);
        level.setBlock(pair.lowerPos(), lower, 2);
        level.setBlock(pair.upperPos(), upper, 2);
    }

    @Nullable
    private static DoorPair findPair(BlockState state, Level level, BlockPos pos) {
        BlockPos lowerPos = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
        BlockPos upperPos = lowerPos.above();
        BlockState lower = level.getBlockState(lowerPos);
        BlockState upper = level.getBlockState(upperPos);
        if (lower.getBlock() != state.getBlock()
                || upper.getBlock() != state.getBlock()
                || lower.getValue(HALF) != DoubleBlockHalf.LOWER
                || upper.getValue(HALF) != DoubleBlockHalf.UPPER) {
            return null;
        }
        lower = lower.setValue(HINGE, upper.getValue(HINGE));
        return new DoorPair(lowerPos, upperPos, lower, upper);
    }

    private record DoorPair(BlockPos lowerPos, BlockPos upperPos, BlockState lowerState, BlockState upperState) {
    }
}
