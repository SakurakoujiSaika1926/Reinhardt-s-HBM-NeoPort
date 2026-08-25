package com.reinhardt.hbm.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.List;

/** Direct modern equivalent of 1.7.10 ArmorFSBPowered. */
public class PoweredArmorFSBItem extends ArmorFSBItem implements HbmChargeableItem {
    private static final String CHARGE_KEY = "charge";

    private final long maxPower;
    private final long chargeRate;
    private final long consumption;
    private final long drain;

    public PoweredArmorFSBItem(
            String fsbGroup,
            Holder<ArmorMaterial> material,
            ArmorItem.Type type,
            boolean noHelmet,
            List<FullSetEffect> fullSetEffects,
            long maxPower,
            long chargeRate,
            long consumption,
            long drain,
            Properties properties
    ) {
        super(fsbGroup, material, type, noHelmet, fullSetEffects, properties);
        this.maxPower = Math.max(1L, maxPower);
        this.chargeRate = Math.max(0L, chargeRate);
        this.consumption = Math.max(0L, consumption);
        this.drain = Math.max(0L, drain);
    }

    @Override
    public boolean isArmorEnabled(ItemStack stack) {
        return hbmCharge(stack) > 0L;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (!level.isClientSide && entity instanceof Player player && player.getItemBySlot(this.getEquipmentSlot()) == stack
                && this.drain > 0L && !player.getAbilities().instabuild && hasFSBArmor(player)) {
            hbmSetCharge(stack, hbmCharge(stack) - this.drain);
        }
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return hbmCharge(stack) < hbmCapacity(stack);
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Mth.clamp(Math.round(13.0F * hbmCharge(stack) / (float) hbmCapacity(stack)), 0, 13);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x00FF00;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(
                "tooltip.reinhardtshbm.armor.charge",
                BatteryPackItem.formatShortNumber(hbmCharge(stack)),
                BatteryPackItem.formatShortNumber(hbmCapacity(stack))
        ).withStyle(ChatFormatting.GREEN));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public long hbmCharge(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains(CHARGE_KEY)) {
            return this.maxPower;
        }
        return Mth.clamp(tag.getLong(CHARGE_KEY), 0L, this.maxPower);
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

    public long consumption() {
        return this.consumption;
    }

    /** Exact 1.7.10 ItemModPads static-charge calculation. */
    public long staticRechargeAmount() {
        long charge = this.drain / 2L;
        return charge == 0L ? this.consumption / 40L : charge;
    }
}
