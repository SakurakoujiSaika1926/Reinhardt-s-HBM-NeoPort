package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.FrackingTowerBlockEntity;
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
import net.minecraft.world.level.block.Block;
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

public class FrackingTowerBlock extends LargeMachineBlock implements EntityBlock {
    @Override
    protected BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.setValue(FACING, mirror.getRotation(state.getValue(FACING)).rotate(state.getValue(FACING)));
    }

    public static final Footprint FOOTPRINT = createFootprint();

    public FrackingTowerBlock(Properties properties, VoxelShape shape) {
        super(properties, FOOTPRINT, shape, FrackingTowerBlock::mapFrackingTowerOffset);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        if (!this.canPlaceMachineFootprintAt(context.getLevel(), context.getClickedPos(), facing, context)) {
            return null;
        }
        return this.defaultBlockState().setValue(FACING, facing);
    }

    private static BlockPos mapFrackingTowerOffset(Direction facing, BlockPos offset) {
        int x = offset.getX();
        int y = offset.getY();
        int z = offset.getZ();
        return switch (facing) {
            case NORTH -> new BlockPos(x, y, z);
            case EAST -> new BlockPos(-z, y, x);
            case SOUTH -> new BlockPos(-x, y, -z);
            case WEST -> new BlockPos(z, y, -x);
            case UP, DOWN -> throw new IllegalArgumentException("Fracking tower requires a horizontal facing: " + facing);
        };
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FrackingTowerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (blockEntityType != HbmBlockEntities.FRACKING_TOWER.get()) {
            return null;
        }
        return (tickerLevel, pos, tickerState, blockEntity) -> FrackingTowerBlockEntity.tick(
                tickerLevel,
                pos,
                tickerState,
                (FrackingTowerBlockEntity) blockEntity
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

    private static void refreshPorts(Level level, BlockPos corePos) {
        if (level.isClientSide) {
            return;
        }
        for (FrackingTowerBlockEntity.Port port : FrackingTowerBlockEntity.portsFor(corePos)) {
            EnergyCableBlock.refreshConnections(level, port.pos());
            for (Direction direction : Direction.values()) {
                BlockPos neighbor = port.pos().relative(direction);
                BlockState state = level.getBlockState(neighbor);
                if (state.getBlock() instanceof FluidDuctBlock duct) {
                    duct.refreshConnections(level, neighbor);
                }
            }
        }
    }

    private static Footprint createFootprint() {
        Set<BlockPos> offsets = new LinkedHashSet<>();
        addSouthFill(offsets, 0, 0, 0, new int[]{3, 0, 0, 0, 0, 0});
        addSouthFill(offsets, 0, 2, 0, new int[]{1, 0, 3, 3, 3, 3});
        addNorthFill(offsets, -2, 2, -2, new int[]{-1, 2, 0, 1, 0, 1});
        addNorthFill(offsets, -2, 2, 3, new int[]{-1, 2, 0, 1, 0, 1});
        addNorthFill(offsets, 3, 2, -2, new int[]{-1, 2, 0, 1, 0, 1});
        addNorthFill(offsets, 3, 2, 3, new int[]{-1, 2, 0, 1, 0, 1});
        addSouthFill(offsets, 0, 0, 0, new int[]{10, -4, 2, 2, 2, 2});
        addSouthFill(offsets, 0, 0, 0, new int[]{24, -9, 1, 1, 1, 1});
        addWestFill(offsets, 0, 15, 0, new int[]{1, 0, 1, 1, -2, 3});
        offsets.add(BlockPos.ZERO);
        return new Footprint(offsets.stream().toList());
    }

    private static void addSouthFill(Set<BlockPos> offsets, int anchorX, int anchorY, int anchorZ, int[] dim) {
        addFill(offsets, anchorX, anchorY, anchorZ, dim[0], dim[1], dim[2], dim[3], dim[4], dim[5]);
    }

    private static void addNorthFill(Set<BlockPos> offsets, int anchorX, int anchorY, int anchorZ, int[] dim) {
        addFill(offsets, anchorX, anchorY, anchorZ, dim[0], dim[1], dim[3], dim[2], dim[5], dim[4]);
    }

    private static void addWestFill(Set<BlockPos> offsets, int anchorX, int anchorY, int anchorZ, int[] dim) {
        addFill(offsets, anchorX, anchorY, anchorZ, dim[0], dim[1], dim[4], dim[5], dim[3], dim[2]);
    }

    private static void addFill(Set<BlockPos> offsets, int anchorX, int anchorY, int anchorZ,
                                int up, int down, int north, int south, int west, int east) {
        for (int x = anchorX - west; x <= anchorX + east; x++) {
            for (int y = anchorY - down; y <= anchorY + up; y++) {
                for (int z = anchorZ - north; z <= anchorZ + south; z++) {
                    if (x == 0 && y == 0 && z == 0) {
                        continue;
                    }
                    offsets.add(new BlockPos(x, y, z));
                }
            }
        }
    }
}

