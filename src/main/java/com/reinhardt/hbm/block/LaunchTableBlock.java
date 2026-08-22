package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.LauncherBlockEntity;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import org.jetbrains.annotations.Nullable;

public class LaunchTableBlock extends LargeMachineBlock implements EntityBlock {
    public static final Footprint FOOTPRINT = Footprint.centered(4, 1, 4);

    public LaunchTableBlock(Properties properties) {
        super(properties, FOOTPRINT, Shapes.empty(), RotationBasis.MODERN_NORTH);
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
            placeDummies(level, pos, state.getValue(FACING));
            clearScaffoldColumn(level, pos, state.getValue(FACING));
            pushEntitiesOutOfFootprint(level, pos, state.getValue(FACING), FOOTPRINT, placer);
        }
    }

    public static void placeDummies(Level level, BlockPos corePos, Direction facing) {
        boolean portsOnXAxis = facing == Direction.EAST || facing == Direction.WEST;
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                if (x == 0 && z == 0) {
                    continue;
                }
                Block block;
                if (x != 0 && z != 0) {
                    block = HbmBlocks.DUMMY_PORT_LAUNCH_TABLE.get();
                } else if (x == 0) {
                    block = portsOnXAxis ? HbmBlocks.DUMMY_PLATE_LAUNCH_TABLE.get() : HbmBlocks.DUMMY_PORT_LAUNCH_TABLE.get();
                } else {
                    block = portsOnXAxis ? HbmBlocks.DUMMY_PORT_LAUNCH_TABLE.get() : HbmBlocks.DUMMY_PLATE_LAUNCH_TABLE.get();
                }
                CompactLauncherBlock.placeDummy(level, corePos.offset(x, 0, z), corePos, block);
            }
        }
    }

    public static void clearScaffoldColumn(Level level, BlockPos corePos, Direction facing) {
        BlockPos base = switch (facing) {
            case NORTH -> corePos.offset(3, 0, 0);
            case SOUTH -> corePos.offset(-3, 0, 0);
            case EAST -> corePos.offset(0, 0, 3);
            case WEST -> corePos.offset(0, 0, -3);
            default -> corePos.offset(3, 0, 0);
        };
        for (int y = 1; y < 12; y++) {
            level.removeBlock(base.above(y), false);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            removeDummies(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    private static void removeDummies(Level level, BlockPos corePos) {
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
        return new LauncherBlockEntity(pos, state, LauncherBlockEntity.Kind.TABLE);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }
}
