package com.reinhardt.hbm.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;
import java.util.function.Supplier;

/** Legacy kept a distinct full pink-wood slab id; it always drops two half slabs. */
public final class PinkDoubleSlabBlock extends Block {
    private final Supplier<? extends Block> halfSlab;

    public PinkDoubleSlabBlock(Properties properties, Supplier<? extends Block> halfSlab) {
        super(properties);
        this.halfSlab = halfSlab;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(halfSlab.get(), 2);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(new ItemStack(halfSlab.get(), 2));
    }
}
