package com.reinhardt.hbm.item;

import com.reinhardt.hbm.client.screen.RttyPagerScreen;
import com.reinhardt.hbm.network.PlayerInformPayload;
import com.reinhardt.hbm.rtty.HbmRttySystem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/** Direct port of ItemRTTYPager's channel, notification and failsafe behavior. */
public final class LegacyRttyPagerItem extends Item {
    public static final String KEY_CHANNEL = "chan";
    public static final int MAX_CHANNEL_LENGTH = 10;
    private static final int INFORM_LINE_BASE = 4_000;

    public LegacyRttyPagerItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide && FMLEnvironment.dist.isClient()) {
            RttyPagerScreen.open(stack, hand);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean selected) {
        if (level.isClientSide || !(entity instanceof ServerPlayer player)) {
            return;
        }

        String channel = channel(stack);
        if (channel.isEmpty()) {
            return;
        }

        HbmRttySystem.Channel signal = HbmRttySystem.listen(level, channel);
        if (signal == null || signal.timestamp() < level.getGameTime() - 1L) {
            return;
        }

        if ("selfdestruct".equals(signal.signal())) {
            level.explode(player, player.getX(), player.getY() + player.getBbHeight() * 0.5D, player.getZ(),
                    5.0F, false, Level.ExplosionInteraction.BLOCK);
            stack.shrink(1);
            return;
        }

        int alive = player.tickCount % 1_000;
        String message = "[ " + channel + " (" + alive + ") ] " + signal.signal();
        PacketDistributor.sendToPlayer(player, new PlayerInformPayload(INFORM_LINE_BASE + slotId, message, 0xFFFF55, 5_000));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String channel = channel(stack);
        if (channel.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.reinhardtshbm.rtty_pager.no_channel").withStyle(ChatFormatting.RED));
        } else {
            tooltip.add(Component.translatable("tooltip.reinhardtshbm.rtty_pager.channel", channel).withStyle(ChatFormatting.YELLOW));
        }
    }

    public static String channel(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString(KEY_CHANNEL);
    }

    public static void setChannel(ItemStack stack, String channel) {
        String normalized = channel == null ? "" : channel.trim();
        if (normalized.length() > MAX_CHANNEL_LENGTH) {
            normalized = normalized.substring(0, MAX_CHANNEL_LENGTH);
        }

        CompoundTag data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (normalized.isEmpty()) {
            data.remove(KEY_CHANNEL);
        } else {
            data.putString(KEY_CHANNEL, normalized);
        }
        if (data.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        }
    }
}
