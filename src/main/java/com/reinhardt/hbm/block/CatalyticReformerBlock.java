package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.CatalyticReformerBlockEntity;
import com.reinhardt.hbm.blockentity.MachineInventory;
import com.reinhardt.hbm.power.PowerNetworkManager;
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

public class CatalyticReformerBlock extends LargeMachineBlock implements EntityBlock {
    @Override
    protected BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.setValue(FACING, mirror.getRotation(state.getValue(FACING)).rotate(state.getValue(FACING)));
    }

    public static final Footprint FOOTPRINT = createFootprint();

    public CatalyticReformerBlock(Properties properties, VoxelShape shape) {
        super(properties, FOOTPRINT, shape, CatalyticReformerBlock::mapCatalyticReformerOffset);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        return LargeMachineBlock.canPlaceFootprint(context, facing, FOOTPRINT,
                CatalyticReformerBlock::mapCatalyticReformerOffset)
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
        addAuthoredFill(offsets, BlockPos.ZERO, new int[]{2, 0, 1, 1, 2, 2});
        addAuthoredFill(offsets, new BlockPos(0, 0, 1), new int[]{3, -3, 0, 1, 2, -1});
        addAuthoredFill(offsets, new BlockPos(0, 0, 1), new int[]{6, -3, 1, 1, 0, 2});
        offsets.add(BlockPos.ZERO);
        return new Footprint(offsets.stream().toList());
    }

    private static void addAuthoredFill(Set<BlockPos> offsets, BlockPos anchor, int[] dimensions) {
        for (int x = anchor.getX() - dimensions[4]; x <= anchor.getX() + dimensions[5]; x++) {
            for (int y = anchor.getY() - dimensions[1]; y <= anchor.getY() + dimensions[0]; y++) {
                for (int z = anchor.getZ() - dimensions[2]; z <= anchor.getZ() + dimensions[3]; z++) {
                    offsets.add(new BlockPos(x, y, z));
                }
            }
        }
    }

    private static BlockPos mapCatalyticReformerOffset(Direction facing, BlockPos offset) {
        return switch (facing) {
            case NORTH -> new BlockPos(offset.getX(), offset.getY(), offset.getZ());
            case EAST -> new BlockPos(-offset.getZ(), offset.getY(), offset.getX());
            case SOUTH -> new BlockPos(-offset.getX(), offset.getY(), -offset.getZ());
            case WEST -> new BlockPos(offset.getZ(), offset.getY(), -offset.getX());
            case UP, DOWN -> throw new IllegalArgumentException("Catalytic reformer requires a horizontal facing: " + facing);
        };
    }
}

