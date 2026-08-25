package com.reinhardt.hbm.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomModelData;

import java.util.List;

/**
 * Direct port of 1.7.10 ItemRBMKPellet metadata.  The former damage value is
 * stored as CustomModelData so each depletion and xenon state remains an
 * ordinary, recipe-compatible item stack in 1.21.1.
 */
public final class RbmkPelletItem extends Item {
    private final String legacyId;
    private final String fullName;
    private final boolean xenon;

    public RbmkPelletItem(Properties properties, String legacyId, String fullName, boolean xenon) {
        super(properties);
        this.legacyId = legacyId;
        this.fullName = fullName;
        this.xenon = xenon;
    }

    public String legacyId() {
        return this.legacyId;
    }

    public boolean hasXenon() {
        return this.xenon;
    }

    public int state(ItemStack stack) {
        CustomModelData modelData = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        int max = this.xenon ? 10 : 5;
        return Math.floorMod(modelData == null ? 0 : modelData.value(), max);
    }

    public ItemStack stackFor(int state) {
        ItemStack stack = new ItemStack(this);
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(Math.floorMod(state, this.xenon ? 10 : 5)));
        return stack;
    }

    public void addCreativeVariants(CreativeModeTab.Output output) {
        for (int state = 0; state < (this.xenon ? 10 : 5); state++) {
            output.accept(stackFor(state));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(this.fullName).withStyle(ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.rbmk_pellet.recycling").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        switch (state(stack) % 5) {
            case 0 -> tooltip.add(Component.translatable("tooltip.reinhardtshbm.rbmk_pellet.new").withStyle(ChatFormatting.GOLD));
            case 1 -> tooltip.add(Component.translatable("tooltip.reinhardtshbm.rbmk_pellet.barely_depleted").withStyle(ChatFormatting.YELLOW));
            case 2 -> tooltip.add(Component.translatable("tooltip.reinhardtshbm.rbmk_pellet.moderately_depleted").withStyle(ChatFormatting.GREEN));
            case 3 -> tooltip.add(Component.translatable("tooltip.reinhardtshbm.rbmk_pellet.highly_depleted").withStyle(ChatFormatting.DARK_GREEN));
            default -> tooltip.add(Component.translatable("tooltip.reinhardtshbm.rbmk_pellet.fully_depleted").withStyle(ChatFormatting.DARK_GRAY));
        }
        if (this.xenon && state(stack) >= 5) {
            tooltip.add(Component.translatable("tooltip.reinhardtshbm.rbmk_pellet.xenon").withStyle(ChatFormatting.DARK_PURPLE));
        }
    }
}
