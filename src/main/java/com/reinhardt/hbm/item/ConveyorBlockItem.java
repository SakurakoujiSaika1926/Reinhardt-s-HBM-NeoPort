package com.reinhardt.hbm.item;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/** Registered for pick-block and commands; construction is performed by ConveyorWandItem. */
public final class ConveyorBlockItem extends BlockItem {
    public ConveyorBlockItem(Block block, Item.Properties properties) { super(block, properties); }
}
