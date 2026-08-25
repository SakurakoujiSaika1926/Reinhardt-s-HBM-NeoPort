package com.reinhardt.hbm.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/** Preserves the legacy placement offset while using the complete OBJ item renderer. */
public final class ObjMachineLegacyOffsetBlockItem extends LegacyOffsetBlockItem {
    public ObjMachineLegacyOffsetBlockItem(Block block, Item.Properties properties, int legacyOffset, boolean placeCoreBehindPlayer) {
        super(block, properties, legacyOffset, placeCoreBehindPlayer);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        ObjMachineBlockItem.installRenderer(consumer);
    }
}
