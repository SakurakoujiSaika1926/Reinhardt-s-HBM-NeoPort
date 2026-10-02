package com.reinhardt.hbm.block;

import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;

/** Huge radioactive mushroom cap/stem drop behavior from 1.7.10 BlockMushHuge. */
public final class MushHugeBlock extends Block {
    public MushHugeBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        RandomSource random = params.getLevel().random;
        int count = Math.max(0, random.nextInt(10) - 7);
        return count == 0 ? List.of() : List.of(new ItemStack(HbmBlocks.MUSH.get(), count));
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(HbmBlocks.MUSH.get());
    }
}
