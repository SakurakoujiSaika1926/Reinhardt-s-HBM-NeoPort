package com.reinhardt.hbm.block;

import com.mojang.serialization.MapCodec;
import com.reinhardt.hbm.blockentity.SatelliteDockBlockEntity;
import com.reinhardt.hbm.blockentity.SatelliteDockDummyBlockEntity;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** 1.7.10 MachineSatDock: a centered 3x3 cargo landing pad. */
public final class SatelliteDockBlock extends BaseEntityBlock {
    public static final MapCodec<SatelliteDockBlock> CODEC = simpleCodec(SatelliteDockBlock::new);
    private static final VoxelShape CORE_SHAPE = Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, 0.75D, 1.0D);
    private static final ThreadLocal<Boolean> REMOVING_PARTS = ThreadLocal.withInitial(() -> false);

    public SatelliteDockBlock(Properties properties) {
        super(properties);
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos core = context.getClickedPos();
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos occupied = core.offset(x, 0, z);
                if (!occupied.equals(core) && !context.getLevel().getBlockState(occupied).canBeReplaced(context)) {
                    return null;
                }
            }
        }
        return defaultBlockState();
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide) {
            return;
        }
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) {
                    continue;
                }
                BlockPos part = pos.offset(x, 0, z);
                level.setBlock(part, HbmBlocks.DUMMY_PLATE_CARGO.get().defaultBlockState(), UPDATE_ALL);
                if (level.getBlockEntity(part) instanceof SatelliteDockDummyBlockEntity dummy) {
                    dummy.setCorePos(pos);
                }
            }
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SatelliteDockBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != HbmBlockEntities.SAT_DOCK.get()) {
            return null;
        }
        return (tickerLevel, tickerPos, tickerState, blockEntity) ->
                SatelliteDockBlockEntity.tick(tickerLevel, tickerPos, tickerState, (SatelliteDockBlockEntity) blockEntity);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof SatelliteDockBlockEntity dock && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(dock, buffer -> buffer.writeBlockPos(pos));
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide && !REMOVING_PARTS.get()) {
            if (level.getBlockEntity(pos) instanceof SatelliteDockBlockEntity dock) {
                dock.dropContents(level, pos);
            }
            REMOVING_PARTS.set(true);
            try {
                SatelliteDockDummyBlock.runWithoutCoreRemoval(() -> {
                    for (int x = -1; x <= 1; x++) {
                        for (int z = -1; z <= 1; z++) {
                            if (x != 0 || z != 0) {
                                level.removeBlock(pos.offset(x, 0, z), false);
                            }
                        }
                    }
                });
            } finally {
                REMOVING_PARTS.set(false);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
        return CORE_SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
        return CORE_SHAPE;
    }
}
