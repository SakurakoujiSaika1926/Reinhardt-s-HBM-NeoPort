package com.reinhardt.hbm.item;

import com.reinhardt.hbm.client.render.SteamEngineItemRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

public class SteamEngineBlockItem extends LegacyOffsetBlockItem {
    public SteamEngineBlockItem(Block block, Item.Properties properties) {
        super(block, properties, 1, true);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private final SteamEngineItemRenderer renderer = new SteamEngineItemRenderer();

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return this.renderer;
            }
        });
    }
}
