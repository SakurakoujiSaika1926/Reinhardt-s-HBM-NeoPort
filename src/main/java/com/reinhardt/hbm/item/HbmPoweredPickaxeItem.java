package com.reinhardt.hbm.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.List;

/** Direct modern equivalent of 1.7.10 ItemToolAbilityPower. */
public final class HbmPoweredPickaxeItem extends HbmPickaxeItem implements HbmChargeableItem {
    private static final String CHARGE_KEY = "charge";

    private final long maxPower;
    private final long chargeRate;
    private final long consumption;

    public HbmPoweredPickaxeItem(HbmToolProfile profile, long maxPower, long chargeRate, long consumption) {
        super(profile);
        this.maxPower = Math.max(1L, maxPower);
        this.chargeRate = Math.max(0L, chargeRate);
        this.consumption = Math.max(1L, consumption);
    }

    @Override
    protected boolean canOperate(ItemStack stack) {
        return hbmCharge(stack) >= this.consumption;
    }

    @Override
    protected void consumeOperation(ItemStack stack) {
        hbmSetCharge(stack, hbmCharge(stack) - this.consumption);
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return hbmCharge(stack) < this.maxPower;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Mth.clamp(Math.round(13.0F * hbmCharge(stack) / (float) this.maxPower), 0, 13);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x00FF00;
    }

    @Override
    public long hbmCharge(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.contains(CHARGE_KEY) ? Mth.clamp(tag.getLong(CHARGE_KEY), 0L, this.maxPower) : this.maxPower;
    }

    @Override
    public long hbmCapacity(ItemStack stack) {
        return this.maxPower;
    }

    @Override
    public long hbmChargeRate(ItemStack stack) {
        return this.chargeRate;
    }

    @Override
    public long hbmDischargeRate(ItemStack stack) {
        return 0L;
    }

    @Override
    public void hbmSetCharge(ItemStack stack, long charge) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putLong(CHARGE_KEY, Mth.clamp(charge, 0L, this.maxPower));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(
                "tooltip.reinhardtshbm.armor.charge",
                BatteryPackItem.formatShortNumber(hbmCharge(stack)),
                BatteryPackItem.formatShortNumber(this.maxPower)
        ).withStyle(ChatFormatting.GREEN));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
