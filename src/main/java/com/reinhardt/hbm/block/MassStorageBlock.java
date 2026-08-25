package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.MassStorageBlockEntity;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class MassStorageBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public enum Kind {
        IRON(10_000), DESH(100_000), RESISTANT(1_000_000), WOOD(100);

        private final int capacity;

        Kind(int capacity) {
            this.capacity = capacity;
        }

        public int capacity() {
            return capacity;
        }
    }

    private final Kind kind;

    public MassStorageBlock(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    public Kind kind() {
        return kind;
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof MassStorageBlockEntity storage) {
            storage.loadFromItem(stack, level.registryAccess());
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MassStorageBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != HbmBlockEntities.MASS_STORAGE.get()) {
            return null;
        }
        return (tickerLevel, tickerPos, tickerState, entity) -> {
            if (entity instanceof MassStorageBlockEntity storage) {
                MassStorageBlockEntity.tick(tickerLevel, tickerPos, tickerState, storage);
            }
        };
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        BlockEntity entity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (entity instanceof MassStorageBlockEntity storage && storage.getLevel() != null) {
            ItemStack stack = new ItemStack(this);
            storage.saveToItem(stack, storage.getLevel().registryAccess());
            return List.of(stack);
        }
        return super.getDrops(state, params);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof MassStorageBlockEntity storage && !storage.canAccess(player)) {
            return InteractionResult.CONSUME;
        }
        if (level.getBlockEntity(pos) instanceof MenuProvider provider && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(provider, buffer -> buffer.writeBlockPos(pos));
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof MassStorageBlockEntity storage ? storage.redstone() : 0;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
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
        builder.add(FACING);
    }
}
