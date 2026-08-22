package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.MachineInventory;
import com.reinhardt.hbm.blockentity.WatzBlockEntity;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
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

public class WatzBlock extends Block implements EntityBlock {
    private static final int LEGACY_CORE_OFFSET = 3;

    public WatzBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos corePos = corePosForPlacement(context.getClickedPos(), legacyPlacementDirection(context.getPlayer()));
        return canPlaceWatz(context, corePos) ? defaultBlockState() : null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide) {
            return;
        }
        Direction direction = legacyPlacementDirection(placer);
        BlockPos corePos = corePosForPlacement(pos, direction);
        MachineDummyBlock.runWithoutCoreDestroy(() -> {
            if (!corePos.equals(pos)) {
                level.removeBlock(pos, false);
                level.setBlock(corePos, defaultBlockState(), Block.UPDATE_ALL);
            }
            WatzBlockEntity.fillDummies(level, corePos);
        });
        LargeMachineBlock.pushEntitiesOutOfPositions(level, corePos, direction, WatzBlockEntity.occupiedPositions(corePos), placer);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WatzBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return (tickerLevel, pos, tickerState, blockEntity) -> {
            if (blockEntity instanceof WatzBlockEntity watz) {
                WatzBlockEntity.tick(tickerLevel, pos, tickerState, watz);
            }
        };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
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
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof MachineInventory inventory) {
                inventory.dropContents(level, pos);
            }
            WatzBlockEntity.removeDummies(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    public net.minecraft.world.item.ItemStack getCloneItemStack(net.minecraft.world.level.LevelReader level, BlockPos pos, BlockState state) {
        return new net.minecraft.world.item.ItemStack(HbmBlocks.STRUCT_WATZ_CORE.get());
    }

    private static boolean canPlaceWatz(BlockPlaceContext context, BlockPos corePos) {
        for (BlockPos occupied : WatzBlockEntity.occupiedPositions(corePos)) {
            if (!context.getLevel().getBlockState(occupied).canBeReplaced(context)) {
                return false;
            }
        }
        return true;
    }

    private static BlockPos corePosForPlacement(BlockPos clickedPos, Direction direction) {
        return clickedPos.offset(direction.getStepX() * -LEGACY_CORE_OFFSET, 0, direction.getStepZ() * -LEGACY_CORE_OFFSET);
    }

    private static Direction legacyPlacementDirection(@Nullable LivingEntity placer) {
        if (placer == null) {
            return Direction.NORTH;
        }
        int index = Mth.floor(placer.getYRot() * 4.0F / 360.0F + 0.5D) & 3;
        return switch (index) {
            case 1 -> Direction.EAST;
            case 2 -> Direction.SOUTH;
            case 3 -> Direction.WEST;
            default -> Direction.NORTH;
        };
    }
}
