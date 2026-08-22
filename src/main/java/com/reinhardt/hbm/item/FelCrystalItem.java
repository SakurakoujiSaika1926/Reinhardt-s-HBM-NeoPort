package com.reinhardt.hbm.item;

import com.reinhardt.hbm.util.Wavelength;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class FelCrystalItem extends Item {
    private final Wavelength wavelength;

    public FelCrystalItem(Properties properties, Wavelength wavelength) {
        super(properties.stacksTo(1));
        this.wavelength = wavelength == null ? Wavelength.NULL : wavelength;
    }

    public Wavelength wavelength() {
        return this.wavelength;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(this.getDescriptionId() + ".desc"));
        tooltip.add(Component.translatable(this.wavelength.translationKey()).withStyle(this.wavelength.textColor())
                .append(" - ")
                .append(Component.translatable(this.wavelength.rangeKey()).withStyle(this.wavelength.textColor())));
    }
}
