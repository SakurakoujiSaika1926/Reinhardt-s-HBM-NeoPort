package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.FloodlightBlockEntity;
import com.reinhardt.hbm.blockentity.FloodlightDummyBlockEntity;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** Invisible, full-collision footprint part with no power capability. */
public final class FloodlightDummyBlock extends Block implements EntityBlock {
    private static final ThreadLocal<Boolean> SUPPRESS_CORE_DESTROY = ThreadLocal.withInitial(() -> false);

    public FloodlightDummyBlock(Properties properties) {
        super(properties);
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

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FloodlightDummyBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                    BlockEntityType<T> type) {
        if (type != HbmBlockEntities.FLOODLIGHT_DUMMY.get()) {
            return null;
        }
        return (tickerLevel, tickerPos, tickerState, blockEntity) ->
                FloodlightDummyBlockEntity.tick(tickerLevel, tickerPos, tickerState,
                        (FloodlightDummyBlockEntity) blockEntity);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return corePartShape(level, pos);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return corePartShape(level, pos);
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof FloodlightDummyBlockEntity dummy) {
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

    /**
     * The old port used a full cube for every invisible footprint part.  That
     * hid the fact that the renderer rotates the three-block assembly when a
     * floor/ceiling mount flips.  Clip the core's complete model bounds to
     * this dummy's local block instead, so the collision follows that same
     * orientation and does not leave a stale one-block-wide bar behind.
     */
    private static VoxelShape corePartShape(BlockGetter level, BlockPos partPos) {
        if (!(level.getBlockEntity(partPos) instanceof FloodlightDummyBlockEntity dummy)) {
            return Shapes.block();
        }
        BlockPos corePos = dummy.corePos();
        BlockState coreState = level.getBlockState(corePos);
        if (!(coreState.getBlock() instanceof FloodlightBlock)) {
            return Shapes.block();
        }

        BlockPos offset = partPos.subtract(corePos);
        AABB bounds = FloodlightBlock.collisionShape(coreState).bounds().move(
                -offset.getX(), -offset.getY(), -offset.getZ());
        double minX = Math.max(0.0D, bounds.minX);
        double minY = Math.max(0.0D, bounds.minY);
        double minZ = Math.max(0.0D, bounds.minZ);
        double maxX = Math.min(1.0D, bounds.maxX);
        double maxY = Math.min(1.0D, bounds.maxY);
        double maxZ = Math.min(1.0D, bounds.maxZ);
        return minX < maxX && minY < maxY && minZ < maxZ
                ? Shapes.box(minX, minY, minZ, maxX, maxY, maxZ)
                : Shapes.empty();
    }

    /** The invisible footprint must not become an artificial floodlight ray stop. */
    @Override
    public int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return 0;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(HbmBlocks.FLOODLIGHT.get());
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof FloodlightDummyBlockEntity dummy) {
            dummy.setDropCore(!player.isCreative());
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hitResult) {
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.getBlockEntity(pos) instanceof FloodlightDummyBlockEntity dummy
                && level.getBlockEntity(dummy.corePos()) instanceof FloodlightBlockEntity) {
            BlockState coreState = level.getBlockState(dummy.corePos());
            return ((FloodlightBlock) coreState.getBlock()).useItemOn(
                    stack, coreState, level, dummy.corePos(), player, hand, hitResult.withPosition(dummy.corePos()));
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!SUPPRESS_CORE_DESTROY.get()
                && !movedByPiston
                && !state.is(newState.getBlock())
                && !level.isClientSide
                && level.getBlockEntity(pos) instanceof FloodlightDummyBlockEntity dummy
                && level.getBlockState(dummy.corePos()).getBlock() instanceof FloodlightBlock) {
            level.destroyBlock(dummy.corePos(), dummy.consumeDropCore());
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
