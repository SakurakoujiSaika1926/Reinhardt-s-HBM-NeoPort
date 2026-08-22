package com.reinhardt.hbm.item;

import com.reinhardt.hbm.client.render.HexafluorideTankItemRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/** Routes the two legacy hexafluoride tank items through ItemRenderBase-equivalent OBJ rendering. */
public final class HexafluorideTankBlockItem extends net.minecraft.world.item.BlockItem {
    public HexafluorideTankBlockItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private final HexafluorideTankItemRenderer renderer = new HexafluorideTankItemRenderer();

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return this.renderer;
            }
        });
    }
}
