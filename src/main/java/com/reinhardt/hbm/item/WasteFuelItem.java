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

/**
 * Former ItemDepletedFuel metadata.  Metadata value 1 represented freshly
 * unloaded, hot fuel and value 0 represented fuel cooled in the spent-fuel
 * pool.  The state is retained explicitly in 1.21 item data.
 */
public final class WasteFuelItem extends Item {
    public static final String HOT_TAG = "spentFuelPoolCooling";

    public WasteFuelItem(Properties properties) {
        super(properties);
    }

    public static boolean isHot(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean(HOT_TAG);
    }

    public static ItemStack hot(ItemStack stack) {
        ItemStack result = stack.copy();
        CompoundTag tag = result.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putBoolean(HOT_TAG, true);
        result.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return result;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (isHot(stack)) {
            tooltip.add(Component.translatable("desc.item.wasteCooling").withStyle(ChatFormatting.GOLD));
        }
    }
}
