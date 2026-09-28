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
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

public class HbmLegacyDoorBlock extends DoorBlock {
    public HbmLegacyDoorBlock(Properties properties) {
        super(BlockSetType.IRON, properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        toggleDoor(state, level, pos, player);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        toggleDoor(state, level, pos, player);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void toggleDoor(BlockState state, Level level, BlockPos pos, Player player) {
        BlockState toggled = state.cycle(OPEN);
        // Match vanilla DoorBlock: update the clicked half immediately on both
        // logical sides and let DoorBlock.updateShape propagate the complete
        // state to the other half. This keeps both halves in one local update
        // chain instead of waiting for two independent server packets.
        level.setBlock(pos, toggled, 10);
        playDoorSound(player, level, pos, toggled.getValue(OPEN));
        level.gameEvent(player, toggled.getValue(OPEN) ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
    }

    @Override
    public void setOpen(@Nullable Entity entity, Level level, BlockState state, BlockPos pos, boolean open) {
        if (state.is(this) && state.getValue(OPEN) != open) {
            level.setBlock(pos, state.setValue(OPEN, open), 10);
            playDoorSound(entity, level, pos, open);
            level.gameEvent(entity, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, net.minecraft.world.level.block.Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        boolean powered = level.hasNeighborSignal(pos)
                || level.hasNeighborSignal(pos.relative(
                state.getValue(HALF) == DoubleBlockHalf.LOWER ? Direction.UP : Direction.DOWN
        ));
        if (!this.defaultBlockState().is(neighborBlock) && powered != state.getValue(POWERED)) {
            if (powered != state.getValue(OPEN)) {
                playDoorSound(null, level, pos, powered);
                level.gameEvent(null, powered ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
            }
            // DoorBlock.updateShape synchronizes the opposite half as part of
            // this update, exactly like the vanilla 1.21.1 redstone path.
            level.setBlock(pos, state.setValue(POWERED, powered).setValue(OPEN, powered), 2);
        }
    }

    private static void playDoorSound(@Nullable Entity source, Level level, BlockPos pos, boolean open) {
        level.playSound(
                source,
                pos,
                (open ? HbmSoundEvents.OPEN_DOOR : HbmSoundEvents.CLOSE_DOOR).get(),
                SoundSource.BLOCKS,
                1.0F,
                level.random.nextFloat() * 0.1F + 0.9F
        );
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
}
