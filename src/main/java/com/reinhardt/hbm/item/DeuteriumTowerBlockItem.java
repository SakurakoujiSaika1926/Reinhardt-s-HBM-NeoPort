package com.reinhardt.hbm.item;

import com.reinhardt.hbm.client.render.DeuteriumTowerBlockEntityRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/** ItemRenderLibrary entry for the deuterium extraction tower. */
public final class DeuteriumTowerBlockItem extends BlockItem {
    public DeuteriumTowerBlockItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private final BlockEntityWithoutLevelRenderer renderer = new DeuteriumTowerItemRenderer();

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return renderer;
            }
        });
    }

    private static final class DeuteriumTowerItemRenderer extends BlockEntityWithoutLevelRenderer {
        private DeuteriumTowerItemRenderer() {
            super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        }

        @Override
        public void renderByItem(ItemStack stack, ItemDisplayContext context, com.mojang.blaze3d.vertex.PoseStack poseStack,
                                 MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
            DeuteriumTowerBlockEntityRenderer.renderItem(context, poseStack, bufferSource, packedLight, packedOverlay);
        }
    }
}
