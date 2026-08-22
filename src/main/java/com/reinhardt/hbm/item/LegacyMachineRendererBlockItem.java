package com.reinhardt.hbm.item;

import com.reinhardt.hbm.client.render.LegacyMachineItemRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.List;
import java.util.function.Consumer;

public final class LegacyMachineRendererBlockItem extends LegacyOffsetBlockItem {
    public LegacyMachineRendererBlockItem(Block block, Item.Properties properties, int legacyOffset) {
        super(block, properties, legacyOffset, true);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private final LegacyMachineItemRenderer renderer = new LegacyMachineItemRenderer();

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return this.renderer;
            }
        });
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        String id = BuiltInRegistries.BLOCK.getKey(getBlock()).getPath();
        if (id.equals("machine_radiolysis") || id.equals("machine_rtg_grey")) {
            tooltip.add(Component.translatable("tooltip.reinhardtshbm.search_alias." + id).withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
