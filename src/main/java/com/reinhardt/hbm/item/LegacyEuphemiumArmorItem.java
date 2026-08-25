package com.reinhardt.hbm.item;

import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Exact full-set effects from 1.7.10 ArmorEuphemium. */
public final class LegacyEuphemiumArmorItem extends ArmorItem {
    public LegacyEuphemiumArmorItem(Holder<ArmorMaterial> material, Type type) {
        super(material, type, new Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean selected) {
        if (level.isClientSide || !(entity instanceof Player player)
                || player.getItemBySlot(this.getEquipmentSlot()) != stack || !hasFullSet(player)) {
            return;
        }
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 5, 127, true, false));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 5, 127, true, false));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 5, 127, true, false));
        player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 5, 127, true, false));
        if (player.getDeltaMovement().y < -0.25D) {
            player.setDeltaMovement(player.getDeltaMovement().x, -0.25D, player.getDeltaMovement().z);
            player.fallDistance = 0.0F;
        }
    }

    private static boolean hasFullSet(Player player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).is(HbmItems.EUPHEMIUM_HELMET.get())
                && player.getItemBySlot(EquipmentSlot.CHEST).is(HbmItems.EUPHEMIUM_PLATE.get())
                && player.getItemBySlot(EquipmentSlot.LEGS).is(HbmItems.EUPHEMIUM_LEGS.get())
                && player.getItemBySlot(EquipmentSlot.FEET).is(HbmItems.EUPHEMIUM_BOOTS.get());
    }
}
