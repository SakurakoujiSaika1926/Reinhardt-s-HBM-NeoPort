package com.reinhardt.hbm.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.List;

public class KeyPinItem extends Item {
    public static final String PINS_KEY = "pins";

    private final boolean transferable;

    public KeyPinItem(Properties properties) {
        this(properties, true);
    }

    public KeyPinItem(Properties properties, boolean transferable) {
        super(properties.stacksTo(1));
        this.transferable = transferable;
    }

    public boolean canTransfer() {
        return this.transferable;
    }

    public static int pins(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.getInt(PINS_KEY);
    }

    public static void setPins(ItemStack stack, int pins) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt(PINS_KEY, pins);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int pins = pins(stack);
        if (pins != 0) {
            tooltip.add(Component.translatable("desc.reinhardtshbm.key_pin.config", pins).withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("desc.reinhardtshbm.key_pin.not_set").withStyle(ChatFormatting.RED));
        }
        if (!this.transferable) {
            tooltip.add(Component.empty());
            tooltip.add(Component.translatable("desc.reinhardtshbm.key_pin.no_transfer").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
