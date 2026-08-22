package com.reinhardt.hbm.item;

import com.reinhardt.hbm.block.RbmkComponentBlock;
import com.reinhardt.hbm.client.render.RbmkFuelChannelItemRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

public class RbmkFuelChannelBlockItem extends BlockItem {
    private final RbmkComponentBlock.Kind kind;

    public RbmkFuelChannelBlockItem(Block block, Item.Properties properties, RbmkComponentBlock.Kind kind) {
        super(block, properties);
        this.kind = kind;
    }

    public RbmkComponentBlock.Kind kind() {
        return kind;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private final RbmkFuelChannelItemRenderer renderer = new RbmkFuelChannelItemRenderer();

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return this.renderer;
            }
        });
    }
}
