package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.StirlingGeneratorBlockEntity;
import com.reinhardt.hbm.item.StirlingGeneratorBlockItem;
import com.reinhardt.hbm.power.PowerNetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
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
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

public class StirlingGeneratorBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final LargeMachineBlock.Footprint FOOTPRINT = LargeMachineBlock.Footprint.centered(1, 2, 1);

    public StirlingGeneratorBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        if (!LargeMachineBlock.canPlaceFootprint(context, facing, FOOTPRINT)) {
            return null;
        }
        return this.defaultBlockState().setValue(FACING, facing);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.LivingEntity placer, net.minecraft.world.item.ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            LargeMachineBlock.placeDummies(level, pos, state.getValue(FACING), FOOTPRINT);
            LargeMachineBlock.pushEntitiesOutOfFootprint(level, pos, state.getValue(FACING), FOOTPRINT, placer);
            if (level.getBlockEntity(pos) instanceof StirlingGeneratorBlockEntity stirling) {
                stirling.setHasCog(StirlingGeneratorBlockItem.hasCog(stack));
            }
        }
    }

    @Override
    public List<net.minecraft.world.item.ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (params.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY)
                instanceof StirlingGeneratorBlockEntity stirling && !stirling.hasCog()) {
            net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(this);
            StirlingGeneratorBlockItem.setHasCog(stack, false);
            return List.of(stack);
        }
        return super.getDrops(state, params);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StirlingGeneratorBlockEntity(pos, state);
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
        if (level.getBlockEntity(pos) instanceof StirlingGeneratorBlockEntity stirling
                && stirling.tryInsertCog(player, hand, stack, pos)) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return (tickerLevel, pos, tickerState, blockEntity) -> {
            if (blockEntity instanceof StirlingGeneratorBlockEntity stirling) {
                StirlingGeneratorBlockEntity.tick(tickerLevel, pos, tickerState, stirling);
            }
        };
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!state.is(oldState.getBlock())) {
            PowerNetworkManager.markDirty(level);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            if (!level.isClientSide) {
                LargeMachineBlock.removeDummies(level, pos, state.getValue(FACING), FOOTPRINT);
            }
            PowerNetworkManager.markDirty(level);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
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
