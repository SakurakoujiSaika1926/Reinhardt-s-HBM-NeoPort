package com.reinhardt.hbm.item;

import com.reinhardt.hbm.client.screen.RttyDesignatorScreen;
import com.reinhardt.hbm.rtty.HbmRttySystem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.fml.loading.FMLEnvironment;

import java.util.List;

/**
 * Modern port of the upstream Redstone-over-Radio designator.
 * <p>
 * Sneak-use opens the channel editor. A normal use ray-casts up to the
 * original 300-block range and broadcasts the position immediately outside
 * the selected face as {@code x:y:z} on that channel.
 */
public final class RttyDesignatorItem extends Item {
    public RttyDesignatorItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (level.isClientSide && FMLEnvironment.dist.isClient()) {
                RttyDesignatorScreen.open(stack, hand);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        if (!level.isClientSide) {
            String channel = LegacyRttyPagerItem.channel(stack);
            if (channel.isEmpty()) {
                return InteractionResultHolder.pass(stack);
            }

            HitResult hit = player.pick(300.0D, 1.0F, false);
            if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
                return InteractionResultHolder.pass(stack);
            }

            BlockPos target = blockHit.getBlockPos().relative(blockHit.getDirection());
            HbmRttySystem.broadcast(level, channel,
                    target.getX() + ":" + target.getY() + ":" + target.getZ());
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String channel = LegacyRttyPagerItem.channel(stack);
        if (channel.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.reinhardtshbm.rtty_designator.no_channel")
                    .withStyle(ChatFormatting.RED));
        } else {
            tooltip.add(Component.translatable("tooltip.reinhardtshbm.rtty_designator.channel", channel)
                    .withStyle(ChatFormatting.YELLOW));
        }
    }
}
