package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.CompressorBlockEntity;
import com.reinhardt.hbm.blockentity.MachineInventory;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
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

public class CompressorBlock extends LargeMachineBlock implements EntityBlock {
    private final CompressorBlockEntity.Kind kind;

    public CompressorBlock(Properties properties, VoxelShape shape, CompressorBlockEntity.Kind kind) {
        super(properties, footprint(kind), shape, offsetMapper(kind));
        this.kind = kind;
    }

    public CompressorBlockEntity.Kind kind() {
        return this.kind;
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return switch (this.kind) {
            case NORMAL -> normalCompressorStateForPlacement(context);
            case COMPACT -> compactCompressorStateForPlacement(context);
        };
    }

    private BlockState normalCompressorStateForPlacement(
            net.minecraft.world.item.context.BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        if (!this.canPlaceMachineFootprintAt(context.getLevel(), context.getClickedPos(), facing, context)) {
            return null;
        }
        return this.defaultBlockState().setValue(FACING, facing);
    }

    private BlockState compactCompressorStateForPlacement(
            net.minecraft.world.item.context.BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        if (!this.canPlaceMachineFootprintAt(context.getLevel(), context.getClickedPos(), facing, context)) {
            return null;
        }
        return this.defaultBlockState().setValue(FACING, facing);
    }

    @Override
    protected BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return switch (this.kind) {
            case NORMAL -> rotateNormalCompressor(state, rotation);
            case COMPACT -> rotateCompactCompressor(state, rotation);
        };
    }

    private static BlockState rotateNormalCompressor(
            BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateCompactCompressor(
            BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return switch (this.kind) {
            case NORMAL -> mirrorNormalCompressor(state, mirror);
            case COMPACT -> mirrorCompactCompressor(state, mirror);
        };
    }

    private static BlockState mirrorNormalCompressor(
            BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.setValue(FACING, mirror.getRotation(state.getValue(FACING)).rotate(state.getValue(FACING)));
    }

    private static BlockState mirrorCompactCompressor(
            BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.setValue(FACING, mirror.getRotation(state.getValue(FACING)).rotate(state.getValue(FACING)));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CompressorBlockEntity(pos, state, this.kind);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return blockEntityType == HbmBlockEntities.COMPRESSOR.get()
                ? (tickerLevel, pos, tickerState, blockEntity) -> {
                    if (blockEntity instanceof CompressorBlockEntity compressor) {
                        CompressorBlockEntity.tick(tickerLevel, pos, tickerState, compressor);
                    }
                }
                : null;
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
        boolean removed = !state.is(newState.getBlock());
        if (removed && level.getBlockEntity(pos) instanceof MachineInventory inventory) {
            inventory.dropContents(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    private static Footprint footprint(CompressorBlockEntity.Kind kind) {
        return switch (kind) {
            case NORMAL -> normalFootprint();
            case COMPACT -> compactFootprint();
        };
    }

    private static OffsetMapper offsetMapper(CompressorBlockEntity.Kind kind) {
        return switch (kind) {
            case NORMAL -> CompressorBlock::mapNormalCompressorOffset;
            case COMPACT -> CompressorBlock::mapCompactCompressorOffset;
        };
    }

    private static Footprint normalFootprint() {
        LinkedHashSet<BlockPos> offsets = new LinkedHashSet<>();
        for (int y = 0; y <= 2; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 2; z++) {
                    offsets.add(new BlockPos(x, y, z));
                }
            }
        }

        BlockPos shiftedCore = new BlockPos(0, 0, 2);
        for (int y = 3; y <= 3; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    offsets.add(shiftedCore.offset(x, y, z));
                }
            }
        }
        for (int y = 4; y <= 8; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = 0; z <= 0; z++) {
                    offsets.add(shiftedCore.offset(x, y, z));
                }
            }
        }

        offsets.add(new BlockPos(0, 0, 1));
        offsets.add(new BlockPos(1, 0, 2));
        offsets.add(new BlockPos(-1, 0, 2));
        return new Footprint(List.copyOf(offsets));
    }

    private static Footprint compactFootprint() {
        LinkedHashSet<BlockPos> offsets = new LinkedHashSet<>();
        for (int y = 0; y <= 2; y++) {
            for (int x = -3; x <= 3; x++) {
                for (int z = -1; z <= 1; z++) {
                    offsets.add(new BlockPos(x, y, z));
                }
            }
        }

        offsets.add(new BlockPos(3, 1, 0));
        offsets.add(new BlockPos(-3, 1, 0));
        offsets.add(new BlockPos(1, 1, 1));
        offsets.add(new BlockPos(-1, 1, 1));
        offsets.add(new BlockPos(1, 1, -1));
        offsets.add(new BlockPos(-1, 1, -1));
        return new Footprint(List.copyOf(offsets));
    }

    private static BlockPos mapNormalCompressorOffset(Direction facing, BlockPos offset) {
        return switch (facing) {
            case SOUTH -> new BlockPos(offset.getX(), offset.getY(), offset.getZ());
            case EAST -> new BlockPos(offset.getZ(), offset.getY(), -offset.getX());
            case NORTH -> new BlockPos(-offset.getX(), offset.getY(), -offset.getZ());
            case WEST -> new BlockPos(-offset.getZ(), offset.getY(), offset.getX());
            default -> throw new IllegalArgumentException("Compressor facing must be horizontal: " + facing);
        };
    }

    private static BlockPos mapCompactCompressorOffset(Direction facing, BlockPos offset) {
        return switch (facing) {
            case SOUTH -> new BlockPos(offset.getX(), offset.getY(), offset.getZ());
            case EAST -> new BlockPos(offset.getZ(), offset.getY(), -offset.getX());
            case NORTH -> new BlockPos(-offset.getX(), offset.getY(), -offset.getZ());
            case WEST -> new BlockPos(-offset.getZ(), offset.getY(), offset.getX());
            default -> throw new IllegalArgumentException("Compact compressor facing must be horizontal: " + facing);
        };
    }
}
