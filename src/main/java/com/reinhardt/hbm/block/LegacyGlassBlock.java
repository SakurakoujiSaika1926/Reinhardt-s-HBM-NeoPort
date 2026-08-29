package com.reinhardt.hbm.block;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;

/** Port of HBM's non-opaque BlockNTMGlass/BlockNTMGlassCT. */
public class LegacyGlassBlock extends Block {
    private final boolean dropsSelf;

    public LegacyGlassBlock(Properties properties, boolean dropsSelf) {
        super(properties);
        this.dropsSelf = dropsSelf;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return dropsSelf ? super.getDrops(state, params) : List.of();
    }

}
