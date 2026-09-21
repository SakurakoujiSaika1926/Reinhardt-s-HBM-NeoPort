package com.reinhardt.hbm.block;

import com.mojang.serialization.MapCodec;
import com.reinhardt.hbm.blockentity.SatelliteDockBlockEntity;
import com.reinhardt.hbm.blockentity.SatelliteDockDummyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** The eight half-height, non-item cargo-pad cells from DummyBlockMachine. */
public final class SatelliteDockDummyBlock extends BaseEntityBlock {
    public static final MapCodec<SatelliteDockDummyBlock> CODEC = simpleCodec(SatelliteDockDummyBlock::new);
    private static final VoxelShape SHAPE = Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, 0.5D, 1.0D);
    private static final ThreadLocal<Boolean> SUPPRESS_CORE_REMOVAL = ThreadLocal.withInitial(() -> false);

    public SatelliteDockDummyBlock(Properties properties) {
        super(properties);
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SatelliteDockDummyBlockEntity(pos, state);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        if (level.getBlockEntity(pos) instanceof SatelliteDockDummyBlockEntity dummy) {
            BlockState coreState = level.getBlockState(dummy.corePos());
            return coreState.getBlock().getCloneItemStack(level, dummy.corePos(), coreState);
        }
        return ItemStack.EMPTY;
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof SatelliteDockDummyBlockEntity dummy) {
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
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof SatelliteDockDummyBlockEntity dummy) {
            dummy.setDropCoreWhenRemoved(!player.isCreative());
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof SatelliteDockDummyBlockEntity dummy
                && level.getBlockEntity(dummy.corePos()) instanceof SatelliteDockBlockEntity dock
                && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(dock, buffer -> buffer.writeBlockPos(dummy.corePos()));
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!SUPPRESS_CORE_REMOVAL.get() && !movedByPiston && !state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof SatelliteDockDummyBlockEntity dummy) {
            BlockPos core = dummy.corePos();
            if (level.getBlockEntity(core) instanceof SatelliteDockBlockEntity) {
                level.destroyBlock(core, dummy.consumeDropCoreWhenRemoved());
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    public static void runWithoutCoreRemoval(Runnable action) {
        boolean previous = SUPPRESS_CORE_REMOVAL.get();
        SUPPRESS_CORE_REMOVAL.set(true);
        try {
            action.run();
        } finally {
            SUPPRESS_CORE_REMOVAL.set(previous);
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
