package com.reinhardt.hbm.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.List;

/** The single 1.7.10 depleted-pellet item with its old material subtype in data. */
public final class RtgDepletedPelletItem extends Item {
    public RtgDepletedPelletItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public Component getName(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        String material = tag.getString("rtg_depleted_material");
        return material.isBlank()
                ? super.getName(stack)
                : Component.translatable("item.reinhardtshbm.pellet_rtg_depleted." + material);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        String material = tag.getString("rtg_depleted_material");
        if (!material.isBlank()) tooltip.add(Component.translatable("item.reinhardtshbm.rtg_depleted_material." + material));
    }
}
