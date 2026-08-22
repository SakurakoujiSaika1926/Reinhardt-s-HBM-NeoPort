package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.MachineInventory;
import com.reinhardt.hbm.blockentity.WasteDrumBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

/**
 * The 1.7.10 spent-fuel pool drum is a non-directional block.  Keeping it
 * directionless also preserves its symmetric water-cooling behavior.
 */
public class WasteDrumBlock extends Block implements EntityBlock {
    public WasteDrumBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WasteDrumBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide) {
            return null;
        }
        return (tickerLevel, pos, tickerState, blockEntity) -> {
            if (blockEntity instanceof WasteDrumBlockEntity drum) {
                WasteDrumBlockEntity.tick(tickerLevel, pos, tickerState, drum);
            }
        };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof MenuProvider provider && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(provider, buffer -> buffer.writeBlockPos(pos));
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);

        // Exact 1.7.10 visual behavior: every water-adjacent face except the
        // underside emits one upward bubble during a client display tick.
        for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.values()) {
            if (direction == net.minecraft.core.Direction.DOWN || !level.getFluidState(pos.relative(direction)).is(FluidTags.WATER)) {
                continue;
            }

            double x = pos.getX() + 0.5D + direction.getStepX() + random.nextDouble() - 0.5D;
            double y = pos.getY() + 0.5D + direction.getStepY() + random.nextDouble() - 0.5D;
            double z = pos.getZ() + 0.5D + direction.getStepZ() + random.nextDouble() - 0.5D;
            if (direction.getStepX() != 0) {
                x = pos.getX() + 0.5D + direction.getStepX() * 0.5D + random.nextDouble() * 0.125D * direction.getStepX();
            }
            if (direction.getStepY() != 0) {
                y = pos.getY() + 0.5D + direction.getStepY() * 0.5D + random.nextDouble() * 0.125D * direction.getStepY();
            }
            if (direction.getStepZ() != 0) {
                z = pos.getZ() + 0.5D + direction.getStepZ() * 0.5D + random.nextDouble() * 0.125D * direction.getStepZ();
            }
            level.addParticle(ParticleTypes.BUBBLE, x, y, z, 0.0D, 0.2D, 0.0D);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof MachineInventory inventory) {
                inventory.dropContents(level, pos);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

}
