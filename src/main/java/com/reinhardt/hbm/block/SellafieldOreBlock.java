package com.reinhardt.hbm.block;

import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;
import java.util.function.Supplier;

/** Sellafield ore keeps the exact 1.7.10 drop identity rather than using a generic ore fallback. */
public final class SellafieldOreBlock extends Block {
    public enum Drop {
        DIAMOND(() -> Items.DIAMOND),
        EMERALD(() -> Items.EMERALD),
        URANIUM_SCORCHED(null),
        SCHRABIDIUM(null),
        RAD_GEM(HbmItems.GEM_RAD);

        private final Supplier<? extends Item> itemSupplier;

        Drop(Supplier<? extends Item> itemSupplier) {
            this.itemSupplier = itemSupplier;
        }
    }

    private final Drop drop;

    public SellafieldOreBlock(Properties properties, Drop drop) {
        super(properties);
        this.drop = drop;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return drop.itemSupplier == null ? super.getDrops(state, params) : List.of(new ItemStack(drop.itemSupplier.get()));
    }
}
