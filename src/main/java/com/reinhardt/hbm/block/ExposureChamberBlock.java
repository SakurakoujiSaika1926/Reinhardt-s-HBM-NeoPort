package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.ExposureChamberBlockEntity;
import com.reinhardt.hbm.blockentity.MachineDummyBlockEntity;
import com.reinhardt.hbm.blockentity.MachineInventory;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.util.LegacyMachineGeometry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ExposureChamberBlock extends LargeMachineBlock implements EntityBlock {
    public static final Footprint FOOTPRINT = Footprint.fromOffsets(BlockPos.ZERO);

    public ExposureChamberBlock(Properties properties, VoxelShape shape) {
        super(properties, FOOTPRINT, shape, RotationBasis.HBM_LEGACY_SOUTH);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        return canPlaceOldFootprintAt(context.getLevel(), context.getClickedPos(), facing, context)
                ? defaultBlockState().setValue(FACING, facing)
                : null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (!level.isClientSide) {
            placeOldDummies(level, pos, state.getValue(FACING), placer);
            refreshPorts(level, pos, state);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ExposureChamberBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != HbmBlockEntities.EXPOSURE_CHAMBER.get()) {
            return null;
        }
        return (tickerLevel, pos, tickerState, blockEntity) -> ExposureChamberBlockEntity.tick(tickerLevel, pos, tickerState, (ExposureChamberBlockEntity) blockEntity);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player.isCrouching()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof MenuProvider menuProvider && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(menuProvider, buffer -> buffer.writeBlockPos(pos));
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof MachineInventory inventory) {
            inventory.dropContents(level, pos);
        }
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            removeOldDummies(level, pos, state.getValue(FACING));
            refreshPorts(level, pos, state);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    public static BlockPos legacyCorePos(BlockPos clickedPos, Direction facing) {
        return clickedPos.relative(facing, -2);
    }

    public static boolean canPlaceLegacyAt(Level level, BlockPos clickedPos, Direction facing, BlockPlaceContext context) {
        BlockPos corePos = legacyCorePos(clickedPos, facing);
        if (!level.getWorldBorder().isWithinBounds(corePos)) {
            return false;
        }
        if (!level.getBlockState(corePos).canBeReplaced(context)) {
            return false;
        }
        return canPlaceOldFootprintAt(level, corePos, facing, context);
    }

    public void placeLegacyDummies(Level level, BlockPos corePos, Direction facing, @Nullable LivingEntity placer) {
        placeOldDummies(level, corePos, facing, placer);
        refreshPorts(level, corePos, defaultBlockState().setValue(FACING, facing));
    }

    private static boolean canPlaceOldFootprintAt(Level level, BlockPos corePos, Direction facing, BlockPlaceContext context) {
        for (BlockPos pos : oldFootprintPositions(corePos, facing)) {
            if (pos.equals(corePos)) {
                continue;
            }
            if (!level.getBlockState(pos).canBeReplaced(context)) {
                return false;
            }
        }
        return true;
    }

    private static void placeOldDummies(Level level, BlockPos corePos, Direction facing, @Nullable LivingEntity placer) {
        for (BlockPos pos : oldFootprintPositions(corePos, facing)) {
            if (pos.equals(corePos)) {
                continue;
            }
            level.setBlock(pos, HbmBlocks.MACHINE_DUMMY.get().defaultBlockState(), 3);
            if (level.getBlockEntity(pos) instanceof MachineDummyBlockEntity dummy) {
                dummy.setCorePos(corePos);
            }
        }
        if (placer != null) {
            LargeMachineBlock.pushEntitiesOutOfPositions(level, corePos, facing, oldFootprintPositions(corePos, facing), placer);
        }
    }

    private static void removeOldDummies(Level level, BlockPos corePos, Direction facing) {
        if (level.getBlockEntity(corePos) == null) {
            return;
        }
        MachineDummyBlock.runWithoutCoreDestroy(() -> {
            for (BlockPos pos : oldFootprintPositions(corePos, facing)) {
                if (pos.equals(corePos)) {
                    continue;
                }
                if (level.getBlockState(pos).is(HbmBlocks.MACHINE_DUMMY.get())
                        && level.getBlockEntity(pos) instanceof MachineDummyBlockEntity dummy
                        && dummy.getCorePos().equals(corePos)) {
                    level.removeBlock(pos, false);
                }
            }
        });
    }

    private static void refreshPorts(Level level, BlockPos corePos, BlockState state) {
        if (level.isClientSide) {
            return;
        }
        Direction facing = state.hasProperty(FACING) ? state.getValue(FACING) : Direction.NORTH;
        for (BlockPos connectorPos : ExposureChamberBlockEntity.powerConnectors(corePos, facing)) {
            EnergyCableBlock.refreshConnections(level, connectorPos);
        }
    }

    private static List<BlockPos> oldFootprintPositions(BlockPos corePos, Direction facing) {
        Set<BlockPos> offsets = new LinkedHashSet<>();
        addOldFill(offsets, BlockPos.ZERO, new int[]{4, 0, 2, 2, 2, 2}, facing);
        addOldFill(offsets, BlockPos.ZERO, new int[]{3, 0, 0, 0, -3, 8}, facing);
        addOldFill(offsets, new BlockPos(0, 2, 0), new int[]{0, 0, 1, -1, -3, 6}, facing);
        addOldFill(offsets, new BlockPos(0, 2, 0), new int[]{0, 0, -1, 1, -3, 6}, facing);

        Direction rot = LegacyMachineGeometry.forgeRotateUp(facing).getOpposite();
        BlockPos sideOrigin = new BlockPos(rot.getStepX() * 7, 0, rot.getStepZ() * 7);
        addOldFill(offsets, sideOrigin, new int[]{3, 0, 1, -1, 0, 1}, facing);
        addOldFill(offsets, sideOrigin, new int[]{3, 0, -1, 1, 0, 1}, facing);

        offsets.add(sideOrigin.relative(facing));
        offsets.add(sideOrigin.relative(facing.getOpposite()));
        offsets.add(new BlockPos(rot.getStepX() * 8, 0, rot.getStepZ() * 8).relative(facing));
        offsets.add(new BlockPos(rot.getStepX() * 8, 0, rot.getStepZ() * 8).relative(facing.getOpposite()));
        offsets.add(new BlockPos(rot.getStepX() * 8, 0, rot.getStepZ() * 8));

        ArrayList<BlockPos> positions = new ArrayList<>(offsets.size());
        for (BlockPos offset : offsets) {
            positions.add(corePos.offset(offset));
        }
        return List.copyOf(positions);
    }

    private static void addOldFill(Set<BlockPos> offsets, BlockPos origin, int[] dim, Direction facing) {
        int[] rot = LegacyMachineGeometry.rotateLegacyDimensions(dim, facing);
        for (int x = origin.getX() - rot[4]; x <= origin.getX() + rot[5]; x++) {
            for (int y = origin.getY() - rot[1]; y <= origin.getY() + rot[0]; y++) {
                for (int z = origin.getZ() - rot[2]; z <= origin.getZ() + rot[3]; z++) {
                    offsets.add(new BlockPos(x, y, z));
                }
            }
        }
    }
}

