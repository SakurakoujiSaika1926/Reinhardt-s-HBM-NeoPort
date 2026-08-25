package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.MachineInventory;
import com.reinhardt.hbm.blockentity.OilDerrickBlockEntity;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
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
import java.util.Set;

public class OilPumpjackBlock extends LargeMachineBlock implements EntityBlock {
    public static final Footprint FOOTPRINT = createFootprint();

    public OilPumpjackBlock(Properties properties, VoxelShape shape) {
        super(properties, FOOTPRINT, shape, RotationBasis.MODERN_NORTH);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        if (!LargeMachineBlock.canPlaceFootprint(context, facing, FOOTPRINT)) {
            return null;
        }
        return this.defaultBlockState().setValue(FACING, facing);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OilDerrickBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (blockEntityType != HbmBlockEntities.OIL_DERRICK.get()) {
            return null;
        }
        return (tickerLevel, pos, tickerState, blockEntity) -> OilDerrickBlockEntity.tick(
                tickerLevel,
                pos,
                tickerState,
                (OilDerrickBlockEntity) blockEntity
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
            refreshConnectorCables(level, pos, state);
        }
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            refreshConnectorCables(level, pos, state);
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
            refreshConnectorCables(level, pos, state);
        }
    }

    private static void refreshConnectorCables(Level level, BlockPos corePos, BlockState state) {
        if (level.isClientSide) {
            return;
        }
        Direction facing = state.hasProperty(FACING) ? state.getValue(FACING) : Direction.NORTH;
        for (OilDerrickBlockEntity.Port port : OilDerrickBlockEntity.pumpjackPorts(corePos, facing)) {
            refreshConnector(level, port.pos());
            refreshConnector(level, port.accessPos());
        }
    }

    private static void refreshConnector(Level level, BlockPos pos) {
        EnergyCableBlock.refreshConnections(level, pos);
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof FluidDuctBlock duct) {
            duct.refreshConnections(level, pos);
        }
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = pos.relative(direction);
            BlockState neighborState = level.getBlockState(neighbor);
            if (neighborState.getBlock() instanceof FluidDuctBlock duct) {
                duct.refreshConnections(level, neighbor);
            } else if (neighborState.getBlock() instanceof EnergyCableBlock) {
                EnergyCableBlock.refreshConnections(level, neighbor);
            }
        }
    }

    private static Footprint createFootprint() {
        Set<BlockPos> offsets = new LinkedHashSet<>();
        for (int x = -6; x <= 0; x++) {
            for (int y = 0; y <= 3; y++) {
                offsets.add(new BlockPos(x, y, 0));
            }
        }
        for (int x = -5; x <= -1; x++) {
            for (int z = -1; z <= 1; z++) {
                offsets.add(new BlockPos(x, 0, z));
            }
        }
        for (int x = -4; x <= -2; x++) {
            offsets.add(new BlockPos(x, 0, -1));
            offsets.add(new BlockPos(x, 0, 1));
        }
        return new Footprint(offsets.stream().toList());
    }
}

