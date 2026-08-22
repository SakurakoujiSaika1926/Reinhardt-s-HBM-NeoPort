package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.HeaterBlockEntity;
import com.reinhardt.hbm.item.ScrewdriverItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.MenuProvider;

import javax.annotation.Nullable;

public class HeaterBlock extends LargeMachineBlock implements EntityBlock {
    private final HeaterBlockEntity.Kind kind;
    @Nullable
    private final Footprint cleanupFootprint;

    public HeaterBlock(Properties properties, Footprint footprint, VoxelShape shape, HeaterBlockEntity.Kind kind) {
        this(properties, footprint, null, shape, kind);
    }

    public HeaterBlock(Properties properties, Footprint footprint, @Nullable Footprint cleanupFootprint, VoxelShape shape, HeaterBlockEntity.Kind kind) {
        super(properties, footprint, shape, RotationBasis.MODERN_NORTH);
        this.kind = kind;
        this.cleanupFootprint = cleanupFootprint;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HeaterBlockEntity(pos, state, this.kind);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide && this.cleanupFootprint != null) {
            removeDummies(level, pos, state.getValue(FACING), this.cleanupFootprint);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof HeaterBlockEntity heater) {
            if (heater.hasMenu() && player instanceof ServerPlayer serverPlayer && heater instanceof MenuProvider menuProvider) {
                serverPlayer.openMenu(menuProvider, buffer -> {
                    buffer.writeBlockPos(pos);
                    buffer.writeVarInt(heater.kind().ordinal());
                });
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        if (ScrewdriverItem.isScrewdriver(stack)) {
            return ScrewdriverItem.useItemOnHeater(stack, level, player, hand, pos);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return (tickerLevel, pos, tickerState, blockEntity) -> {
            if (blockEntity instanceof HeaterBlockEntity heater) {
                HeaterBlockEntity.tick(tickerLevel, pos, tickerState, heater);
            }
        };
    }
}

