package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.BobbleheadType;
import com.reinhardt.hbm.item.BobbleheadBlockItem;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/** Exact ItemRenderLibrary bobble transform: inventory y=-3.5, x10 then common x0.5. */
public final class BobbleheadItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final IClientItemExtensions EXTENSION = new IClientItemExtensions() {
        private final BobbleheadItemRenderer renderer = new BobbleheadItemRenderer();

        @Override
        public BlockEntityWithoutLevelRenderer getCustomRenderer() {
            return renderer;
        }
    };

    public BobbleheadItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    public static IClientItemExtensions clientExtension() {
        return EXTENSION;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BobbleheadType type = BobbleheadBlockItem.type(stack);
        poseStack.pushPose();
        if (context == ItemDisplayContext.GUI) {
            poseStack.translate(0.0F, -3.5F, 0.0F);
            poseStack.scale(10.0F, 10.0F, 10.0F);
        }
        poseStack.scale(0.5F, 0.5F, 0.5F);
        BobbleheadBlockEntityRenderer.renderItem(type, HbmBlocks.BOBBLEHEAD.get().defaultBlockState(), poseStack,
                bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }
}
