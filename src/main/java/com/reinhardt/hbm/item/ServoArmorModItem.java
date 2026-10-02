package com.reinhardt.hbm.item;

import com.reinhardt.hbm.util.ArmorModHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** 1.7.10 servo and Desh-servo effects, scoped to their actual armor piece. */
public final class ServoArmorModItem extends ArmorModItem {
    private final boolean desh;

    public ServoArmorModItem(Item.Properties properties, boolean desh) {
        super(properties, ArmorModHandler.SERVOS, false, true, true, false);
        this.desh = desh;
    }

    @Override
    public void tickArmor(Player player, ItemStack armor) {
        if (!(armor.getItem() instanceof ArmorItem armorItem)) {
            return;
        }
        if (armorItem.getType() == ArmorItem.Type.CHESTPLATE) {
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 60, this.desh ? 2 : 0, false, false));
        } else if (armorItem.getType() == ArmorItem.Type.LEGGINGS) {
            player.addEffect(new MobEffectInstance(MobEffects.JUMP, 60, this.desh ? 2 : 1, false, false));
        }
    }

    @Override
    public double movementMultiplier(ItemStack armor) {
        return armor.getItem() instanceof ArmorItem armorItem && armorItem.getType() == ArmorItem.Type.LEGGINGS
                ? (this.desh ? 1.5D : 1.25D)
                : 1.0D;
    }

    @Override
    public double attackDamageMultiplier(ItemStack armor) {
        return armor.getItem() instanceof ArmorItem armorItem && armorItem.getType() == ArmorItem.Type.CHESTPLATE
                ? (this.desh ? 2.5D : 1.5D)
                : 1.0D;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(this.desh
                        ? "tooltip.reinhardtshbm.armor_mod.servo_desh"
                        : "tooltip.reinhardtshbm.armor_mod.servo")
                .withStyle(ChatFormatting.DARK_PURPLE));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
