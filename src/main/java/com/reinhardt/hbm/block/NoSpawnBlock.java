package com.reinhardt.hbm.block;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

/** 1.7.10 BlockNoSpawn tooltip companion; spawn blocking is set in Properties. */
public final class NoSpawnBlock extends Block {
    public NoSpawnBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tile.nospawn").withStyle(ChatFormatting.RED));
    }
}
