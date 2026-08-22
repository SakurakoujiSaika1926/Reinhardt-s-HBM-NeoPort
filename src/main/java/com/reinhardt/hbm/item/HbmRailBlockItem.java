package com.reinhardt.hbm.item;

import com.reinhardt.hbm.block.HbmRailBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class HbmRailBlockItem extends BlockItem {
    public HbmRailBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (!(this.getBlock() instanceof HbmRailBlock rail)) {
            return;
        }

        int speedPercent = Math.round((rail.maxSpeed() / HbmRailBlock.VANILLA_SPEED) * 100.0F);
        if (speedPercent != 100) {
            tooltip.add(Component.translatable("tooltip.reinhardtshbm.rail.speed", speedPercent)
                    .withStyle(speedPercent > 100 ? ChatFormatting.BLUE : ChatFormatting.RED));
        }
        if (!rail.flexible()) {
            tooltip.add(Component.translatable("tooltip.reinhardtshbm.rail.no_turns").withStyle(ChatFormatting.RED));
        }
        if (!rail.slopable()) {
            tooltip.add(Component.translatable("tooltip.reinhardtshbm.rail.no_slopes").withStyle(ChatFormatting.RED));
        }
    }
}
