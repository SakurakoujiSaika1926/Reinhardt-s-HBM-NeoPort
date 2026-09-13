package com.reinhardt.hbm.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import java.util.Locale;

/** 1.7.10 EntityDummy target dummy. */
public final class LegacyDummyEntity extends PathfinderMob {
    public LegacyDummyEntity(EntityType<? extends LegacyDummyEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        return super.hurt(source, amount);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() instanceof ArmorItem armor) {
            EquipmentSlot slot = armor.getEquipmentSlot();
            // 1.7.10 copied the complete held stack into the armor slot.
            // Armor normally stacks to one, but preserving the exact copy
            // semantics matters for custom/legacy armor items.
            setItemSlot(slot, held.copy());
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public Component getDisplayName() {
        return Component.literal(String.format(Locale.ROOT, "%.1f / %.1f", getHealth(), getMaxHealth()));
    }

    @Override
    public boolean isCustomNameVisible() {
        return true;
    }

    @Override
    protected float getEquipmentDropChance(EquipmentSlot slot) {
        // EntityDummy#dropEquipment was an empty override in 1.7.10.  Armor
        // placed on the target dummy is for display/testing only and must not
        // become a death drop.
        return 0.0F;
    }
}
