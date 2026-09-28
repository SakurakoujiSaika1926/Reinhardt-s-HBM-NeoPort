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
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import org.jetbrains.annotations.Nullable;

public class LaunchTableBlock extends LargeMachineBlock implements EntityBlock {
    @Override
    protected BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.setValue(FACING, mirror.getRotation(state.getValue(FACING)).rotate(state.getValue(FACING)));
    }

    public static final Footprint FOOTPRINT = Footprint.centered(4, 1, 4);

    public LaunchTableBlock(Properties properties) {
        // LaunchTable inherits BlockContainer's full core collision in 1.7.10;
        // only its surrounding dummy segments use the segmented shapes.
        super(properties, FOOTPRINT, Shapes.block(), LaunchTableBlock::mapLaunchTableOffset);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        return this.canPlaceMachineFootprintAt(context.getLevel(), context.getClickedPos(), state.getValue(FACING), context)
                ? state
                : null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.LivingEntity placer, net.minecraft.world.item.ItemStack stack) {
        if (!level.isClientSide) {
            placeDummies(level, pos, state.getValue(FACING));
            clearScaffoldColumn(level, pos, state.getValue(FACING));
            pushEntitiesOutOfFootprint(level, pos, state.getValue(FACING), FOOTPRINT, placer,
                    LaunchTableBlock::mapLaunchTableOffset);
        }
    }

    public static void placeDummies(Level level, BlockPos corePos, Direction facing) {
        switch (facing) {
            case NORTH -> placeNorthDummies(level, corePos);
            case EAST -> placeEastDummies(level, corePos);
            case SOUTH -> placeSouthDummies(level, corePos);
            case WEST -> placeWestDummies(level, corePos);
            case UP, DOWN -> throw new IllegalArgumentException("Launch table requires a horizontal facing: " + facing);
        }
    }

    private static void placeNorthDummies(Level level, BlockPos corePos) {
        placeLaunchTableGrid(level, corePos, HbmBlocks.DUMMY_PORT_LAUNCH_TABLE.get(),
                HbmBlocks.DUMMY_PLATE_LAUNCH_TABLE.get());
    }

    private static void placeEastDummies(Level level, BlockPos corePos) {
        placeLaunchTableGrid(level, corePos, HbmBlocks.DUMMY_PLATE_LAUNCH_TABLE.get(),
                HbmBlocks.DUMMY_PORT_LAUNCH_TABLE.get());
    }

    private static void placeSouthDummies(Level level, BlockPos corePos) {
        placeLaunchTableGrid(level, corePos, HbmBlocks.DUMMY_PORT_LAUNCH_TABLE.get(),
                HbmBlocks.DUMMY_PLATE_LAUNCH_TABLE.get());
    }

    private static void placeWestDummies(Level level, BlockPos corePos) {
        placeLaunchTableGrid(level, corePos, HbmBlocks.DUMMY_PLATE_LAUNCH_TABLE.get(),
                HbmBlocks.DUMMY_PORT_LAUNCH_TABLE.get());
    }

    private static void placeLaunchTableGrid(Level level, BlockPos corePos, Block xAxisBlock, Block zAxisBlock) {
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                if (x == 0 && z == 0) {
                    continue;
                }
                Block block;
                if (x != 0 && z != 0) {
                    block = HbmBlocks.DUMMY_PORT_LAUNCH_TABLE.get();
                } else if (x == 0) {
                    block = zAxisBlock;
                } else {
                    block = xAxisBlock;
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
        if (!movedByPiston && !state.is(newState.getBlock()) && !level.isClientSide) {
            if (level.getBlockEntity(pos) instanceof LauncherBlockEntity launcher) launcher.dropContents(level, pos);
            removeDummies(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    private static void removeDummies(Level level, BlockPos corePos) {
        if (level.getBlockEntity(corePos) == null) {
            return;
        }
        MachineDummyBlock.runWithoutCoreDestroy(() -> {
            for (BlockPos offset : FOOTPRINT.offsets()) {
                BlockPos pos = corePos.offset(mapLaunchTableOffset(Direction.NORTH, offset));
                if (!pos.equals(corePos) && level.getBlockState(pos).getBlock() instanceof LauncherDummyBlock) {
                    level.removeBlock(pos, false);
                }
            }
        });
    }

    private static BlockPos mapLaunchTableOffset(Direction facing, BlockPos offset) {
        int x = offset.getX();
        int y = offset.getY();
        int z = offset.getZ();
        return switch (facing) {
            case NORTH -> new BlockPos(x, y, z);
            case EAST -> new BlockPos(-z, y, x);
            case SOUTH -> new BlockPos(-x, y, -z);
            case WEST -> new BlockPos(z, y, -x);
            case UP, DOWN -> throw new IllegalArgumentException("Launch table requires a horizontal facing: " + facing);
        };
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
