package com.reinhardt.hbm.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Chicago Pile rod item with the original common and type-specific descriptions. */
public final class PileRodItem extends Item {
    private final Kind kind;

    public PileRodItem(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
    }

    public Kind kind() {
        return this.kind;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.pile_rod.common").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.pile_rod.extract").withStyle(ChatFormatting.YELLOW));
        for (String key : this.kind.tooltipKeys()) {
            tooltip.add(Component.translatable(key).withStyle(this.kind.tooltipColor()));
        }
    }

    public enum Kind {
        URANIUM(ChatFormatting.GREEN,
                "tooltip.reinhardtshbm.pile_rod.uranium",
                "tooltip.reinhardtshbm.pile_rod.inspect"),
        PU239(ChatFormatting.GREEN,
                "tooltip.reinhardtshbm.pile_rod.pu239"),
        PLUTONIUM(ChatFormatting.LIGHT_PURPLE,
                "tooltip.reinhardtshbm.pile_rod.source"),
        SOURCE(ChatFormatting.LIGHT_PURPLE,
                "tooltip.reinhardtshbm.pile_rod.source"),
        BORON(ChatFormatting.BLUE,
                "tooltip.reinhardtshbm.pile_rod.boron"),
        LITHIUM(ChatFormatting.GREEN,
                "tooltip.reinhardtshbm.pile_rod.lithium",
                "tooltip.reinhardtshbm.pile_rod.inspect"),
        DETECTOR(ChatFormatting.BLUE,
                "tooltip.reinhardtshbm.pile_rod.detector");

        private final ChatFormatting tooltipColor;
        private final String[] tooltipKeys;

        Kind(ChatFormatting tooltipColor, String... tooltipKeys) {
            this.tooltipColor = tooltipColor;
            this.tooltipKeys = tooltipKeys;
        }

        public ChatFormatting tooltipColor() {
            return this.tooltipColor;
        }

        public String[] tooltipKeys() {
            return this.tooltipKeys;
        }
    }
}
