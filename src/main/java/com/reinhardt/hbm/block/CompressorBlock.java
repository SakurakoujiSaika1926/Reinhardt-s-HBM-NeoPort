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
        super(properties, footprint(kind), shape, RotationBasis.HBM_LEGACY_SOUTH);
        this.kind = kind;
    }

    public CompressorBlockEntity.Kind kind() {
        return this.kind;
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
        return kind == CompressorBlockEntity.Kind.COMPACT ? compactFootprint() : normalFootprint();
    }

    private static Footprint normalFootprint() {
        LinkedHashSet<BlockPos> offsets = new LinkedHashSet<>();
        addLegacyBox(offsets, 2, 0, 1, 2, 1, 1, BlockPos.ZERO);

        BlockPos shiftedCore = new BlockPos(0, 0, 2);
        addLegacyBox(offsets, 3, -3, 1, 1, 1, 1, shiftedCore);
        addLegacyBox(offsets, 8, -4, 0, 0, 1, 1, shiftedCore);

        offsets.add(new BlockPos(0, 0, 1));
        offsets.add(new BlockPos(1, 0, 2));
        offsets.add(new BlockPos(-1, 0, 2));
        return new Footprint(List.copyOf(offsets));
    }

    private static Footprint compactFootprint() {
        LinkedHashSet<BlockPos> offsets = new LinkedHashSet<>();
        addLegacyBox(offsets, 2, 0, 1, 1, 3, 3, BlockPos.ZERO);

        offsets.add(new BlockPos(3, 1, 0));
        offsets.add(new BlockPos(-3, 1, 0));
        offsets.add(new BlockPos(1, 1, 1));
        offsets.add(new BlockPos(-1, 1, 1));
        offsets.add(new BlockPos(1, 1, -1));
        offsets.add(new BlockPos(-1, 1, -1));
        return new Footprint(List.copyOf(offsets));
    }

    private static void addLegacyBox(LinkedHashSet<BlockPos> offsets, int up, int down, int north, int south, int west, int east, BlockPos origin) {
        for (int y = -down; y <= up; y++) {
            for (int x = -west; x <= east; x++) {
                for (int z = -north; z <= south; z++) {
                    offsets.add(origin.offset(x, y, z));
                }
            }
        }
    }
}
