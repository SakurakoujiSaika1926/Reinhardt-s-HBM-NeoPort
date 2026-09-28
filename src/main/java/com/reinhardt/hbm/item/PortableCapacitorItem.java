package com.reinhardt.hbm.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.List;
import java.util.function.Consumer;

/**
 * Flattened form of one 1.7.10 {@code ItemBatteryPack} capacitor metadata value.
 * Capacitors hold thirty seconds of their transfer rate and start empty when crafted.
 */
public final class PortableCapacitorItem extends Item implements HbmChargeableItem {
    private static final String CHARGE_TAG = "charge";

    private final String socketVariant;
    private final long capacity;
    private final long transferRate;

    public PortableCapacitorItem(Properties properties, String socketVariant, long transferRate) {
        super(properties.stacksTo(1));
        this.socketVariant = socketVariant;
        this.transferRate = Math.max(0L, transferRate);
        this.capacity = Math.multiplyExact(this.transferRate, 20L * 30L);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        ObjMachineBlockItem.installRenderer(consumer);
    }

    public String socketVariant() {
        return socketVariant;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return hbmCharge(stack) < capacity;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Mth.clamp(Math.round(13.0F * hbmCharge(stack) / (float) capacity), 0, 13);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        float fill = (float) hbmCharge(stack) / Math.max(1.0F, capacity);
        return Mth.hsvToRgb(fill / 3.0F, 1.0F, 1.0F);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        long charge = hbmCharge(stack);
        tooltip.add(Component.translatable(
                "tooltip.reinhardtshbm.battery.energy",
                BatteryPackItem.formatShortNumber(charge),
                BatteryPackItem.formatShortNumber(capacity),
                String.format(java.util.Locale.ROOT, "%.1f", charge * 100.0D / Math.max(1L, capacity))
        ).withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.translatable(
                "tooltip.reinhardtshbm.battery.charge_rate",
                BatteryPackItem.formatShortNumber(transferRate)
        ).withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable(
                "tooltip.reinhardtshbm.battery.discharge_rate",
                BatteryPackItem.formatShortNumber(transferRate)
        ).withStyle(ChatFormatting.YELLOW));
    }

    @Override
    public long hbmCharge(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.contains(CHARGE_TAG) ? Mth.clamp(tag.getLong(CHARGE_TAG), 0L, capacity) : 0L;
    }

    @Override
    public long hbmCapacity(ItemStack stack) {
        return capacity;
    }

    @Override
    public long hbmChargeRate(ItemStack stack) {
        return transferRate;
    }

    @Override
    public long hbmDischargeRate(ItemStack stack) {
        return transferRate;
    }

    @Override
    public void hbmSetCharge(ItemStack stack, long charge) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putLong(CHARGE_TAG, Mth.clamp(charge, 0L, capacity));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
}
