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

/** Renders the metadata-bearing bobblehead with the legacy ItemRenderBase pose. */
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
        // ItemRenderLibrary supplied a 1/16 base transform before the bobble's
        // inventory/common transforms. Without it the old scale(10) inventory
        // adjustment is applied directly to block-sized OBJ coordinates.
        LegacyMachineItemRenderer.applyItemRenderBasePose(context, poseStack);
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
