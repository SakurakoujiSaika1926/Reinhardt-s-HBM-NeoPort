package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.FluidTankBlockEntity;
import com.reinhardt.hbm.item.FluidIdentifierItem;
import com.reinhardt.hbm.item.BlowtorchItem;
import com.reinhardt.hbm.item.ScrewdriverItem;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.List;

public class FluidTankBlock extends LargeMachineBlock implements EntityBlock {
    public FluidTankBlock(Properties properties, Footprint footprint, VoxelShape shape) {
        super(properties, footprint, shape, RotationBasis.MODERN_NORTH);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluidTankBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof FluidTankBlockEntity tank) {
            tank.loadFromItem(stack);
        }
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        BlockEntity blockEntity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (blockEntity instanceof FluidTankBlockEntity tank) {
            ItemStack stack = new ItemStack(this);
            tank.saveToItem(stack);
            return List.of(stack);
        }
        return super.getDrops(state, params);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide || blockEntityType != HbmBlockEntities.FLUID_TANK.get()) {
            return null;
        }
        return (tickerLevel, pos, tickerState, blockEntity) -> FluidTankBlockEntity.tick(tickerLevel, pos, tickerState, (FluidTankBlockEntity) blockEntity);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.getItem() instanceof BlowtorchItem blowtorch && level.getBlockEntity(pos) instanceof FluidTankBlockEntity tank) {
            if (tank.isDamaged()) {
                if (!level.isClientSide && blowtorch.canTorch(stack) && tank.repair(player)) {
                    blowtorch.consumeTorchFuel(stack);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        if (!(stack.getItem() instanceof FluidIdentifierItem) || !player.isShiftKeyDown()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof FluidTankBlockEntity tank) {
            tank.setType(FluidIdentifierItem.primary(stack));
            level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.25F, 1.2F);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onBlockExploded(BlockState state, Level level, BlockPos pos, Explosion explosion) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof FluidTankBlockEntity tank) {
            tank.handleExplosion(explosion);
            return;
        }
        super.onBlockExploded(state, level, pos, explosion);
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
}

