package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.DfcEmitterBlockEntity;
import com.reinhardt.hbm.blockentity.DfcInjectorBlockEntity;
import com.reinhardt.hbm.blockentity.DfcReceiverBlockEntity;
import com.reinhardt.hbm.blockentity.DfcStabilizerBlockEntity;
import com.reinhardt.hbm.blockentity.MachineInventory;
import com.reinhardt.hbm.power.PowerNetworkManager;
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
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

public class DfcComponentBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    private final Kind kind;

    public DfcComponentBlock(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public Kind kind() {
        return this.kind;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return switch (this.kind) {
            case EMITTER -> new DfcEmitterBlockEntity(pos, state);
            case RECEIVER -> new DfcReceiverBlockEntity(pos, state);
            case INJECTOR -> new DfcInjectorBlockEntity(pos, state);
            case STABILIZER -> new DfcStabilizerBlockEntity(pos, state);
        };
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return (tickerLevel, tickerPos, tickerState, blockEntity) -> {
            switch (blockEntity) {
                case DfcEmitterBlockEntity emitter -> DfcEmitterBlockEntity.tick(tickerLevel, tickerPos, tickerState, emitter);
                case DfcReceiverBlockEntity receiver -> DfcReceiverBlockEntity.tick(tickerLevel, tickerPos, tickerState, receiver);
                case DfcInjectorBlockEntity injector -> DfcInjectorBlockEntity.tick(tickerLevel, tickerPos, tickerState, injector);
                case DfcStabilizerBlockEntity stabilizer -> DfcStabilizerBlockEntity.tick(tickerLevel, tickerPos, tickerState, stabilizer);
                default -> {
                }
            }
        };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player.isShiftKeyDown()) {
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
        if (!state.is(oldState.getBlock()) && (this.kind == Kind.EMITTER || this.kind == Kind.RECEIVER || this.kind == Kind.STABILIZER)) {
            PowerNetworkManager.markDirty(level);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            if (level.getBlockEntity(pos) instanceof MachineInventory inventory) {
                inventory.dropContents(level, pos);
            }
            if (this.kind == Kind.EMITTER || this.kind == Kind.RECEIVER || this.kind == Kind.STABILIZER) {
                PowerNetworkManager.markDirty(level);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    public enum Kind {
        EMITTER,
        RECEIVER,
        INJECTOR,
        STABILIZER
    }
}
