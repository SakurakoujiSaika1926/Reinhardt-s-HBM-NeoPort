package com.reinhardt.hbm.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.List;

public class ZirnoxRodItem extends LegacyVariantItem {
    private static final String LIFE_TAG = "life";

    public static final List<Fuel> FUELS = List.of(
            new Fuel("natural_uranium_fuel", 250_000, 30, false),
            new Fuel("uranium_fuel", 200_000, 50, false),
            new Fuel("th232_fuel", 20_000, 0, true),
            new Fuel("thorium_fuel", 200_000, 40, false),
            new Fuel("mox_fuel", 165_000, 75, false),
            new Fuel("plutonium_fuel", 175_000, 65, false),
            new Fuel("u233_fuel", 150_000, 100, false),
            new Fuel("u235_fuel", 165_000, 85, false),
            new Fuel("les_fuel", 150_000, 150, false),
            new Fuel("lithium_fuel", 20_000, 0, true),
            new Fuel("zfb_mox_fuel", 50_000, 35, false)
    );

    public ZirnoxRodItem(Properties properties) {
        super(
                properties.stacksTo(1),
                "rod_zirnox",
                LegacyVariantItem.variants(FUELS.stream().map(Fuel::id).toArray(String[]::new))
        );
    }

    public Fuel fuel(ItemStack stack) {
        String id = variant(stack).id();
        for (Fuel fuel : FUELS) {
            if (fuel.id().equals(id)) {
                return fuel;
            }
        }
        return FUELS.getFirst();
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return life(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        Fuel fuel = fuel(stack);
        return Math.min(13, Math.round(13.0F * life(stack) / Math.max(1, fuel.maxLife())));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x5fd14f;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Fuel fuel = fuel(stack);
        double depletion = Math.floor(life(stack) * 100_000.0D / Math.max(1, fuel.maxLife())) / 1_000.0D;
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.zirnox_rod.depletion", depletion).withStyle(ChatFormatting.YELLOW));
        if (fuel.breeding()) {
            tooltip.add(Component.translatable("tooltip.reinhardtshbm.zirnox_rod.breeding").withStyle(ChatFormatting.DARK_AQUA));
        } else {
            tooltip.add(Component.translatable("tooltip.reinhardtshbm.zirnox_rod.heat", fuel.heat()).withStyle(ChatFormatting.DARK_AQUA));
        }
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.zirnox_rod.life", fuel.maxLife()).withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    public static int life(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt(LIFE_TAG);
    }

    public static void setLife(ItemStack stack, int life) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt(LIFE_TAG, Math.max(0, life));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static void incrementLife(ItemStack stack) {
        setLife(stack, life(stack) + 1);
    }

    public record Fuel(String id, int maxLife, int heat, boolean breeding) {
    }
}
