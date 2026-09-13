package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.MachineInventory;
import com.reinhardt.hbm.blockentity.RefineryBlockEntity;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Set;

public class RefineryBlock extends LargeMachineBlock implements EntityBlock {
    public static final Footprint FOOTPRINT = createFootprint();

    public RefineryBlock(Properties properties, VoxelShape shape) {
        super(properties, FOOTPRINT, shape, RotationBasis.MODERN_NORTH);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = this.defaultBlockState().setValue(FACING, Direction.NORTH);
        return LargeMachineBlock.canPlaceFootprint(context, state, FOOTPRINT) ? state : null;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RefineryBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (blockEntityType != HbmBlockEntities.REFINERY.get()) {
            return null;
        }
        return (tickerLevel, pos, tickerState, blockEntity) -> RefineryBlockEntity.tick(
                tickerLevel,
                pos,
                tickerState,
                (RefineryBlockEntity) blockEntity
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
            refreshPorts(level, pos);
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
            refreshPorts(level, pos);
        }
    }

    @Override
    public void onBlockExploded(BlockState state, Level level, BlockPos pos, Explosion explosion) {
        RefineryBlockEntity.awardInfernoForBombletExplosion(level, pos, explosion);
        super.onBlockExploded(state, level, pos, explosion);
    }

    private static void refreshPorts(Level level, BlockPos corePos) {
        if (level.isClientSide) {
            return;
        }
        for (RefineryBlockEntity.Port port : RefineryBlockEntity.portsFor(corePos)) {
            BlockPos connectorPos = port.connectorPos();
            EnergyCableBlock.refreshConnections(level, connectorPos);
            for (Direction direction : Direction.values()) {
                BlockPos neighbor = connectorPos.relative(direction);
                BlockState state = level.getBlockState(neighbor);
                if (state.getBlock() instanceof FluidDuctBlock duct) {
                    duct.refreshConnections(level, neighbor);
                }
            }
        }
    }

    private static Footprint createFootprint() {
        Set<BlockPos> offsets = new LinkedHashSet<>();
        for (int y = 0; y <= 8; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    offsets.add(new BlockPos(x, y, z));
                }
            }
        }
        offsets.add(BlockPos.ZERO);
        return new Footprint(offsets.stream().toList());
    }
}

