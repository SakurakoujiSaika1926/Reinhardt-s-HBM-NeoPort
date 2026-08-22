package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.FractionTowerBlockEntity;
import com.reinhardt.hbm.item.FluidIdentifierItem;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
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
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Set;

public class FractionTowerBlock extends LargeMachineBlock implements EntityBlock {
    public static final Footprint FOOTPRINT = createFootprint();

    public FractionTowerBlock(Properties properties, VoxelShape shape) {
        super(properties, FOOTPRINT, shape, RotationBasis.MODERN_NORTH);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = this.defaultBlockState().setValue(FACING, Direction.NORTH);
        return LargeMachineBlock.canPlaceFootprint(context, state, FOOTPRINT) ? state : null;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FractionTowerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (blockEntityType != HbmBlockEntities.FRACTION_TOWER.get()) {
            return null;
        }
        return (tickerLevel, pos, tickerState, blockEntity) -> FractionTowerBlockEntity.tick(
                tickerLevel,
                pos,
                tickerState,
                (FractionTowerBlockEntity) blockEntity
        );
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof FractionTowerBlockEntity tower) {
            printTowerInfo(player, pos, tower);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
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
        if (!(stack.getItem() instanceof FluidIdentifierItem)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof FractionTowerBlockEntity tower) {
            applyIdentifier(level, pos, player, stack, tower);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    public static void printTowerInfo(Player player, BlockPos pos, FractionTowerBlockEntity tower) {
        player.displayClientMessage(Component.translatable("chat.reinhardtshbm.fractioning.y", pos.getY()), false);
        for (int index = 0; index < FractionTowerBlockEntity.TANK_COUNT; index++) {
            player.displayClientMessage(tower.tankLine(index), false);
        }
    }

    public static void applyIdentifier(Level level, BlockPos pos, Player player, ItemStack stack, FractionTowerBlockEntity tower) {
        if (level.getBlockEntity(pos.below(3)) instanceof FractionTowerBlockEntity) {
            player.displayClientMessage(Component.translatable("chat.reinhardtshbm.fractioning.onlybottom"), false);
            return;
        }

        tower.setInputType(FluidIdentifierItem.primary(stack));
        player.displayClientMessage(Component.translatable(
                "chat.reinhardtshbm.fractioning.changedto",
                Component.translatable(FluidIdentifierItem.primary(stack).translationKey())
        ), false);
    }

    private static Footprint createFootprint() {
        Set<BlockPos> offsets = new LinkedHashSet<>();
        for (int y = 0; y <= 2; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    offsets.add(new BlockPos(x, y, z));
                }
            }
        }
        offsets.add(BlockPos.ZERO);
        return new Footprint(offsets.stream().toList());
    }
}

