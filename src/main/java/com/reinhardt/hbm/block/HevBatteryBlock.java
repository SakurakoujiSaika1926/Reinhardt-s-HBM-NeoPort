package com.reinhardt.hbm.block;

import com.reinhardt.hbm.item.ArmorFSBItem;
import com.reinhardt.hbm.item.HbmChargeableItem;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Direct port of the one-use 1.7.10 HEV suit battery block. */
public final class HevBatteryBlock extends Block {
    private static final long CHARGE = 150_000L;
    private static final VoxelShape SHAPE = box(6.0D, 0.0D, 6.0D, 10.0D, 6.0D, 10.0D);

    public HevBatteryBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!ArmorFSBItem.hasFSBArmorIgnoringCharge(player)) {
            return InteractionResult.CONSUME;
        }

        boolean charged = false;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!(stack.getItem() instanceof HbmChargeableItem battery)) {
                continue;
            }
            long current = battery.hbmCharge(stack);
            long target = Math.min(battery.hbmCapacity(stack), current + CHARGE);
            if (target > current) {
                battery.hbmSetCharge(stack, target);
                charged = true;
            }
        }

        if (charged) {
            level.playSound(null, pos, HbmSoundEvents.SUIT_BATTERY.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            level.destroyBlock(pos, false);
        }
        return InteractionResult.CONSUME;
    }
}
