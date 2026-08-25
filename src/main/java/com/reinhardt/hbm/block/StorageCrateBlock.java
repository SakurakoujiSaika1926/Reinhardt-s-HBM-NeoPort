package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.StorageCrateBlockEntity;
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

import javax.annotation.Nullable;
import java.util.List;

public class StorageCrateBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private final StorageCrateBlockEntity.Kind kind;

    public StorageCrateBlock(Properties properties, StorageCrateBlockEntity.Kind kind) {
        super(properties);
        this.kind = kind;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    public StorageCrateBlockEntity.Kind kind() {
        return this.kind;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof StorageCrateBlockEntity crate) {
            crate.loadFromItem(stack, level.registryAccess());
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StorageCrateBlockEntity(pos, state);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        BlockEntity blockEntity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (blockEntity instanceof StorageCrateBlockEntity crate && crate.getLevel() != null) {
            ItemStack stack = new ItemStack(this);
            crate.saveToItem(stack, crate.getLevel().registryAccess());
            return List.of(stack);
        }
        return super.getDrops(state, params);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (blockEntityType != HbmBlockEntities.STORAGE_CRATE.get()) {
            return null;
        }
        return (tickerLevel, pos, tickerState, blockEntity) -> {
            if (blockEntity instanceof StorageCrateBlockEntity crate) {
                StorageCrateBlockEntity.tick(tickerLevel, pos, tickerState, crate);
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
        if (blockEntity instanceof StorageCrateBlockEntity crate && !crate.canAccess(player)) {
            return InteractionResult.CONSUME;
        }
        if (blockEntity instanceof MenuProvider menuProvider && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(menuProvider, buffer -> buffer.writeBlockPos(pos));
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
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
