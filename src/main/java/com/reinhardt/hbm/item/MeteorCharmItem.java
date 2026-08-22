package com.reinhardt.hbm.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class MeteorCharmItem extends Item {
    private final Kind kind;

    public MeteorCharmItem(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.meteor_charm.blessed").withStyle(ChatFormatting.AQUA));
        if (this.kind == Kind.PROTECTION) {
            tooltip.add(Component.translatable("tooltip.reinhardtshbm.protection_charm.divert").withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.translatable("tooltip.reinhardtshbm.protection_charm.safe").withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.translatable("tooltip.reinhardtshbm.protection_charm.broadcast").withStyle(ChatFormatting.AQUA));
        } else {
            tooltip.add(Component.translatable("tooltip.reinhardtshbm.meteor_charm.disable").withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.translatable("tooltip.reinhardtshbm.meteor_charm.broadcast").withStyle(ChatFormatting.AQUA));
        }
    }

    public Kind kind() {
        return this.kind;
    }

    public enum Kind {
        METEOR,
        PROTECTION
    }
}
