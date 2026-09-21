package com.reinhardt.hbm.block;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.ArrayList;
import java.util.List;

public final class JungleCrateBlock extends Block {
    public JungleCrateBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = new ArrayList<>();
        drops.add(new ItemStack(Items.GOLD_INGOT, 4 + params.getLevel().random.nextInt(4)));
        drops.add(new ItemStack(Items.GOLD_NUGGET, 8 + params.getLevel().random.nextInt(10)));
        add(drops, CrateBlockSupport.stack("powder_gold", 2 + params.getLevel().random.nextInt(3)));
        add(drops, CrateBlockSupport.stack("wire_gold", 4 + params.getLevel().random.nextInt(5)));
        add(drops, CrateBlockSupport.stack("wire_dense_gold", 1 + params.getLevel().random.nextInt(2)));
        if (params.getLevel().random.nextInt(2) == 0) {
            add(drops, CrateBlockSupport.stack("plate_gold", 1 + params.getLevel().random.nextInt(2)));
        }
        if (params.getLevel().random.nextInt(3) == 0) {
            add(drops, CrateBlockSupport.stack("crystal_gold"));
        }
        return drops;
    }

    private static void add(List<ItemStack> drops, ItemStack stack) {
        if (!stack.isEmpty()) {
            drops.add(stack);
        }
    }
}
