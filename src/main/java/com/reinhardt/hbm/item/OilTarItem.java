package com.reinhardt.hbm.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class OilTarItem extends Item {
    private final Variant variant;

    public OilTarItem(Properties properties, Variant variant) {
        super(properties);
        this.variant = variant == null ? Variant.CRUDE : variant;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.reinhardtshbm.oil_tar." + this.variant.id());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (this.variant != Variant.CRUDE) {
            tooltip.add(Component.translatable("item.reinhardtshbm.oil_tar").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    public enum Variant {
        CRUDE,
        CRACK,
        COAL,
        WOOD,
        WAX,
        PARAFFIN;

        public String id() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        public static Variant byId(String id) {
            if (id == null || id.isBlank()) {
                return CRUDE;
            }
            for (Variant variant : values()) {
                if (variant.id().equals(id)) {
                    return variant;
                }
            }
            return CRUDE;
        }
    }
}
