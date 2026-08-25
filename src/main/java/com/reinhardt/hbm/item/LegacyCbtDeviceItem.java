package com.reinhardt.hbm.item;

import com.reinhardt.hbm.registry.HbmMobEffects;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Direct 1.7.10 ItemSyringe behavior for the CBT device. */
public final class LegacyCbtDeviceItem extends Item {
    public LegacyCbtDeviceItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            player.addEffect(new MobEffectInstance(HbmMobEffects.BANG, 30, 0));
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
