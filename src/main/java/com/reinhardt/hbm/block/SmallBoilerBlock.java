package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.MachineInventory;
import com.reinhardt.hbm.blockentity.SmallBoilerBlockEntity;
import com.reinhardt.hbm.blockentity.SmallElectricBoilerBlockEntity;
import com.reinhardt.hbm.item.FluidIdentifierItem;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class SmallBoilerBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    private final Kind kind;

    public SmallBoilerBlock(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(LIT, false));
    }

    public Kind kind() {
        return this.kind;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if (this.kind == Kind.ELECTRIC) {
            return new SmallElectricBoilerBlockEntity(pos, state);
        }
        return new SmallBoilerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        BlockEntityType<?> expectedType = this.kind == Kind.ELECTRIC
                ? HbmBlockEntities.SMALL_ELECTRIC_BOILER.get()
                : HbmBlockEntities.SMALL_BOILER.get();
        boolean legacyElectricType = this.kind == Kind.ELECTRIC && blockEntityType == HbmBlockEntities.SMALL_BOILER.get();
        if (level.isClientSide || (blockEntityType != expectedType && !legacyElectricType)) {
            return null;
        }
        return (tickerLevel, pos, tickerState, blockEntity) -> {
            SmallBoilerBlockEntity boiler = (SmallBoilerBlockEntity) blockEntity;
            if (this.kind == Kind.ELECTRIC && !(boiler instanceof SmallElectricBoilerBlockEntity)) {
                SmallElectricBoilerBlockEntity replacement = new SmallElectricBoilerBlockEntity(pos, tickerState);
                boiler.copyMachineStateTo(replacement, tickerLevel.registryAccess());
                tickerLevel.setBlockEntity(replacement);
                boiler = replacement;
            }
            SmallBoilerBlockEntity.tick(tickerLevel, pos, tickerState, boiler);
        };
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
        if (stack.getItem() instanceof FluidIdentifierItem) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof SmallBoilerBlockEntity boiler) {
                boiler.setConfiguredInput(FluidIdentifierItem.primary(stack));
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
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
        if (!state.is(oldState.getBlock()) && this.kind == Kind.ELECTRIC) {
            PowerNetworkManager.markDirty(level);
            refreshElectricConnector(level, pos);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof MachineInventory inventory) {
                inventory.dropContents(level, pos);
            }
            if (this.kind == Kind.ELECTRIC) {
                PowerNetworkManager.markDirty(level);
                refreshElectricConnector(level, pos);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    private void refreshElectricConnector(Level level, BlockPos pos) {
        level.updateNeighborsAt(pos, this);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    public enum Kind {
        OLD,
        ELECTRIC
    }
}
