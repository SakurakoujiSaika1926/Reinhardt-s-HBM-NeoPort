package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.ExcavatorBlockEntity;
import com.reinhardt.hbm.blockentity.MachineDummyBlockEntity;
import com.reinhardt.hbm.blockentity.MachineInventory;
import com.reinhardt.hbm.power.PowerNetworkManager;
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
import java.util.List;

public class ExcavatorBlock extends LargeMachineBlock implements EntityBlock {
    public static final int OFFSET = 3;
    public static final int HEIGHT_OFFSET = 3;
    public static final Footprint FOOTPRINT = footprint();

    public ExcavatorBlock(Properties properties, VoxelShape shape) {
        super(properties, FOOTPRINT, shape, RotationBasis.HBM_LEGACY_SOUTH);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        return canPlaceOldFootprintAt(context.getLevel(), legacyCorePos(context.getClickedPos(), state.getValue(FACING)), state.getValue(FACING), context) ? state : null;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ExcavatorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return blockEntityType == HbmBlockEntities.EXCAVATOR.get()
                ? (tickerLevel, pos, tickerState, blockEntity) -> {
                    if (blockEntity instanceof ExcavatorBlockEntity excavator) {
                        ExcavatorBlockEntity.tick(tickerLevel, pos, tickerState, excavator);
                    }
                }
                : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof MenuProvider menuProvider && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(menuProvider, buffer -> buffer.writeBlockPos(pos));
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(oldState.getBlock())) {
            placeOldDummies(level, pos, state.getValue(FACING), null);
        }
        if (!state.is(oldState.getBlock())) {
            PowerNetworkManager.markDirty(level);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof MachineInventory inventory) {
            inventory.dropContents(level, pos);
            PowerNetworkManager.markDirty(level);
        }
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            removeOldDummies(level, pos, state.getValue(FACING));
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (!level.isClientSide) {
            LargeMachineBlock.pushEntitiesOutOfPositions(level, pos, state.getValue(FACING), oldFootprintPositions(pos, state.getValue(FACING)), placer);
        }
    }

    public static BlockPos legacyCorePos(BlockPos clickedPos, Direction facing) {
        return clickedPos.above(HEIGHT_OFFSET).relative(facing, -OFFSET);
    }

    public static boolean canPlaceLegacyAt(Level level, BlockPos clickedPos, Direction facing, BlockPlaceContext context) {
        return canPlaceOldFootprintAt(level, legacyCorePos(clickedPos, facing), facing, context);
    }

    private static Footprint footprint() {
        List<BlockPos> offsets = new ArrayList<>();
        // HBM 1.7.10 MachineExcavator: core is placed at clicked + up(3) - facing * 3.
        // These are the exact MultiblockHandlerXR boxes relative to that core.
        addOldDimBox(offsets, new int[]{3, 0, 3, 3, 3, 3});
        addOldDimBox(offsets, new int[]{-1, 3, 3, -2, 3, -2});
        addOldDimBox(offsets, new int[]{-1, 3, 3, -2, -2, 3});
        addOldDimBox(offsets, new int[]{-1, 3, -2, 3, 3, 3});
        return new Footprint(List.copyOf(offsets));
    }

    private static void addOldDimBox(List<BlockPos> offsets, int[] dim) {
        addBox(offsets, -dim[4], dim[5], -dim[1], dim[0], -dim[2], dim[3]);
    }

    private static void addBox(List<BlockPos> offsets, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!offsets.contains(pos)) {
                        offsets.add(pos);
                    }
                }
            }
        }
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

    private static List<BlockPos> oldFootprintPositions(BlockPos corePos, Direction facing) {
        return LegacyMachineGeometry.positionsForLegacyFootprint(corePos, facing, FOOTPRINT);
    }
}

