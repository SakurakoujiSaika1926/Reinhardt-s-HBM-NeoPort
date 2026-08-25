package com.reinhardt.hbm.item;

import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Direct port of the durable 1.7.10 balefire-and-steel igniter. */
public final class LegacyBalefireMatchItem extends Item {
    public LegacyBalefireMatchItem(Properties properties) {
        super(properties.stacksTo(1).durability(256));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockPos target = context.getClickedPos().relative(context.getClickedFace());
        ItemStack stack = context.getItemInHand();
        if (player == null || !player.mayUseItemAt(target, context.getClickedFace(), stack)) {
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide && level.isEmptyBlock(target)) {
            level.playSound(null, target, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS,
                    1.0F, level.random.nextFloat() * 0.4F + 0.8F);
            level.setBlock(target, HbmBlocks.BALEFIRE.get().defaultBlockState(), 11);
        }
        if (player != null) {
            stack.hurtAndBreak(1, player, player.getEquipmentSlotForItem(stack));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
