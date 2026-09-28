package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.GeothermalHeatExchangerBlockEntity;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.item.FluidIdentifierItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
    @Override
    protected BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.setValue(FACING, mirror.getRotation(state.getValue(FACING)).rotate(state.getValue(FACING)));
    }

    public static final Footprint FOOTPRINT = Footprint.box(-1, 1, 0, 11, -1, 1);

    public GeothermalHeatExchangerBlock(Properties properties) {
        // No 1.7.10 geothermal block exists to establish an old basis. This private table only
        // preserves the pre-migration MODERN_NORTH behavior; it is not claimed as legacy parity.
        super(properties, FOOTPRINT, Shapes.block(), GeothermalHeatExchangerBlock::mapKnownGeothermalOffset);
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        if (!this.canPlaceMachineFootprintAt(context.getLevel(), context.getClickedPos(), Direction.NORTH, context)) {
            return null;
        }
        return defaultBlockState();
    }

    private static BlockPos mapKnownGeothermalOffset(Direction facing, BlockPos offset) {
        int x = offset.getX();
        int y = offset.getY();
        int z = offset.getZ();
        return switch (facing) {
            case NORTH -> new BlockPos(x, y, z);
            case EAST -> new BlockPos(-z, y, x);
            case SOUTH -> new BlockPos(-x, y, -z);
            case WEST -> new BlockPos(z, y, -x);
            case UP, DOWN -> throw new IllegalArgumentException("Geothermal heat exchanger requires a horizontal facing: " + facing);
        };
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
