package com.reinhardt.hbm.block;

import com.reinhardt.hbm.ReinhardtsHBM;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;

/** A normal ore with the explicit drop used by the legacy BlockOre. */
public final class DropOreBlock extends net.minecraft.world.level.block.Block {
    private final String dropId;

    public DropOreBlock(Properties properties, String dropId) {
        super(properties);
        this.dropId = dropId;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        Item item = BuiltInRegistries.ITEM.get(ReinhardtsHBM.id(this.dropId));
        return item == null || item == net.minecraft.world.item.Items.AIR
                ? List.of()
                : List.of(new ItemStack(item));
    }
}
