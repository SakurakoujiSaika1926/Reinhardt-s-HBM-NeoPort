package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
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
        switch (BobbleheadBlockItem.type(stack)) {
            case NONE -> renderNoneItem(context, poseStack, bufferSource, packedLight, packedOverlay);
            case STRENGTH -> renderStrengthItem(context, poseStack, bufferSource, packedLight, packedOverlay);
            case PERCEPTION -> renderPerceptionItem(context, poseStack, bufferSource, packedLight, packedOverlay);
            case ENDURANCE -> renderEnduranceItem(context, poseStack, bufferSource, packedLight, packedOverlay);
            case CHARISMA -> renderCharismaItem(context, poseStack, bufferSource, packedLight, packedOverlay);
            case INTELLIGENCE -> renderIntelligenceItem(context, poseStack, bufferSource, packedLight, packedOverlay);
            case AGILITY -> renderAgilityItem(context, poseStack, bufferSource, packedLight, packedOverlay);
            case LUCK -> renderLuckItem(context, poseStack, bufferSource, packedLight, packedOverlay);
            case BOB -> renderBobItem(context, poseStack, bufferSource, packedLight, packedOverlay);
            case FRIZZLE -> renderFrizzleItem(context, poseStack, bufferSource, packedLight, packedOverlay);
            case PU238 -> renderPu238Item(context, poseStack, bufferSource, packedLight, packedOverlay);
            case VT -> renderVtItem(context, poseStack, bufferSource, packedLight, packedOverlay);
            case DOC -> renderDocItem(context, poseStack, bufferSource, packedLight, packedOverlay);
            case BLUEHAT -> renderBluehatItem(context, poseStack, bufferSource, packedLight, packedOverlay);
            case PHEO -> renderPheoItem(context, poseStack, bufferSource, packedLight, packedOverlay);
            case ADAM29 -> renderAdam29Item(context, poseStack, bufferSource, packedLight, packedOverlay);
            case UFFR -> renderUffrItem(context, poseStack, bufferSource, packedLight, packedOverlay);
            case VAER -> renderVaerItem(context, poseStack, bufferSource, packedLight, packedOverlay);
            case NOS -> renderNosItem(context, poseStack, bufferSource, packedLight, packedOverlay);
            case DRILLGON -> renderDrillgonItem(context, poseStack, bufferSource, packedLight, packedOverlay);
            case CIRNO -> renderCirnoItem(context, poseStack, bufferSource, packedLight, packedOverlay);
            case MICROWAVE -> renderMicrowaveItem(context, poseStack, bufferSource, packedLight, packedOverlay);
            case PEEP -> renderPeepItem(context, poseStack, bufferSource, packedLight, packedOverlay);
            case MELLOW -> renderMellowItem(context, poseStack, bufferSource, packedLight, packedOverlay);
            case ABEL -> renderAbelItem(context, poseStack, bufferSource, packedLight, packedOverlay);
        }
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
            poseStack.translate(0.0F, -3.5F, 0.0F);
            poseStack.scale(10.0F, 10.0F, 10.0F);
        }
        poseStack.scale(0.5F, 0.5F, 0.5F);
        BobbleheadBlockEntityRenderer.renderNoneItem(
                HbmBlocks.BOBBLEHEAD.get().defaultBlockState(), poseStack,
                bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderStrengthItem(ItemDisplayContext context, PoseStack poseStack,
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
            poseStack.translate(0.0F, -3.5F, 0.0F);
            poseStack.scale(10.0F, 10.0F, 10.0F);
        }
        poseStack.scale(0.5F, 0.5F, 0.5F);
        BobbleheadBlockEntityRenderer.renderStrengthItem(
                HbmBlocks.BOBBLEHEAD.get().defaultBlockState(), poseStack,
                bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderPerceptionItem(ItemDisplayContext context, PoseStack poseStack,
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
            poseStack.translate(0.0F, -3.5F, 0.0F);
            poseStack.scale(10.0F, 10.0F, 10.0F);
        }
        poseStack.scale(0.5F, 0.5F, 0.5F);
        BobbleheadBlockEntityRenderer.renderPerceptionItem(
                HbmBlocks.BOBBLEHEAD.get().defaultBlockState(), poseStack,
                bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderEnduranceItem(ItemDisplayContext context, PoseStack poseStack,
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
            poseStack.translate(0.0F, -3.5F, 0.0F);
            poseStack.scale(10.0F, 10.0F, 10.0F);
        }
        poseStack.scale(0.5F, 0.5F, 0.5F);
        BobbleheadBlockEntityRenderer.renderEnduranceItem(
                HbmBlocks.BOBBLEHEAD.get().defaultBlockState(), poseStack,
                bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderCharismaItem(ItemDisplayContext context, PoseStack poseStack,
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
            poseStack.translate(0.0F, -3.5F, 0.0F);
            poseStack.scale(10.0F, 10.0F, 10.0F);
        }
        poseStack.scale(0.5F, 0.5F, 0.5F);
        BobbleheadBlockEntityRenderer.renderCharismaItem(
                HbmBlocks.BOBBLEHEAD.get().defaultBlockState(), poseStack,
                bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderIntelligenceItem(ItemDisplayContext context, PoseStack poseStack,
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
            poseStack.translate(0.0F, -3.5F, 0.0F);
            poseStack.scale(10.0F, 10.0F, 10.0F);
        }
        poseStack.scale(0.5F, 0.5F, 0.5F);
        BobbleheadBlockEntityRenderer.renderIntelligenceItem(
                HbmBlocks.BOBBLEHEAD.get().defaultBlockState(), poseStack,
                bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderAgilityItem(ItemDisplayContext context, PoseStack poseStack,
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
            poseStack.translate(0.0F, -3.5F, 0.0F);
            poseStack.scale(10.0F, 10.0F, 10.0F);
        }
        poseStack.scale(0.5F, 0.5F, 0.5F);
        BobbleheadBlockEntityRenderer.renderAgilityItem(
                HbmBlocks.BOBBLEHEAD.get().defaultBlockState(), poseStack,
                bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderLuckItem(ItemDisplayContext context, PoseStack poseStack,
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
            poseStack.translate(0.0F, -3.5F, 0.0F);
            poseStack.scale(10.0F, 10.0F, 10.0F);
        }
        poseStack.scale(0.5F, 0.5F, 0.5F);
        BobbleheadBlockEntityRenderer.renderLuckItem(
                HbmBlocks.BOBBLEHEAD.get().defaultBlockState(), poseStack,
                bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderBobItem(ItemDisplayContext context, PoseStack poseStack,
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
            poseStack.translate(0.0F, -3.5F, 0.0F);
            poseStack.scale(10.0F, 10.0F, 10.0F);
        }
        poseStack.scale(0.5F, 0.5F, 0.5F);
        BobbleheadBlockEntityRenderer.renderBobItem(
                HbmBlocks.BOBBLEHEAD.get().defaultBlockState(), poseStack,
                bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderFrizzleItem(ItemDisplayContext context, PoseStack poseStack,
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
            poseStack.translate(0.0F, -3.5F, 0.0F);
            poseStack.scale(10.0F, 10.0F, 10.0F);
        }
        poseStack.scale(0.5F, 0.5F, 0.5F);
        BobbleheadBlockEntityRenderer.renderFrizzleItem(
                HbmBlocks.BOBBLEHEAD.get().defaultBlockState(), poseStack,
                bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderPu238Item(ItemDisplayContext context, PoseStack poseStack,
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
            poseStack.translate(0.0F, -3.5F, 0.0F);
            poseStack.scale(10.0F, 10.0F, 10.0F);
        }
        poseStack.scale(0.5F, 0.5F, 0.5F);
        BobbleheadBlockEntityRenderer.renderPu238Item(
                HbmBlocks.BOBBLEHEAD.get().defaultBlockState(), poseStack,
                bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderVtItem(ItemDisplayContext context, PoseStack poseStack,
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
            poseStack.translate(0.0F, -3.5F, 0.0F);
            poseStack.scale(10.0F, 10.0F, 10.0F);
        }
        poseStack.scale(0.5F, 0.5F, 0.5F);
        BobbleheadBlockEntityRenderer.renderVtItem(
                HbmBlocks.BOBBLEHEAD.get().defaultBlockState(), poseStack,
                bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderDocItem(ItemDisplayContext context, PoseStack poseStack,
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
            poseStack.translate(0.0F, -3.5F, 0.0F);
            poseStack.scale(10.0F, 10.0F, 10.0F);
        }
        poseStack.scale(0.5F, 0.5F, 0.5F);
        BobbleheadBlockEntityRenderer.renderDocItem(
                HbmBlocks.BOBBLEHEAD.get().defaultBlockState(), poseStack,
                bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderBluehatItem(ItemDisplayContext context, PoseStack poseStack,
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
            poseStack.translate(0.0F, -3.5F, 0.0F);
            poseStack.scale(10.0F, 10.0F, 10.0F);
        }
        poseStack.scale(0.5F, 0.5F, 0.5F);
        BobbleheadBlockEntityRenderer.renderBluehatItem(
                HbmBlocks.BOBBLEHEAD.get().defaultBlockState(), poseStack,
                bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderPheoItem(ItemDisplayContext context, PoseStack poseStack,
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
            poseStack.translate(0.0F, -3.5F, 0.0F);
            poseStack.scale(10.0F, 10.0F, 10.0F);
        }
        poseStack.scale(0.5F, 0.5F, 0.5F);
        BobbleheadBlockEntityRenderer.renderPheoItem(
                HbmBlocks.BOBBLEHEAD.get().defaultBlockState(), poseStack,
                bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderAdam29Item(ItemDisplayContext context, PoseStack poseStack,
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
            poseStack.translate(0.0F, -3.5F, 0.0F);
            poseStack.scale(10.0F, 10.0F, 10.0F);
        }
        poseStack.scale(0.5F, 0.5F, 0.5F);
        BobbleheadBlockEntityRenderer.renderAdam29Item(
                HbmBlocks.BOBBLEHEAD.get().defaultBlockState(), poseStack,
                bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderUffrItem(ItemDisplayContext context, PoseStack poseStack,
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
            poseStack.translate(0.0F, -3.5F, 0.0F);
            poseStack.scale(10.0F, 10.0F, 10.0F);
        }
        poseStack.scale(0.5F, 0.5F, 0.5F);
        BobbleheadBlockEntityRenderer.renderUffrItem(
                HbmBlocks.BOBBLEHEAD.get().defaultBlockState(), poseStack,
                bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderVaerItem(ItemDisplayContext context, PoseStack poseStack,
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
            poseStack.translate(0.0F, -3.5F, 0.0F);
            poseStack.scale(10.0F, 10.0F, 10.0F);
        }
        poseStack.scale(0.5F, 0.5F, 0.5F);
        BobbleheadBlockEntityRenderer.renderVaerItem(
                HbmBlocks.BOBBLEHEAD.get().defaultBlockState(), poseStack,
                bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderNosItem(ItemDisplayContext context, PoseStack poseStack,
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
            poseStack.translate(0.0F, -3.5F, 0.0F);
            poseStack.scale(10.0F, 10.0F, 10.0F);
        }
        poseStack.scale(0.5F, 0.5F, 0.5F);
        BobbleheadBlockEntityRenderer.renderNosItem(
                HbmBlocks.BOBBLEHEAD.get().defaultBlockState(), poseStack,
                bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderDrillgonItem(ItemDisplayContext context, PoseStack poseStack,
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
            poseStack.translate(0.0F, -3.5F, 0.0F);
            poseStack.scale(10.0F, 10.0F, 10.0F);
        }
        poseStack.scale(0.5F, 0.5F, 0.5F);
        BobbleheadBlockEntityRenderer.renderDrillgonItem(
                HbmBlocks.BOBBLEHEAD.get().defaultBlockState(), poseStack,
                bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderCirnoItem(ItemDisplayContext context, PoseStack poseStack,
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
            poseStack.translate(0.0F, -3.5F, 0.0F);
            poseStack.scale(10.0F, 10.0F, 10.0F);
        }
        poseStack.scale(0.5F, 0.5F, 0.5F);
        BobbleheadBlockEntityRenderer.renderCirnoItem(
                HbmBlocks.BOBBLEHEAD.get().defaultBlockState(), poseStack,
                bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderMicrowaveItem(ItemDisplayContext context, PoseStack poseStack,
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
            poseStack.translate(0.0F, -3.5F, 0.0F);
            poseStack.scale(10.0F, 10.0F, 10.0F);
        }
        poseStack.scale(0.5F, 0.5F, 0.5F);
        BobbleheadBlockEntityRenderer.renderMicrowaveItem(
                HbmBlocks.BOBBLEHEAD.get().defaultBlockState(), poseStack,
                bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderPeepItem(ItemDisplayContext context, PoseStack poseStack,
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
            poseStack.translate(0.0F, -3.5F, 0.0F);
            poseStack.scale(10.0F, 10.0F, 10.0F);
        }
        poseStack.scale(0.5F, 0.5F, 0.5F);
        BobbleheadBlockEntityRenderer.renderPeepItem(
                HbmBlocks.BOBBLEHEAD.get().defaultBlockState(), poseStack,
                bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderMellowItem(ItemDisplayContext context, PoseStack poseStack,
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
            poseStack.translate(0.0F, -3.5F, 0.0F);
            poseStack.scale(10.0F, 10.0F, 10.0F);
        }
        poseStack.scale(0.5F, 0.5F, 0.5F);
        BobbleheadBlockEntityRenderer.renderMellowItem(
                HbmBlocks.BOBBLEHEAD.get().defaultBlockState(), poseStack,
                bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderAbelItem(ItemDisplayContext context, PoseStack poseStack,
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
            poseStack.translate(0.0F, -3.5F, 0.0F);
            poseStack.scale(10.0F, 10.0F, 10.0F);
        }
        poseStack.scale(0.5F, 0.5F, 0.5F);
        BobbleheadBlockEntityRenderer.renderAbelItem(
                HbmBlocks.BOBBLEHEAD.get().defaultBlockState(), poseStack,
                bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }


}
