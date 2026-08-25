package com.reinhardt.hbm.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class BatteryPackItem extends LegacyVariantItem implements HbmChargeableItem {
    private static final String CHARGE_TAG = "charge";
    private static final Map<String, BatterySpec> SPECS = specs();

    public BatteryPackItem(Properties properties, List<Variant> variants) {
        super(properties.stacksTo(1), "battery_pack", variants);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        ObjMachineBlockItem.installRenderer(consumer);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        BatterySpec spec = spec(stack);
        long charge = storedCharge(stack);
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.battery.energy", formatShortNumber(charge), formatShortNumber(spec.capacity()), percent(charge, spec.capacity())).withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.battery.charge_rate", formatShortNumber(spec.chargeRate())).withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.battery.discharge_rate", formatShortNumber(spec.dischargeRate())).withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("item.reinhardtshbm.battery_pack").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public void addCreativeVariants(CreativeModeTab.Output output) {
        for (Variant variant : variants()) {
            ItemStack empty = stackFor(this, variant.id());
            hbmSetCharge(empty, 0L);
            output.accept(empty);

            ItemStack full = stackFor(this, variant.id());
            hbmSetCharge(full, SPECS.get(variant.id()).capacity());
            output.accept(full);
        }
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return storedCharge(stack) > 0L;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        BatterySpec spec = spec(stack);
        return Math.round(13.0F * storedCharge(stack) / Math.max(1.0F, spec.capacity()));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        BatterySpec spec = spec(stack);
        float fill = (float) storedCharge(stack) / Math.max(1.0F, spec.capacity());
        return Mth.hsvToRgb(fill / 3.0F, 1.0F, 1.0F);
    }

    public static boolean isBattery(ItemStack stack) {
        return !stack.isEmpty() && (stack.getItem() instanceof BatteryPackItem
                || stack.getItem() instanceof SelfChargingBatteryItem
                || stack.getItem() instanceof InfiniteBatteryItem
                || stack.getItem() instanceof HbmChargeableItem);
    }

    public static long dischargeIntoMachine(ItemStack stack, long stored, long capacity) {
        if (!isBattery(stack) || stored >= capacity) {
            return stored;
        }
        if (stack.getItem() instanceof InfiniteBatteryItem) {
            long extracted = Math.min(capacity - stored, InfiniteBatteryItem.TRANSFER_RATE);
            return stored + Math.max(0L, extracted);
        }
        if (stack.getItem() instanceof SelfChargingBatteryItem battery) {
            long extracted = Math.min(capacity - stored, battery.output(stack));
            return stored + Math.max(0L, extracted);
        }
        if (stack.getItem() instanceof HbmChargeableItem item) {
            long charge = item.hbmCharge(stack);
            long extracted = Math.min(Math.min(capacity - stored, item.hbmDischargeRate(stack)), charge);
            if (extracted > 0L) {
                item.hbmSetCharge(stack, charge - extracted);
            }
            return stored + Math.max(0L, extracted);
        }
        BatteryPackItem item = (BatteryPackItem) stack.getItem();
        BatterySpec spec = item.spec(stack);
        long charge = item.storedCharge(stack);
        long extracted = Math.min(Math.min(capacity - stored, spec.dischargeRate()), charge);
        if (extracted <= 0L) {
            return stored;
        }
        item.hbmSetCharge(stack, charge - extracted);
        return stored + extracted;
    }

    public static long chargeFromMachine(ItemStack stack, long stored) {
        if (!isBattery(stack) || stored <= 0L) {
            return stored;
        }
        if (stack.getItem() instanceof InfiniteBatteryItem) {
            return stored;
        }
        if (stack.getItem() instanceof SelfChargingBatteryItem) {
            return stored;
        }
        if (stack.getItem() instanceof HbmChargeableItem item) {
            long charge = item.hbmCharge(stack);
            long inserted = Math.min(Math.min(stored, item.hbmChargeRate(stack)), item.hbmCapacity(stack) - charge);
            if (inserted > 0L) {
                item.hbmSetCharge(stack, charge + inserted);
            }
            return stored - Math.max(0L, inserted);
        }
        BatteryPackItem item = (BatteryPackItem) stack.getItem();
        BatterySpec spec = item.spec(stack);
        long charge = item.storedCharge(stack);
        long inserted = Math.min(Math.min(stored, spec.chargeRate()), spec.capacity() - charge);
        if (inserted <= 0L) {
            return stored;
        }
        item.hbmSetCharge(stack, charge + inserted);
        return stored - inserted;
    }

    public static long charge(ItemStack stack) {
        if (stack.getItem() instanceof InfiniteBatteryItem) {
            return InfiniteBatteryItem.POWER;
        }
        if (stack.getItem() instanceof SelfChargingBatteryItem battery) {
            return battery.output(stack);
        }
        if (stack.getItem() instanceof HbmChargeableItem item) {
            return item.hbmCharge(stack);
        }
        if (!(stack.getItem() instanceof BatteryPackItem item)) {
            return 0L;
        }
        return item.storedCharge(stack);
    }

    public static long capacity(ItemStack stack) {
        if (stack.getItem() instanceof InfiniteBatteryItem) {
            return InfiniteBatteryItem.POWER;
        }
        if (stack.getItem() instanceof SelfChargingBatteryItem battery) {
            return battery.output(stack);
        }
        if (stack.getItem() instanceof HbmChargeableItem item) {
            return item.hbmCapacity(stack);
        }
        if (!(stack.getItem() instanceof BatteryPackItem item)) {
            return 0L;
        }
        return item.spec(stack).capacity();
    }

    public static long chargeRate(ItemStack stack) {
        if (stack.getItem() instanceof InfiniteBatteryItem) {
            return 0L;
        }
        if (stack.getItem() instanceof SelfChargingBatteryItem) {
            return 0L;
        }
        if (stack.getItem() instanceof HbmChargeableItem item) {
            return item.hbmChargeRate(stack);
        }
        if (!(stack.getItem() instanceof BatteryPackItem item)) {
            return 0L;
        }
        return item.spec(stack).chargeRate();
    }

    public static long dischargeRate(ItemStack stack) {
        if (stack.getItem() instanceof InfiniteBatteryItem) {
            return InfiniteBatteryItem.TRANSFER_RATE;
        }
        if (stack.getItem() instanceof SelfChargingBatteryItem battery) {
            return battery.output(stack);
        }
        if (stack.getItem() instanceof HbmChargeableItem item) {
            return item.hbmDischargeRate(stack);
        }
        if (!(stack.getItem() instanceof BatteryPackItem item)) {
            return 0L;
        }
        return item.spec(stack).dischargeRate();
    }

    public static void setStoredCharge(ItemStack stack, long charge) {
        if (stack.getItem() instanceof InfiniteBatteryItem || stack.getItem() instanceof SelfChargingBatteryItem) {
            return;
        }
        if (stack.getItem() instanceof HbmChargeableItem item) {
            item.hbmSetCharge(stack, charge);
            return;
        }
        if (stack.getItem() instanceof BatteryPackItem item) {
            item.hbmSetCharge(stack, charge);
        }
    }

    public static boolean isCapacitor(ItemStack stack) {
        if (stack.getItem() instanceof InfiniteBatteryItem) {
            return false;
        }
        if (!(stack.getItem() instanceof BatteryPackItem item)) {
            return false;
        }
        return item.variant(stack).id().startsWith("capacitor_");
    }

    public static String variantId(ItemStack stack) {
        if (stack.getItem() instanceof InfiniteBatteryItem) {
            return "battery_creative";
        }
        if (stack.getItem() instanceof SelfChargingBatteryItem) {
            return "battery_sc";
        }
        if (!(stack.getItem() instanceof BatteryPackItem item)) {
            return "";
        }
        return item.variant(stack).id();
    }

    private BatterySpec spec(ItemStack stack) {
        return SPECS.getOrDefault(variant(stack).id(), SPECS.get("battery_redstone"));
    }

    @Override
    public long hbmCharge(ItemStack stack) {
        return storedCharge(stack);
    }

    @Override
    public long hbmCapacity(ItemStack stack) {
        return spec(stack).capacity();
    }

    @Override
    public long hbmChargeRate(ItemStack stack) {
        return spec(stack).chargeRate();
    }

    @Override
    public long hbmDischargeRate(ItemStack stack) {
        return spec(stack).dischargeRate();
    }

    @Override
    public void hbmSetCharge(ItemStack stack, long charge) {
        setStoredChargeInternal(stack, charge);
    }

    private long storedCharge(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        long value = tag.contains(CHARGE_TAG) ? tag.getLong(CHARGE_TAG) : 0L;
        return Math.max(0L, Math.min(value, spec(stack).capacity()));
    }

    private void setStoredChargeInternal(ItemStack stack, long charge) {
        BatterySpec spec = spec(stack);
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putLong(CHARGE_TAG, Math.max(0L, Math.min(charge, spec.capacity())));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static String formatShortNumber(long value) {
        if (value >= 1_000_000_000L) {
            return trim(value / 1_000_000_000.0D) + "G";
        }
        if (value >= 1_000_000L) {
            return trim(value / 1_000_000.0D) + "M";
        }
        if (value >= 1_000L) {
            return trim(value / 1_000.0D) + "k";
        }
        return Long.toString(value);
    }

    private static String trim(double value) {
        return value >= 100.0D ? String.format(java.util.Locale.ROOT, "%.0f", value) : String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    private static String percent(long charge, long capacity) {
        return String.format(java.util.Locale.ROOT, "%.1f", charge * 100.0D / Math.max(1.0D, capacity));
    }

    private static Map<String, BatterySpec> specs() {
        Map<String, BatterySpec> specs = new HashMap<>();
        addBattery(specs, "battery_redstone", 100L);
        addBattery(specs, "battery_lead", 1_000L);
        addBattery(specs, "battery_lithium", 10_000L);
        addBattery(specs, "battery_sodium", 50_000L);
        addBattery(specs, "battery_schrabidium", 250_000L);
        specs.put("battery_quantum", new BatterySpec(1_000_000L * 20L * 60L * 60L, 10_000_000L, 1_000_000L));
        addCapacitor(specs, "capacitor_copper", 1_000L);
        addCapacitor(specs, "capacitor_gold", 10_000L);
        addCapacitor(specs, "capacitor_niobium", 100_000L);
        addCapacitor(specs, "capacitor_tantalum", 500_000L);
        addCapacitor(specs, "capacitor_bismuth", 2_500_000L);
        addCapacitor(specs, "capacitor_spark", 10_000_000L);
        return Map.copyOf(specs);
    }

    private static void addBattery(Map<String, BatterySpec> specs, String id, long dischargeRate) {
        specs.put(id, new BatterySpec(dischargeRate * 20L * 60L * 15L, dischargeRate * 10L, dischargeRate));
    }

    private static void addCapacitor(Map<String, BatterySpec> specs, String id, long dischargeRate) {
        specs.put(id, new BatterySpec(dischargeRate * 20L * 30L, dischargeRate, dischargeRate));
    }

    private record BatterySpec(long capacity, long chargeRate, long dischargeRate) {
    }
}
