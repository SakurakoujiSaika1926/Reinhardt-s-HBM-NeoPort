package com.reinhardt.hbm.item;

import com.reinhardt.hbm.block.RbmkComponentBlock;
import com.reinhardt.hbm.client.render.RbmkComponentItemRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

public class RbmkComponentBlockItem extends BlockItem {
    private final RbmkComponentBlock.Kind kind;

    public RbmkComponentBlockItem(Block block, Item.Properties properties, RbmkComponentBlock.Kind kind) {
        super(block, properties);
        this.kind = kind;
    }

    public RbmkComponentBlock.Kind kind() {
        return this.kind;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private final RbmkComponentItemRenderer renderer = new RbmkComponentItemRenderer();

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return this.renderer;
            }
        });
    }
}
