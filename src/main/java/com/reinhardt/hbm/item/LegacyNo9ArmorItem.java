package com.reinhardt.hbm.item;

import com.reinhardt.hbm.radiation.HbmLivingHazards;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.List;
import java.util.function.Consumer;

/** Exact 1.7.10 black-lung suppression from ArmorNo9. */
public final class LegacyNo9ArmorItem extends ArmorItem {
    public LegacyNo9ArmorItem(Holder<ArmorMaterial> material) {
        super(material, Type.HELMET, new Properties().stacksTo(1).durability(Type.HELMET.getDurability(30)));
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public net.minecraft.client.model.HumanoidModel<?> getHumanoidArmorModel(
                    LivingEntity livingEntity,
                    ItemStack itemStack,
                    EquipmentSlot equipmentSlot,
                    net.minecraft.client.model.HumanoidModel<?> original
            ) {
                original.setAllVisible(false);
                return original;
            }
        });
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean selected) {
        if (level.isClientSide || !(entity instanceof Player player)
                || player.getItemBySlot(EquipmentSlot.HEAD) != stack) {
            return;
        }

        HbmLivingHazards hazards = HbmLivingHazards.get(player);
        int blackLung = hazards.getBlackLung();
        int cap = (int) (HbmLivingHazards.MAX_BLACK_LUNG * 0.9D);
        if (blackLung > cap) {
            hazards.setBlackLung(player, cap);
        } else if (blackLung >= HbmLivingHazards.MAX_BLACK_LUNG / 4) {
            hazards.setBlackLung(player, blackLung - 1);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("+0.5 DT").withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.literal("Lets you breathe coal, neat!").withStyle(ChatFormatting.YELLOW));
    }
}
