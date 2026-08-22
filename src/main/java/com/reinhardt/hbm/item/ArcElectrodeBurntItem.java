package com.reinhardt.hbm.item;

import net.minecraft.world.item.Item;

public final class ArcElectrodeBurntItem extends LegacyVariantItem {
    public ArcElectrodeBurntItem(Properties properties) {
        super(properties.stacksTo(1), "arc_electrode_burnt", variants(
                "graphite", "lanthanium", "desh", "saturnite"
        ));
    }
}
