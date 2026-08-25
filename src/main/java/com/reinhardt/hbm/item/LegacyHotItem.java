package com.reinhardt.hbm.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

/**
 * 1.7.10 ItemHot port. Heat is intentionally item-local rather than a shared
 * material value: heated outputs may be stacked with cold outputs in modern
 * inventories, so each stack keeps its own remaining heat counter.
 */
public class LegacyHotItem extends Item {
    public static final String HEAT_TAG = "heat";

    private final int baseHeat;
    private final boolean dusted;

    public LegacyHotItem(Properties properties, int baseHeat, boolean dusted) {
        super(properties);
        this.baseHeat = baseHeat;
        this.dusted = dusted;
    }

    public int maxHeat(ItemStack stack) {
        if (!this.dusted) {
            return this.baseHeat;
        }
        // The old ItemHotDusted used metadata as the forged grade.
        return Math.max(0, this.baseHeat - stack.getDamageValue() * 10);
    }

    public int heat(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt(HEAT_TAG);
    }

    public double heatFraction(ItemStack stack) {
        int max = maxHeat(stack);
        return max <= 0 ? 0.0D : Math.clamp((double) heat(stack) / max, 0.0D, 1.0D);
    }

    public static ItemStack heatUp(ItemStack stack) {
        if (stack.getItem() instanceof LegacyHotItem hot) {
            hot.setHeat(stack, hot.maxHeat(stack));
        }
        return stack;
    }

    public static ItemStack heatUp(ItemStack stack, double fraction) {
        if (stack.getItem() instanceof LegacyHotItem hot) {
            hot.setHeat(stack, (int) (hot.maxHeat(stack) * fraction));
        }
        return stack;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean selected) {
        if (!level.isClientSide) {
            int heat = heat(stack);
            if (heat > 0) {
                setHeat(stack, heat - 1);
            }
        }
        super.inventoryTick(stack, level, entity, slotId, selected);
    }

    public void setHeat(ItemStack stack, int heat) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (heat <= 0) {
            tag.remove(HEAT_TAG);
            stack.remove(DataComponents.CUSTOM_MODEL_DATA);
        } else {
            tag.putInt(HEAT_TAG, Math.min(heat, maxHeat(stack)));
            stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(1));
        }
        if (tag.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }
}
