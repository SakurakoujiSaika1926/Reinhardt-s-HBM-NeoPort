package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.HeatBoilerBlockEntity;
import com.reinhardt.hbm.blockentity.IndustrialBoilerBlockEntity;
import com.reinhardt.hbm.item.FluidIdentifierItem;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

public class IndustrialBoilerBlock extends LargeMachineBlock implements EntityBlock {
    public IndustrialBoilerBlock(Properties properties, Footprint footprint, VoxelShape shape) {
        // MachineHeatBoilerIndustrial uses the 1.7.10 SOUTH-basis dummy rotation.
        super(properties, footprint, shape, RotationBasis.HBM_LEGACY_SOUTH);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new IndustrialBoilerBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!(stack.getItem() instanceof FluidIdentifierItem)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof IndustrialBoilerBlockEntity boiler) {
            boiler.setConfiguredInput(FluidIdentifierItem.primary(stack));
            level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.25F, 1.2F);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return InteractionResult.PASS;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (blockEntityType != HbmBlockEntities.INDUSTRIAL_BOILER.get()) {
            return null;
        }
        return (tickerLevel, tickerPos, tickerState, blockEntity) -> HeatBoilerBlockEntity.tick(
                tickerLevel,
                tickerPos,
                tickerState,
                (IndustrialBoilerBlockEntity) blockEntity
        );
    }
}

