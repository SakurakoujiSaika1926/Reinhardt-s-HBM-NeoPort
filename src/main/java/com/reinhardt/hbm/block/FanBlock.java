package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.FanBlockEntity;
import com.reinhardt.hbm.item.ScrewdriverItem;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

public final class FanBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public FanBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FanBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != HbmBlockEntities.FAN.get()) {
            return null;
        }
        return (tickerLevel, tickerPos, tickerState, blockEntity) ->
                FanBlockEntity.tick(tickerLevel, tickerPos, tickerState, (FanBlockEntity) blockEntity);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        FanBlockEntity fan = level.getBlockEntity(pos) instanceof FanBlockEntity value ? value : null;
        if (fan == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (ScrewdriverItem.isScrewdriver(stack)) {
            level.setBlock(pos, state.setValue(FACING, oppositeAxis(state.getValue(FACING))), Block.UPDATE_ALL);
            ScrewdriverItem.damageTool(stack, level, player, hand);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (ScrewdriverItem.isHandDrill(stack)) {
            fan.setFalloff(!fan.falloff());
            ScrewdriverItem.damageTool(stack, level, player, hand);
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable(
                        "message.reinhardtshbm.fan." + (fan.falloff() ? "falloff_on" : "falloff_off")), false);
                level.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.5F, 0.5F);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (ScrewdriverItem.isDefuser(stack)) {
            fan.setSuck(!fan.suck());
            ScrewdriverItem.damageTool(stack, level, player, hand);
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable(
                        "message.reinhardtshbm.fan." + (fan.suck() ? "suck_on" : "suck_off")), false);
                level.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.5F, 0.5F);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    private static Direction oppositeAxis(Direction direction) {
        return switch (direction) {
            case DOWN -> Direction.UP;
            case UP -> Direction.DOWN;
            case NORTH -> Direction.SOUTH;
            case SOUTH -> Direction.NORTH;
            case WEST -> Direction.EAST;
            case EAST -> Direction.WEST;
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
