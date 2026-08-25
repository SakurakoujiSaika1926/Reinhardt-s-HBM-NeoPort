package com.reinhardt.hbm.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Peas despawn nearby duck entities, matching ItemPeas. */
public final class LegacyPeasItem extends Item {
    public LegacyPeasItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            player.displayClientMessage(Component.translatable("message.reinhardtshbm.peas.used"), true);
            if (!player.isCreative()) stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
