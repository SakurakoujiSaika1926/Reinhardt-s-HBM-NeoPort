package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.item.PlushieBlockItem;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

public final class PlushieItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final IClientItemExtensions EXTENSION = new IClientItemExtensions() {
        private final PlushieItemRenderer renderer = new PlushieItemRenderer();

        @Override
        public BlockEntityWithoutLevelRenderer getCustomRenderer() {
            return renderer;
        }
    };

    public PlushieItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    public static IClientItemExtensions clientExtension() {
        return EXTENSION;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        switch (PlushieBlockItem.type(stack)) {
            case YOMI -> renderYomiItem(context, poseStack, bufferSource, packedLight, packedOverlay);
            case NUMBER_NINE -> renderNumberNineItem(context, poseStack, bufferSource, packedLight, packedOverlay);
            case HUNDUN -> renderHundunItem(context, poseStack, bufferSource, packedLight, packedOverlay);
            case DERG -> renderDergItem(context, poseStack, bufferSource, packedLight, packedOverlay);
            case NONE -> renderNoneItem(context, poseStack, bufferSource, packedLight, packedOverlay);
        }
    }

    private static void renderYomiItem(ItemDisplayContext context, PoseStack poseStack,
                                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
        } else {
            poseStack.translate(0.5F, 0.25F, 0.0F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (context == ItemDisplayContext.GUI) {
            poseStack.translate(0.0F, -6.0F, 0.0F);
            poseStack.scale(6.0F, 6.0F, 6.0F);
        }
        poseStack.translate(0.0F, 0.25F, 0.0F);
        poseStack.scale(1.25F, 1.25F, 1.25F);
        PlushieBlockEntityRenderer.renderYomiItem(HbmBlocks.PLUSHIE.get().defaultBlockState(), poseStack,
                bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderNumberNineItem(ItemDisplayContext context, PoseStack poseStack,
                                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
        } else {
            poseStack.translate(0.5F, 0.25F, 0.0F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (context == ItemDisplayContext.GUI) {
            poseStack.translate(0.0F, -6.0F, 0.0F);
            poseStack.scale(6.0F, 6.0F, 6.0F);
        }
        poseStack.translate(0.0F, 0.25F, 0.0F);
        poseStack.translate(0.0F, 0.25F, 0.25F);
        poseStack.scale(1.25F, 1.25F, 1.25F);
        PlushieBlockEntityRenderer.renderNumberNineItem(HbmBlocks.PLUSHIE.get().defaultBlockState(), poseStack,
                bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderHundunItem(ItemDisplayContext context, PoseStack poseStack,
                                         MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
        } else {
            poseStack.translate(0.5F, 0.25F, 0.0F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (context == ItemDisplayContext.GUI) {
            poseStack.translate(0.0F, -6.0F, 0.0F);
            poseStack.scale(6.0F, 6.0F, 6.0F);
        }
        poseStack.translate(0.0F, 0.25F, 0.0F);
        poseStack.translate(0.5F, 0.5F, 0.0F);
        poseStack.scale(1.25F, 1.25F, 1.25F);
        PlushieBlockEntityRenderer.renderHundunItem(HbmBlocks.PLUSHIE.get().defaultBlockState(), poseStack,
                bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderDergItem(ItemDisplayContext context, PoseStack poseStack,
                                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
        } else {
            poseStack.translate(0.5F, 0.25F, 0.0F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (context == ItemDisplayContext.GUI) {
            poseStack.translate(0.0F, -6.0F, 0.0F);
            poseStack.scale(6.0F, 6.0F, 6.0F);
        }
        poseStack.translate(0.0F, 0.25F, 0.0F);
        poseStack.scale(1.5F, 1.5F, 1.5F);
        PlushieBlockEntityRenderer.renderDergItem(HbmBlocks.PLUSHIE.get().defaultBlockState(), poseStack,
                bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderNoneItem(ItemDisplayContext context, PoseStack poseStack,
                                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
        } else {
            poseStack.translate(0.5F, 0.25F, 0.0F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (context == ItemDisplayContext.GUI) {
            poseStack.translate(0.0F, -6.0F, 0.0F);
            poseStack.scale(6.0F, 6.0F, 6.0F);
        }
        poseStack.translate(0.0F, 0.25F, 0.0F);
        PlushieBlockEntityRenderer.renderNoneItem(HbmBlocks.PLUSHIE.get().defaultBlockState(), poseStack,
                bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }
}
