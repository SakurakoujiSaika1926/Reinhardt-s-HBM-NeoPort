package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.BlastDoorBlockEntity;
import com.reinhardt.hbm.blockentity.BlastDoorDummyBlockEntity;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class BlastDoorDummyBlock extends Block implements EntityBlock {
    private static final ThreadLocal<Boolean> SUPPRESS_CORE_DESTROY = ThreadLocal.withInitial(() -> false);

    public BlastDoorDummyBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlastDoorDummyBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (blockEntityType != HbmBlockEntities.BLAST_DOOR_DUMMY.get()) {
            return null;
        }
        return (tickerLevel, pos, tickerState, blockEntity) -> BlastDoorDummyBlockEntity.tick(
                tickerLevel,
                pos,
                tickerState,
                (BlastDoorDummyBlockEntity) blockEntity
        );
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof BlastDoorDummyBlockEntity dummy) {
            BlockPos corePos = dummy.corePos();
            if (!corePos.equals(pos)) {
                BlockState coreState = level.getBlockState(corePos);
                if (!coreState.isAir()) {
                    return coreState.getDestroyProgress(player, level, corePos);
                }
            }
        }
        return super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(HbmBlocks.BLAST_DOOR.get());
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof BlastDoorDummyBlockEntity dummy) {
            dummy.setDropCoreWhenRemoved(!player.isCreative());
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.getBlockEntity(pos) instanceof BlastDoorDummyBlockEntity dummy) {
            BlockPos corePos = dummy.corePos();
            BlockState coreState = level.getBlockState(corePos);
            if (coreState.getBlock() instanceof BlastDoorBlock coreBlock) {
                return coreBlock.useWithoutItem(coreState, level, corePos, player, hitResult.withPosition(corePos));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.getBlockEntity(pos) instanceof BlastDoorDummyBlockEntity dummy) {
            BlockPos corePos = dummy.corePos();
            BlockState coreState = level.getBlockState(corePos);
            if (coreState.getBlock() instanceof BlastDoorBlock coreBlock) {
                return coreBlock.useItemOn(stack, coreState, level, corePos, player, hand, hitResult.withPosition(corePos));
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!SUPPRESS_CORE_DESTROY.get()
                && !movedByPiston
                && !state.is(newState.getBlock())
                && !level.isClientSide
                && level.getBlockEntity(pos) instanceof BlastDoorDummyBlockEntity dummy) {
            BlockPos corePos = dummy.corePos();
            if (!corePos.equals(pos)
                    && level.getBlockState(corePos).getBlock() instanceof BlastDoorBlock) {
                level.destroyBlock(corePos, dummy.consumeDropCoreWhenRemoved());
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    public static void runWithoutCoreDestroy(Runnable action) {
        boolean previous = SUPPRESS_CORE_DESTROY.get();
        SUPPRESS_CORE_DESTROY.set(true);
        try {
            action.run();
        } finally {
            SUPPRESS_CORE_DESTROY.set(previous);
        }
    }
}
