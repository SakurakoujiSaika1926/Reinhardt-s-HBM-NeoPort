package com.reinhardt.hbm.block;

import com.reinhardt.hbm.ReinhardtsHBM;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;

/** Depth ore drops from the 1.7.10 BlockDepthOre table. */
public final class DepthOreBlock extends DepthRockBlock {
    private final String dropId;
    private final int minimum;
    private final int range;

    public DepthOreBlock(Properties properties, String dropId, int minimum, int range) {
        super(properties);
        this.dropId = dropId;
        this.minimum = minimum;
        this.range = range;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        Item item = BuiltInRegistries.ITEM.get(ReinhardtsHBM.id(this.dropId));
        if (item == null || item == net.minecraft.world.item.Items.AIR) {
            return List.of();
        }
        int count = this.minimum + (this.range <= 1 ? 0 : params.getLevel().random.nextInt(this.range));
        return List.of(new ItemStack(item, count));
    }
}
