package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.LauncherBlockEntity;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class CompactLauncherBlock extends LargeMachineBlock implements EntityBlock {
    public static final Footprint FOOTPRINT = Footprint.centered(1, 1, 1);
    private static final VoxelShape CORE_SHAPE = Shapes.empty();

    public CompactLauncherBlock(Properties properties) {
        super(properties, FOOTPRINT, CORE_SHAPE, RotationBasis.MODERN_NORTH);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        return canPlaceFootprintAt(context.getLevel(), context.getClickedPos(), state.getValue(FACING), FOOTPRINT, context)
                ? state
                : null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.LivingEntity placer, net.minecraft.world.item.ItemStack stack) {
        if (!level.isClientSide) {
            placeDummies(level, pos);
            pushEntitiesOutOfFootprint(level, pos, state.getValue(FACING), FOOTPRINT, placer);
        }
    }

    public static void placeDummies(Level level, BlockPos corePos) {
        placeDummy(level, corePos.offset(1, 0, 1), corePos, HbmBlocks.DUMMY_PORT_COMPACT_LAUNCHER.get());
        placeDummy(level, corePos.offset(1, 0, 0), corePos, HbmBlocks.DUMMY_PLATE_COMPACT_LAUNCHER.get());
        placeDummy(level, corePos.offset(1, 0, -1), corePos, HbmBlocks.DUMMY_PORT_COMPACT_LAUNCHER.get());
        placeDummy(level, corePos.offset(0, 0, -1), corePos, HbmBlocks.DUMMY_PLATE_COMPACT_LAUNCHER.get());
        placeDummy(level, corePos.offset(-1, 0, -1), corePos, HbmBlocks.DUMMY_PORT_COMPACT_LAUNCHER.get());
        placeDummy(level, corePos.offset(-1, 0, 0), corePos, HbmBlocks.DUMMY_PLATE_COMPACT_LAUNCHER.get());
        placeDummy(level, corePos.offset(-1, 0, 1), corePos, HbmBlocks.DUMMY_PORT_COMPACT_LAUNCHER.get());
        placeDummy(level, corePos.offset(0, 0, 1), corePos, HbmBlocks.DUMMY_PLATE_COMPACT_LAUNCHER.get());
    }

    public static void placeDummy(Level level, BlockPos pos, BlockPos corePos, Block block) {
        level.setBlock(pos, block.defaultBlockState(), Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof com.reinhardt.hbm.blockentity.MachineDummyBlockEntity dummy) {
            dummy.setCorePos(corePos);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            removeDummies(level, pos);
            if (level.getBlockEntity(pos) instanceof LauncherBlockEntity launcher) launcher.dropContents(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    private static void removeDummies(Level level, BlockPos corePos) {
        if (level.getBlockEntity(corePos) == null) {
            return;
        }
        MachineDummyBlock.runWithoutCoreDestroy(() -> {
            for (BlockPos offset : FOOTPRINT.offsets()) {
                BlockPos pos = corePos.offset(offset);
                if (!pos.equals(corePos) && level.getBlockState(pos).getBlock() instanceof LauncherDummyBlock) {
                    level.removeBlock(pos, false);
                }
            }
        });
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LauncherBlockEntity(pos, state, LauncherBlockEntity.Kind.COMPACT);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                                    net.minecraft.world.entity.player.Player player,
                                                                    net.minecraft.world.phys.BlockHitResult hit) {
        if (level.isClientSide) return net.minecraft.world.InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) return net.minecraft.world.InteractionResult.PASS;
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof LauncherBlockEntity launcher
                && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            serverPlayer.openMenu(launcher, buffer -> buffer.writeBlockPos(pos));
        }
        return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != com.reinhardt.hbm.registry.HbmBlockEntities.LAUNCHER.get()) return null;
        return (l, p, s, be) -> LauncherBlockEntity.tick(l, p, s, (LauncherBlockEntity) be);
    }
}
