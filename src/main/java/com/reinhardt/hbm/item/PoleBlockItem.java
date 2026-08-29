package com.reinhardt.hbm.item;

import com.reinhardt.hbm.client.render.PoleItemRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/** Block item that renders the complete 1.7.10 pole model in every item context. */
public final class PoleBlockItem extends BlockItem {
    private final boolean satellite;

    public PoleBlockItem(Block block, Item.Properties properties, boolean satellite) {
        super(block, properties);
        this.satellite = satellite;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private final PoleItemRenderer renderer = new PoleItemRenderer(satellite);

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return renderer;
            }
        });
    }
}
