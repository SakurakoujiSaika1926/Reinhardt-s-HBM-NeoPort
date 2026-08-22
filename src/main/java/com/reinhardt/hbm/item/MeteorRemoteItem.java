package com.reinhardt.hbm.item;

import com.reinhardt.hbm.event.MeteorEvents;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class MeteorRemoteItem extends Item {
    public MeteorRemoteItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.meteor_remote").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (!player.getAbilities().instabuild) {
            stack.hurtAndBreak(1, player, usedHand == InteractionHand.MAIN_HAND
                    ? net.minecraft.world.entity.EquipmentSlot.MAINHAND
                    : net.minecraft.world.entity.EquipmentSlot.OFFHAND);
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            MeteorEvents.spawnMeteorAtPlayer(serverPlayer, false);
            player.sendSystemMessage(Component.translatable("message.reinhardtshbm.meteor_remote.watch_head"));
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), HbmSoundEvents.TECH_BLEEP.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        player.swing(usedHand);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
