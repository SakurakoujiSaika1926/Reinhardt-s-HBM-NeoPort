package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.WandLogicBlockEntity;
import com.reinhardt.hbm.item.ScrewdriverItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
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
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class WandLogicBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public WandLogicBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.SOUTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.getBlockEntity(pos) instanceof WandLogicBlockEntity logic) {
            logic.placedRotation = state.getValue(FACING).get3DDataValue();
            logic.sync();
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WandLogicBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return (tickerLevel, pos, tickerState, blockEntity) -> {
            if (blockEntity instanceof WandLogicBlockEntity logic) {
                WandLogicBlockEntity.tick(tickerLevel, pos, tickerState, logic);
            }
        };
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hitResult) {
        if (!player.isShiftKeyDown()
                && stack.getItem() instanceof BlockItem blockItem
                && level.getBlockEntity(pos) instanceof WandLogicBlockEntity logic
                && blockItem.getBlock() != this) {
            if (!level.isClientSide) {
                logic.setDisguise(blockItem.getBlock(), stack.getDamageValue());
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (ScrewdriverItem.isScrewdriver(stack) || ScrewdriverItem.isHandDrill(stack) || ScrewdriverItem.isDefuser(stack)) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof WandLogicBlockEntity logic) {
                if (ScrewdriverItem.isScrewdriver(stack)) {
                    logic.cycleAction(player.isShiftKeyDown());
                } else if (ScrewdriverItem.isHandDrill(stack)) {
                    logic.cycleInteraction(player.isShiftKeyDown());
                } else {
                    logic.cycleCondition(player.isShiftKeyDown());
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
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
