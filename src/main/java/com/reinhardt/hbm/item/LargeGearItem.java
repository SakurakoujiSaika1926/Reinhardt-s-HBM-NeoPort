package com.reinhardt.hbm.item;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/** Large gears keep the dedicated OBJ item renderer but are registered as fixed items. */
public final class LargeGearItem extends Item {
    public LargeGearItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        ObjMachineBlockItem.installRenderer(consumer);
    }
}
