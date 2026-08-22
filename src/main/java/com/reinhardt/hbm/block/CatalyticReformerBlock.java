package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.CatalyticReformerBlockEntity;
import com.reinhardt.hbm.blockentity.MachineInventory;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.util.LegacyMachineGeometry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
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
import java.util.Set;

public class CatalyticReformerBlock extends LargeMachineBlock implements EntityBlock {
    public static final Footprint FOOTPRINT = createFootprint();

    public CatalyticReformerBlock(Properties properties, VoxelShape shape) {
        super(properties, FOOTPRINT, shape, RotationBasis.MODERN_NORTH);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        return LargeMachineBlock.canPlaceFootprint(context, facing, FOOTPRINT)
                ? this.defaultBlockState().setValue(FACING, facing)
                : null;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CatalyticReformerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (blockEntityType != HbmBlockEntities.CATALYTIC_REFORMER.get()) {
            return null;
        }
        return (tickerLevel, pos, tickerState, blockEntity) -> CatalyticReformerBlockEntity.tick(
                tickerLevel,
                pos,
                tickerState,
                (CatalyticReformerBlockEntity) blockEntity
        );
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
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!state.is(oldState.getBlock())) {
            PowerNetworkManager.markDirty(level);
            refreshPorts(level, pos, state);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        boolean removed = !state.is(newState.getBlock());
        if (removed && level.getBlockEntity(pos) instanceof MachineInventory inventory) {
            inventory.dropContents(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (removed) {
            PowerNetworkManager.markDirty(level);
            refreshPorts(level, pos, state);
        }
    }

    private static void refreshPorts(Level level, BlockPos corePos, BlockState state) {
        if (level.isClientSide) {
            return;
        }
        Direction facing = state.hasProperty(FACING) ? state.getValue(FACING) : Direction.NORTH;
        for (CatalyticReformerBlockEntity.Port port : CatalyticReformerBlockEntity.portsFor(corePos, facing)) {
            BlockPos connectorPos = port.connectorPos();
            EnergyCableBlock.refreshConnections(level, connectorPos);
            CatalyticCrackerBlock.refreshDuctsAround(level, connectorPos);
        }
    }

    private static Footprint createFootprint() {
        Set<BlockPos> offsets = new LinkedHashSet<>();
        addLegacyFill(offsets, BlockPos.ZERO, new int[]{2, 0, 1, 1, 2, 2}, Direction.NORTH);
        addLegacyFill(offsets, new BlockPos(0, 0, 1), new int[]{3, -3, 1, 0, -1, 2}, Direction.NORTH);
        addLegacyFill(offsets, new BlockPos(0, 0, 1), new int[]{6, -3, 1, 1, 2, 0}, Direction.NORTH);
        offsets.add(BlockPos.ZERO);
        return new Footprint(offsets.stream().toList());
    }

    private static void addLegacyFill(Set<BlockPos> offsets, BlockPos anchor, int[] dim, Direction direction) {
        int[] rot = LegacyMachineGeometry.rotateLegacyDimensions(dim, direction);
        for (int x = anchor.getX() - rot[4]; x <= anchor.getX() + rot[5]; x++) {
            for (int y = anchor.getY() - rot[1]; y <= anchor.getY() + rot[0]; y++) {
                for (int z = anchor.getZ() - rot[2]; z <= anchor.getZ() + rot[3]; z++) {
                    offsets.add(new BlockPos(x, y, z));
                }
            }
        }
    }
}

