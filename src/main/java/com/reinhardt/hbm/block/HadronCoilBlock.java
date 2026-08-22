package com.reinhardt.hbm.block;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.Locale;

public class HadronCoilBlock extends Block {
    private final int coilStrength;

    public HadronCoilBlock(Properties properties, int coilStrength) {
        super(properties);
        this.coilStrength = coilStrength;
    }

    public int coilStrength() {
        return this.coilStrength;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("info.coil")
                .append(": " + String.format(Locale.US, "%,d", this.coilStrength))
                .withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
