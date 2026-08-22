package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.GeothermalHeatExchangerBlockEntity;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.item.FluidIdentifierItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import org.jetbrains.annotations.Nullable;

public final class GeothermalHeatExchangerBlock extends LargeMachineBlock implements EntityBlock {
    public static final Footprint FOOTPRINT = Footprint.box(-1, 1, 0, 11, -1, 1);

    public GeothermalHeatExchangerBlock(Properties properties) {
        super(properties, FOOTPRINT, Shapes.block(), RotationBasis.MODERN_NORTH);
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        if (!canPlaceFootprintAt(context.getLevel(), context.getClickedPos(), net.minecraft.core.Direction.NORTH, FOOTPRINT, context)) {
            return null;
        }
        return defaultBlockState();
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!(stack.getItem() instanceof FluidIdentifierItem)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof GeothermalHeatExchangerBlockEntity exchanger
                && exchanger.applyFluidIdentifier(FluidIdentifierItem.primary(stack))) {
            level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.25F, 1.2F);
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "chat.reinhardtshbm.changed_fluid_type",
                    net.minecraft.network.chat.Component.translatable(FluidIdentifierItem.primary(stack).translationKey())), false);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hitResult) {
        return InteractionResult.PASS;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GeothermalHeatExchangerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == HbmBlockEntities.GEOTHERMAL_HEAT_EXCHANGER.get()
                ? (tickerLevel, pos, tickerState, blockEntity) -> GeothermalHeatExchangerBlockEntity.tick(
                tickerLevel, pos, tickerState, (GeothermalHeatExchangerBlockEntity) blockEntity)
                : null;
    }
}
