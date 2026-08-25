package com.reinhardt.hbm.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

/** The sole ItemBlockLore entry for the 1.7.10 meteor battery. */
public final class MeteorBatteryBlockItem extends BlockItem {
    public MeteorBatteryBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.meteor_battery").withStyle(ChatFormatting.DARK_GRAY));
    }
}
