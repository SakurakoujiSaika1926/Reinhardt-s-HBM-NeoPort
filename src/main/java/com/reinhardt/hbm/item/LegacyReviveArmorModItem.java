package com.reinhardt.hbm.item;

import com.reinhardt.hbm.util.ArmorModHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** One-use and three-use death prevention upgrades from 1.7.10. */
public final class LegacyReviveArmorModItem extends ArmorModItem {
    public LegacyReviveArmorModItem(Item.Properties properties, int uses) {
        super(properties.durability(uses), ArmorModHandler.EXTRA, false, false, true, false);
    }

    public static boolean tryRevive(Player player) {
        for (ItemStack armor : player.getArmorSlots()) {
            ItemStack revive = ArmorModHandler.pryMods(armor, player.registryAccess())[ArmorModHandler.EXTRA];
            if (!(revive.getItem() instanceof LegacyReviveArmorModItem)) {
                continue;
            }

            revive.setDamageValue(revive.getDamageValue() + 1);
            if (revive.getDamageValue() >= revive.getMaxDamage()) {
                ArmorModHandler.removeMod(armor, ArmorModHandler.EXTRA);
            } else {
                ArmorModHandler.applyMod(armor, revive, player.registryAccess());
            }
            player.setHealth(player.getMaxHealth());
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 99));
            return true;
        }
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.revive_armor_mod", Math.max(0, stack.getMaxDamage() - stack.getDamageValue()))
                .withStyle(ChatFormatting.GOLD));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
