package com.reinhardt.hbm.item;

import com.reinhardt.hbm.block.StorageCrateBlock;
import com.reinhardt.hbm.blockentity.StorageCrateBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class StorageCrateBlockItem extends BlockItem {
    public StorageCrateBlockItem(StorageCrateBlock block, Properties properties) {
        super(block, properties.stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (this.getBlock() instanceof StorageCrateBlock crate) {
            int total = crate.kind().slots();
            int used = StorageCrateBlockEntity.usedSlots(stack, total);
            float percent = total <= 0 ? 0.0F : used * 100.0F / total;
            ChatFormatting main = percent >= 75.0F ? ChatFormatting.RED : percent < 25.0F ? ChatFormatting.GREEN : ChatFormatting.YELLOW;
            tooltip.add(Component.translatable(
                    "tooltip.reinhardtshbm.crate.slots_used",
                    Component.literal(Integer.toString(used)).withStyle(main),
                    Component.literal(Integer.toString(total)).withStyle(ChatFormatting.GOLD),
                    Component.literal(String.format(java.util.Locale.ROOT, "%.1f", percent)).withStyle(main)
            ).withStyle(ChatFormatting.GRAY));
        }
    }
}
