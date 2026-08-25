package com.reinhardt.hbm.item;

import com.reinhardt.hbm.client.render.LegacyTankItemRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/** Uses the fixed 1.7.10 item transforms for the two large legacy tanks. */
public final class LegacyTankBlockItem extends BlockItem {
    private final LegacyTankItemRenderer.Kind kind;

    public LegacyTankBlockItem(Block block, Item.Properties properties, LegacyTankItemRenderer.Kind kind) {
        super(block, properties);
        this.kind = kind;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (this.kind == LegacyTankItemRenderer.Kind.BIG_ASS_TANK
                && getBlock() instanceof com.reinhardt.hbm.block.LargeFluidTankBlock tank) {
            return tank.placeFromLegacyAnchor(context);
        }
        return super.useOn(context);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private final LegacyTankItemRenderer renderer = new LegacyTankItemRenderer(kind);

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return renderer;
            }
        });
    }
}
