package com.reinhardt.hbm.item;

import com.reinhardt.hbm.player.HbmPlayerArmorState;
import com.reinhardt.hbm.util.ArmorModHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Direct port of ItemModLodestone's armor-slot item attraction. */
public final class ArmorMagnetItem extends ArmorModItem {
    private final int range;

    public ArmorMagnetItem(Properties properties, int range) {
        super(properties, ArmorModHandler.EXTRA, true, true, true, true);
        this.range = range;
    }

    @Override
    public void tickArmor(Player player, ItemStack armor) {
        if (!HbmPlayerArmorState.isMagnetActive(player)) {
            return;
        }

        for (ItemEntity item : player.level().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(this.range))) {
            Vec3 direction = player.position().subtract(item.position());
            if (direction.lengthSqr() < 1.0E-6D) {
                continue;
            }

            direction = direction.normalize();
            Vec3 motion = item.getDeltaMovement().add(direction.scale(0.05D));
            if (direction.y > 0.0D && motion.y < 0.04D) {
                motion = motion.add(0.0D, 0.2D, 0.0D);
            }
            item.setDeltaMovement(motion);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.armor.magnet_attract").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.armor.magnet_range", this.range).withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
