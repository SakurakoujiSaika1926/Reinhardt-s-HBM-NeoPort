package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.CokerBlockEntity;
import com.reinhardt.hbm.blockentity.MachineInventory;
import com.reinhardt.hbm.registry.HbmBlockEntities;
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

public class MachineCokerBlock extends LargeMachineBlock implements EntityBlock {
    public static final Footprint FOOTPRINT = createFootprint();

    public MachineCokerBlock(Properties properties, VoxelShape shape) {
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
        return new CokerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (blockEntityType != HbmBlockEntities.COKER.get()) {
            return null;
        }
        return (tickerLevel, pos, tickerState, blockEntity) -> CokerBlockEntity.tick(
                tickerLevel,
                pos,
                tickerState,
                (CokerBlockEntity) blockEntity
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
            refreshPorts(level, pos);
        }
    }

    private static void refreshPorts(Level level, BlockPos corePos) {
        if (level.isClientSide) {
            return;
        }
        for (CokerBlockEntity.Port port : CokerBlockEntity.portsFor(corePos)) {
            CatalyticCrackerBlock.refreshDuctsAround(level, port.connectorPos());
        }
    }

    private static Footprint createFootprint() {
        Set<BlockPos> offsets = new LinkedHashSet<>();
        for (int y = 0; y <= 22; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    offsets.add(new BlockPos(x, y, z));
                }
            }
        }
        for (int y = 1; y <= 6; y++) {
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    offsets.add(new BlockPos(x, y, z));
                }
            }
        }
        for (int y = 1; y <= 2; y++) {
            offsets.add(new BlockPos(2, y, 2));
            offsets.add(new BlockPos(2, y, -2));
            offsets.add(new BlockPos(-2, y, 2));
            offsets.add(new BlockPos(-2, y, -2));
        }
        offsets.add(BlockPos.ZERO);
        return new Footprint(offsets.stream().toList());
    }
}

