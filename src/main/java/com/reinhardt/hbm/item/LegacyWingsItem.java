package com.reinhardt.hbm.item;

import com.reinhardt.hbm.util.ArmorModHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** The limp and powered wing armor modifiers from WingsMurk. */
public final class LegacyWingsItem extends ArmorModItem {
    private final boolean powered;

    public LegacyWingsItem(Properties properties, boolean powered) {
        super(properties.stacksTo(1), ArmorModHandler.PLATE_ONLY, false, true, false, false);
        this.powered = powered;
    }

    @Override
    public void tickArmor(Player player, ItemStack armor) {
        if (player.onGround()) return;
        ItemStack installed = ArmorModHandler.pryMods(armor, player.registryAccess())[ArmorModHandler.PLATE_ONLY];
        if (installed.isEmpty()) return;
        var velocity = player.getDeltaMovement();
        if (!powered) {
            if (velocity.y < -0.4D) player.setDeltaMovement(velocity.x, -0.4D, velocity.z);
            player.fallDistance = 0.0F;
            return;
        }
        if (player.isCrouching()) {
            player.setDeltaMovement(velocity.x, Math.max(-0.1D, velocity.y + 0.2D), velocity.z);
        } else {
            player.setDeltaMovement(velocity.x, Math.min(0.8D, velocity.y + 0.2D), velocity.z);
        }
        player.fallDistance = 0.0F;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.wings." + (powered ? "powered" : "limp"))
                .withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
