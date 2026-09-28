package com.reinhardt.hbm.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;

/**
 * Direct equivalent of 1.7.10 ItemMetaUpgrade: one upgrade id with three
 * metadata tiers and distinct inventory textures.
 */
public final class LegacyMetaUpgradeItem extends MachineUpgradeItem {
    private static final String TIER_TAG = "tier";
    private final String baseId;

    public LegacyMetaUpgradeItem(Properties properties, String baseId) {
        super(properties, UpgradeType.SPECIAL, 0);
        this.baseId = baseId;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.reinhardtshbm." + this.baseId + "_" + tier(stack));
    }

    public void addCreativeVariants(CreativeModeTab.Output output) {
        for (int tier = 1; tier <= 3; tier++) {
            output.accept(stackFor(this, tier));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        int resolvedTier = tier(stack);
        if (this.baseId.equals("upgrade_stack")) {
            int amount = switch (resolvedTier) {
                case 2 -> 16;
                case 3 -> 64;
                default -> 4;
            };
            tooltip.add(Component.translatable("tooltip.reinhardtshbm.upgrade.crane_stack", amount)
                    .withStyle(ChatFormatting.GREEN));
        } else if (this.baseId.equals("upgrade_ejector")) {
            int delay = switch (resolvedTier) {
                case 2 -> 5;
                case 3 -> 2;
                default -> 10;
            };
            tooltip.add(Component.translatable("tooltip.reinhardtshbm.upgrade.crane_ejector", delay)
                    .withStyle(ChatFormatting.GREEN));
        }
    }

    public static ItemStack stackFor(LegacyMetaUpgradeItem item, int tier) {
        int normalizedTier = Math.clamp(tier, 1, 3);
        ItemStack stack = new ItemStack(item);
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt(TIER_TAG, normalizedTier);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(normalizedTier - 1));
        return stack;
    }

    public static int tier(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.contains(TIER_TAG)) {
            return Math.clamp(tag.getInt(TIER_TAG), 1, 3);
        }
        CustomModelData modelData = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        return modelData == null ? 1 : Math.clamp(modelData.value() + 1, 1, 3);
    }
}
