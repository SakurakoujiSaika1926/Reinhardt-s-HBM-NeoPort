package com.reinhardt.hbm.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

/** Restores the two 1.12.2 traits advertised by the rad-sealed solid duct. */
public final class RadSealedFluidDuctBlockItem extends BlockItem {
    public RadSealedFluidDuctBlockItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.fluid_duct_solid_sealed.radshield")
                .withStyle(ChatFormatting.DARK_GREEN));
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.fluid_duct_solid_sealed.blastres", 400)
                .withStyle(ChatFormatting.GOLD));
    }
}
