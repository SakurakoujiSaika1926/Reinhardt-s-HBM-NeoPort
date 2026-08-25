package com.reinhardt.hbm.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** 1.7.10 F.L.E.I.J.A. and Solinium bomb construction component. */
public final class BombPartItem extends Item {
    private final Target target;

    public BombPartItem(Target target, Rarity rarity) {
        super(new Properties().stacksTo(1).rarity(rarity));
        this.target = target;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.bomb_part.used_in").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(this.target.blockTranslationKey()).withStyle(ChatFormatting.DARK_GRAY));
    }

    public enum Target {
        FLEIJA("block.reinhardtshbm.nuke_fleija"),
        SOLINIUM("block.reinhardtshbm.nuke_solinium");

        private final String blockTranslationKey;

        Target(String blockTranslationKey) {
            this.blockTranslationKey = blockTranslationKey;
        }

        public String blockTranslationKey() {
            return blockTranslationKey;
        }
    }
}
