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

/** 1.7.10 ItemCustomMissile NBT payload, retained as component-backed custom data. */
public final class CustomMissileItem extends Item {
    private static final String CHIP = "chip";
    private static final String WARHEAD = "warhead";
    private static final String FUSELAGE = "fuselage";
    private static final String STABILITY = "stability";
    private static final String THRUSTER = "thruster";

    public CustomMissileItem(Properties properties) {
        super(properties);
    }

    public static ItemStack build(ItemStack chip, ItemStack warhead, ItemStack fuselage, ItemStack stability, ItemStack thruster) {
        ItemStack missile = new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                com.reinhardt.hbm.ReinhardtsHBM.id("missile_custom")));
        CompoundTag tag = new CompoundTag();
        tag.putString(CHIP, MissilePartItem.id(chip));
        tag.putString(WARHEAD, MissilePartItem.id(warhead));
        tag.putString(FUSELAGE, MissilePartItem.id(fuselage));
        tag.putString(THRUSTER, MissilePartItem.id(thruster));
        if (!stability.isEmpty()) {
            tag.putString(STABILITY, MissilePartItem.id(stability));
        }
        missile.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return missile;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains(WARHEAD) || !tag.contains(FUSELAGE) || !tag.contains(THRUSTER) || !tag.contains(CHIP)) {
            tooltip.add(Component.translatable("tooltip.reinhardtshbm.missile.invalid").withStyle(ChatFormatting.RED));
            return;
        }
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.missile.custom.warhead", tag.getString(WARHEAD)).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.missile.custom.fuselage", tag.getString(FUSELAGE)).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.missile.custom.thruster", tag.getString(THRUSTER)).withStyle(ChatFormatting.GRAY));
        if (tag.contains(STABILITY)) {
            tooltip.add(Component.translatable("tooltip.reinhardtshbm.missile.custom.fins", tag.getString(STABILITY)).withStyle(ChatFormatting.GRAY));
        }
    }
}
