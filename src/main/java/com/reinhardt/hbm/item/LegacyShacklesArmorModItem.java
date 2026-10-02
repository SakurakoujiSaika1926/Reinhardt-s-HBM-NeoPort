package com.reinhardt.hbm.item;

import com.reinhardt.hbm.radiation.HbmLivingRadiation;
import com.reinhardt.hbm.util.ArmorModHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.List;

/** Exact modern equivalent of the infinite, radiation-accumulating shackles revive module. */
public final class LegacyShacklesArmorModItem extends ArmorModItem {
    private static final String REVIVE_COUNT_KEY = "shackle_revives";

    public LegacyShacklesArmorModItem(Item.Properties properties) {
        super(properties, ArmorModHandler.EXTRA, false, false, true, false);
    }

    public static boolean tryRevive(Player player) {
        HbmLivingRadiation radiation = HbmLivingRadiation.get(player);
        if (radiation.getRadiation() >= 1000.0F) {
            return false;
        }
        for (ItemStack armor : player.getArmorSlots()) {
            ItemStack shackles = ArmorModHandler.pryMods(armor, player.registryAccess())[ArmorModHandler.EXTRA];
            if (!(shackles.getItem() instanceof LegacyShacklesArmorModItem)) {
                continue;
            }

            CompoundTag tag = shackles.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            int revives = Math.max(0, tag.getInt(REVIVE_COUNT_KEY)) + 1;
            tag.putInt(REVIVE_COUNT_KEY, revives);
            shackles.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            ArmorModHandler.applyMod(armor, shackles, player.registryAccess());

            player.setHealth(player.getMaxHealth());
            radiation.addRadiation((float) revives * (float) revives);
            HbmLivingRadiation.set(player, radiation);
            return true;
        }
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.armor_mod.shackles")
                .withStyle(ChatFormatting.RED));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
