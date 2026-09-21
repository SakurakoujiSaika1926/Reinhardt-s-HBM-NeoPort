package com.reinhardt.hbm.item;

import net.minecraft.world.item.Item;

public final class ArcElectrodeBurntItem extends Item {
    private final String variantId;

    public ArcElectrodeBurntItem(Properties properties, String variantId) {
        super(properties.stacksTo(1));
        this.variantId = variantId;
    }

    public String variantId() {
        return this.variantId;
    }
}
