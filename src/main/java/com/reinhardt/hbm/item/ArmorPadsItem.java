package com.reinhardt.hbm.item;

import com.reinhardt.hbm.util.ArmorModHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Direct port of the three 1.7.10 ItemModPads variants. */
public final class ArmorPadsItem extends ArmorModItem {
    private final float fallDamageMultiplier;
    private final boolean staticCharging;

    public ArmorPadsItem(Properties properties, float fallDamageMultiplier, boolean staticCharging) {
        super(properties, ArmorModHandler.BOOTS_ONLY, false, false, false, true);
        this.fallDamageMultiplier = fallDamageMultiplier;
        this.staticCharging = staticCharging;
    }

    @Override
    public float modifyArmorDamage(Player player, ItemStack armor, DamageSource source, float amount) {
        return source.is(DamageTypes.FALL) ? amount * this.fallDamageMultiplier : amount;
    }

    @Override
    public void tickArmor(Player player, ItemStack armor) {
        if (!this.staticCharging
                || !player.onGround()
                || player.getDeltaMovement().horizontalDistanceSqr() <= 1.0E-6D
                || !ArmorFSBItem.hasFSBArmorIgnoringCharge(player)) {
            return;
        }

        for (ItemStack equipped : player.getArmorSlots()) {
            if (equipped.getItem() instanceof PoweredArmorFSBItem powered) {
                long charge = powered.staticRechargeAmount();
                if (charge > 0L) {
                    powered.hbmSetCharge(equipped, Math.min(powered.hbmCapacity(equipped), powered.hbmCharge(equipped) + charge));
                }
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int reduction = Math.round((1.0F - this.fallDamageMultiplier) * 100.0F);
        if (reduction > 0) {
            tooltip.add(Component.translatable("tooltip.reinhardtshbm.armor.fall_reduction", reduction).withStyle(ChatFormatting.RED));
        }
        if (this.staticCharging) {
            tooltip.add(Component.translatable("tooltip.reinhardtshbm.armor.static_charge").withStyle(ChatFormatting.DARK_PURPLE));
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
