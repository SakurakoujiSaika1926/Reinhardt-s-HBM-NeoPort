package com.reinhardt.hbm.block;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;

/** The three metadata variants of the 1.7.10 Glyphid hive base block. */
public final class GlyphidBaseBlock extends LegacyVariantBlock {
    public GlyphidBaseBlock(Properties properties) {
        super(properties, 2);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of();
    }
}
