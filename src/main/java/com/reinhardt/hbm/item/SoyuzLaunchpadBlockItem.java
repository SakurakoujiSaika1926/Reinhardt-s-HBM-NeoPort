package com.reinhardt.hbm.item;

import com.reinhardt.hbm.client.render.SoyuzLaunchpadItemRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/** Latest-old two-block placement offset plus its dedicated static OBJ item renderer. */
public final class SoyuzLaunchpadBlockItem extends LegacyOffsetBlockItem {
    public SoyuzLaunchpadBlockItem(Block block, Item.Properties properties) {
        super(block, properties, 2, true);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private final SoyuzLaunchpadItemRenderer renderer = new SoyuzLaunchpadItemRenderer();

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return this.renderer;
            }
        });
    }
}
