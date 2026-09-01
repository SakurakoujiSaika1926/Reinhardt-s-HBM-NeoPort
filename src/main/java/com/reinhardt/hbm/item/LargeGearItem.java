package com.reinhardt.hbm.item;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/** The two 1.7.10 large-gear variants share one item and one dedicated OBJ renderer. */
public final class LargeGearItem extends LegacyVariantItem {
    public LargeGearItem(Item.Properties properties) {
        super(properties, "gear_large", variants("normal", "steel"));
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        ObjMachineBlockItem.installRenderer(consumer);
    }
}
