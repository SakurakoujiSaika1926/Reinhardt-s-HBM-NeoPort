package com.reinhardt.hbm.item;

import com.reinhardt.hbm.block.PowerPylonBlock;
import com.reinhardt.hbm.client.render.PowerPylonItemRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/** Keeps the multi-block pylon OBJ out of the vanilla block-item transform path. */
public final class PowerPylonBlockItem extends BlockItem {
    private final PowerPylonBlock.Kind kind;

    public PowerPylonBlockItem(Block block, Item.Properties properties, PowerPylonBlock.Kind kind) {
        super(block, properties);
        this.kind = kind;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private final PowerPylonItemRenderer renderer = new PowerPylonItemRenderer(kind);

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return renderer;
            }
        });
    }
}
