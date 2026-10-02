package com.reinhardt.hbm.item;

import com.reinhardt.hbm.entity.LegacyBlackHoleEntity;
import com.reinhardt.hbm.config.HbmConfig;
import com.reinhardt.hbm.radiation.HbmLivingRadiation;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Exact gameplay counterpart of 1.7.10 ItemDigamma(60). */
public final class LegacyDigammaParticleItem extends ParticleCapsuleItem {
    private static final float DOSE_PER_TICK = 1.0F / 60.0F;

    public LegacyDigammaParticleItem(Properties properties) {
        super(properties, true);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (!level.isClientSide && entity instanceof Player player) {
            HbmLivingRadiation data = HbmLivingRadiation.get(player);
            data.addDigamma(DOSE_PER_TICK);
            HbmLivingRadiation.set(player, data);
        }
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (!entity.level().isClientSide && entity.onGround()) {
            // EntityQuasar in the current 1.7.10 source is the size-5
            // black-hole implementation; no separate world logic is added.
            if (HbmConfig.DROPPED_SINGULARITY_EFFECT.get()) {
                LegacyBlackHoleEntity.spawn(entity.level(), entity.position(), 5.0F, false);
            }
            entity.discard();
            return true;
        }
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.particle_digamma.particle_half_life")
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.particle_digamma.player_half_life", "3.0 s")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.empty());
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.particle_digamma.dose", "3.3 mDRX/s")
                .withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.particle_digamma.drop")
                .withStyle(ChatFormatting.RED));
    }
}
