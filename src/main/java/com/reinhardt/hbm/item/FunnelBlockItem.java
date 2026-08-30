package com.reinhardt.hbm.item;

import com.reinhardt.hbm.client.render.FunnelBlockEntityRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/** Uses the three-part MachineFunnel OBJ inventory renderer from 1.7.10. */
public final class FunnelBlockItem extends BlockItem {
    public FunnelBlockItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private final BlockEntityWithoutLevelRenderer renderer = new FunnelItemRenderer();

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return this.renderer;
            }
        });
    }

    private static final class FunnelItemRenderer extends BlockEntityWithoutLevelRenderer {
        private FunnelItemRenderer() {
            super(net.minecraft.client.Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                    net.minecraft.client.Minecraft.getInstance().getEntityModels());
        }

        @Override
        public void renderByItem(net.minecraft.world.item.ItemStack stack,
                                 net.minecraft.world.item.ItemDisplayContext context,
                                 com.mojang.blaze3d.vertex.PoseStack poseStack,
                                 net.minecraft.client.renderer.MultiBufferSource bufferSource,
                                 int packedLight, int packedOverlay) {
            FunnelBlockEntityRenderer.renderItem(stack, context, poseStack, bufferSource, packedLight, packedOverlay);
        }
    }
}
