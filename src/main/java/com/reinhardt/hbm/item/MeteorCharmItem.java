package com.reinhardt.hbm.item;

import com.reinhardt.hbm.registry.HbmDamageTypes;
import com.reinhardt.hbm.util.ArmorModHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class MeteorCharmItem extends ArmorModItem {
    private final Kind kind;

    public MeteorCharmItem(Properties properties, Kind kind) {
        super(properties, ArmorModHandler.HELMET_ONLY, true, false, false, false);
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
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public float modifyArmorDamage(Player player, ItemStack armor, DamageSource source, float amount) {
        if (!source.is(HbmDamageTypes.BROADCAST)) {
            return amount;
        }
        return this.kind == Kind.PROTECTION ? amount * 0.5F : 0.0F;
    }

    public Kind kind() {
        return this.kind;
    }

    public enum Kind {
        METEOR,
        PROTECTION
    }
}
