package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.item.SnowglobeBlockItem;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

public final class SnowglobeItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final IClientItemExtensions EXTENSION = new IClientItemExtensions() {
        private final SnowglobeItemRenderer renderer = new SnowglobeItemRenderer();

        @Override
        public BlockEntityWithoutLevelRenderer getCustomRenderer() {
            return renderer;
        }
    };

    public SnowglobeItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    public static IClientItemExtensions clientExtension() {
        return EXTENSION;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        switch (SnowglobeBlockItem.type(stack)) {
            case RIVET_CITY -> renderRivetCityItem(context, poseStack, bufferSource, packedLight, packedOverlay);
            case TENPENNY_TOWER -> renderTenpennyTowerItem(context, poseStack, bufferSource, packedLight, packedOverlay);
            case LUCKY_38 -> renderLucky38Item(context, poseStack, bufferSource, packedLight, packedOverlay);
            case SIERRA_MADRE -> renderSierraMadreItem(context, poseStack, bufferSource, packedLight, packedOverlay);
            case PRYDWEN -> renderPrydwenItem(context, poseStack, bufferSource, packedLight, packedOverlay);
            case NONE -> renderNoneItem(context, poseStack, bufferSource, packedLight, packedOverlay);
        }
    }

    private static void renderRivetCityItem(ItemDisplayContext context, PoseStack poseStack,
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
            poseStack.translate(0.0F, -2.0F, 0.0F);
            poseStack.scale(6.0F, 6.0F, 6.0F);
        }
        poseStack.translate(0.0F, 0.25F, 0.0F);
        poseStack.scale(3.0F, 3.0F, 3.0F);
        SnowglobeBlockEntityRenderer.renderRivetCityItem(HbmBlocks.SNOWGLOBE.get().defaultBlockState(), poseStack,
                bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderTenpennyTowerItem(ItemDisplayContext context, PoseStack poseStack,
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
            poseStack.translate(0.0F, -2.0F, 0.0F);
            poseStack.scale(6.0F, 6.0F, 6.0F);
        }
        poseStack.translate(0.0F, 0.25F, 0.0F);
        poseStack.scale(3.0F, 3.0F, 3.0F);
        SnowglobeBlockEntityRenderer.renderTenpennyTowerItem(HbmBlocks.SNOWGLOBE.get().defaultBlockState(), poseStack,
                bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderLucky38Item(ItemDisplayContext context, PoseStack poseStack,
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
            poseStack.translate(0.0F, -2.0F, 0.0F);
            poseStack.scale(6.0F, 6.0F, 6.0F);
        }
        poseStack.translate(0.0F, 0.25F, 0.0F);
        poseStack.scale(3.0F, 3.0F, 3.0F);
        SnowglobeBlockEntityRenderer.renderLucky38Item(HbmBlocks.SNOWGLOBE.get().defaultBlockState(), poseStack,
                bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderSierraMadreItem(ItemDisplayContext context, PoseStack poseStack,
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
            poseStack.translate(0.0F, -2.0F, 0.0F);
            poseStack.scale(6.0F, 6.0F, 6.0F);
        }
        poseStack.translate(0.0F, 0.25F, 0.0F);
        poseStack.scale(3.0F, 3.0F, 3.0F);
        SnowglobeBlockEntityRenderer.renderSierraMadreItem(HbmBlocks.SNOWGLOBE.get().defaultBlockState(), poseStack,
                bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderPrydwenItem(ItemDisplayContext context, PoseStack poseStack,
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
            poseStack.translate(0.0F, -2.0F, 0.0F);
            poseStack.scale(6.0F, 6.0F, 6.0F);
        }
        poseStack.translate(0.0F, 0.25F, 0.0F);
        poseStack.scale(3.0F, 3.0F, 3.0F);
        SnowglobeBlockEntityRenderer.renderPrydwenItem(HbmBlocks.SNOWGLOBE.get().defaultBlockState(), poseStack,
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
            poseStack.translate(0.0F, -2.0F, 0.0F);
            poseStack.scale(6.0F, 6.0F, 6.0F);
        }
        poseStack.translate(0.0F, 0.25F, 0.0F);
        poseStack.scale(3.0F, 3.0F, 3.0F);
        SnowglobeBlockEntityRenderer.renderNoneItem(HbmBlocks.SNOWGLOBE.get().defaultBlockState(), poseStack,
                bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }
}
