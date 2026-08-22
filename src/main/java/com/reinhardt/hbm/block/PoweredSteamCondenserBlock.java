package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.MachineDummyBlockEntity;
import com.reinhardt.hbm.blockentity.PoweredSteamCondenserBlockEntity;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.util.LegacyMachineGeometry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class PoweredSteamCondenserBlock extends LargeMachineBlock implements EntityBlock {
    private static final Footprint FOOTPRINT = createFootprint();

    public PoweredSteamCondenserBlock(Properties properties, VoxelShape shape) {
        super(properties, FOOTPRINT, shape, RotationBasis.HBM_LEGACY_SOUTH);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        if (!canPlaceAt(context.getLevel(), context.getClickedPos(), facing, context)) {
            return null;
        }
        return this.defaultBlockState().setValue(FACING, facing);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PoweredSteamCondenserBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return blockEntityType == HbmBlockEntities.POWERED_STEAM_CONDENSER.get()
                ? (tickerLevel, pos, tickerState, blockEntity) -> PoweredSteamCondenserBlockEntity.tick(
                tickerLevel,
                pos,
                tickerState,
                (PoweredSteamCondenserBlockEntity) blockEntity
        )
                : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return InteractionResult.PASS;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(oldState.getBlock())) {
            placeDummies(level, pos, state.getValue(FACING), (LivingEntity) null);
            refreshPorts(level, pos, state);
            PowerNetworkManager.markDirty(level);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            removeDummies(level, pos, state.getValue(FACING));
            refreshPorts(level, pos, state);
            PowerNetworkManager.markDirty(level);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (!level.isClientSide) {
            LargeMachineBlock.pushEntitiesOutOfPositions(level, pos, state.getValue(FACING), footprintPositions(pos, state.getValue(FACING)), placer);
            refreshPorts(level, pos, state);
        }
    }

    public static List<BlockPos> connectorPositions(BlockPos corePos, Direction facing) {
        Direction rot = LegacyMachineGeometry.forgeRotateUp(facing);
        return List.of(
                corePos.relative(rot, 4).above(),
                corePos.relative(rot.getOpposite(), 4).above(),
                corePos.relative(facing, 2).relative(rot.getOpposite()).above(),
                corePos.relative(facing, 2).relative(rot).above(),
                corePos.relative(facing.getOpposite(), 2).relative(rot.getOpposite()).above(),
                corePos.relative(facing.getOpposite(), 2).relative(rot).above()
        );
    }

    private static Footprint createFootprint() {
        Set<BlockPos> offsets = new LinkedHashSet<>();
        // HBM 1.7.10: getDimensions {U,D,N,S,W,E} = {2,0,1,1,3,3}, offset = 1.
        addOldDimBox(offsets, new int[]{2, 0, 1, 1, 3, 3});
        return new Footprint(List.copyOf(offsets));
    }

    private static void addOldDimBox(Set<BlockPos> offsets, int[] dim) {
        for (int y = -dim[1]; y <= dim[0]; y++) {
            for (int x = -dim[4]; x <= dim[5]; x++) {
                for (int z = -dim[2]; z <= dim[3]; z++) {
                    offsets.add(new BlockPos(x, y, z));
                }
            }
        }
    }

    private static boolean canPlaceAt(Level level, BlockPos corePos, Direction facing, BlockPlaceContext context) {
        for (BlockPos pos : footprintPositions(corePos, facing)) {
            if (pos.equals(corePos)) {
                continue;
            }
            if (!level.getBlockState(pos).canBeReplaced(context)) {
                return false;
            }
        }
        return true;
    }

    private static void placeDummies(Level level, BlockPos corePos, Direction facing, @Nullable LivingEntity placer) {
        for (BlockPos pos : footprintPositions(corePos, facing)) {
            if (pos.equals(corePos)) {
                continue;
            }
            level.setBlock(pos, HbmBlocks.MACHINE_DUMMY.get().defaultBlockState(), 3);
            if (level.getBlockEntity(pos) instanceof MachineDummyBlockEntity dummy) {
                dummy.setCorePos(corePos);
            }
        }
        if (placer != null) {
            LargeMachineBlock.pushEntitiesOutOfPositions(level, corePos, facing, footprintPositions(corePos, facing), placer);
        }
    }

    private static void removeDummies(Level level, BlockPos corePos, Direction facing) {
        MachineDummyBlock.runWithoutCoreDestroy(() -> {
            for (BlockPos pos : footprintPositions(corePos, facing)) {
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

    private static List<BlockPos> footprintPositions(BlockPos corePos, Direction facing) {
        return LegacyMachineGeometry.positionsForLegacyFootprint(corePos, facing, FOOTPRINT);
    }

    private static void refreshPorts(Level level, BlockPos corePos, BlockState state) {
        if (level.isClientSide) {
            return;
        }
        Direction facing = state.hasProperty(FACING) ? state.getValue(FACING) : Direction.SOUTH;
        for (PoweredSteamCondenserBlockEntity.Port port : PoweredSteamCondenserBlockEntity.portsFor(corePos, facing)) {
            EnergyCableBlock.refreshConnections(level, port.connectorPos());
            CatalyticCrackerBlock.refreshDuctsAtPort(level, port.pos(), port.connectorPos());
        }
    }
}

