package com.reinhardt.hbm.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Modern equivalent of ItemDesingatorManual's persistent coordinate tool. */
public final class LegacyManualDesignatorItem extends LegacyRangeDesignatorItem {
    public LegacyManualDesignatorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player.isShiftKeyDown()) {
            ItemStack stack = player.getItemInHand(hand);
            stack.remove(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.reinhardtshbm.designator_manual.cleared"), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return super.use(level, player, hand);
    }
}
