package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.block.PowerPylonBlock;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Renders every legacy pylon item with its own verified 1.7.10 pose sequence. */
public final class PowerPylonItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ModelResourceLocation RED_CONNECTOR =
            MachineModelRenderer.standalone("block/red_connector_world");
    private static final ModelResourceLocation CONNECTOR_RED_SUPER =
            MachineModelRenderer.standalone("block/connector_red_super_world");
    private static final ModelResourceLocation SMALL_STEEL =
            MachineModelRenderer.standalone("block/red_pylon_small_steel_world");
    private static final ModelResourceLocation MEDIUM_WOOD =
            MachineModelRenderer.standalone("block/red_pylon_medium_wood_world");
    private static final ModelResourceLocation MEDIUM_WOOD_TRANSFORMER =
            MachineModelRenderer.standalone("block/red_pylon_medium_transformer_world");
    private static final ModelResourceLocation MEDIUM_STEEL =
            MachineModelRenderer.standalone("block/red_pylon_steel_world");
    private static final ModelResourceLocation MEDIUM_STEEL_TRANSFORMER =
            MachineModelRenderer.standalone("block/red_pylon_steel_transformer_world");
    private static final ModelResourceLocation LARGE =
            MachineModelRenderer.standalone("block/red_pylon_large_world");
    private static final ModelResourceLocation SUBSTATION =
            MachineModelRenderer.standalone("block/substation_world");
    private final PowerPylonBlock.Kind kind;

    public PowerPylonItemRenderer(PowerPylonBlock.Kind kind) {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        this.kind = kind;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        switch (this.kind) {
            case RED_CONNECTOR -> renderRedConnector(context, poseStack, bufferSource, packedLight, packedOverlay);
            case CONNECTOR_RED_SUPER -> renderConnectorRedSuper(context, poseStack, bufferSource, packedLight, packedOverlay);
            case RED_PYLON -> throw new IllegalStateException("Legacy red pylon uses its registered flat item model");
            case RED_PYLON_STEEL -> renderRedPylonSteel(context, poseStack, bufferSource, packedLight, packedOverlay);
            case RED_PYLON_MEDIUM_WOOD -> renderRedPylonMediumWood(context, poseStack, bufferSource, packedLight, packedOverlay);
            case RED_PYLON_MEDIUM_WOOD_TRANSFORMER -> renderRedPylonMediumWoodTransformer(context, poseStack, bufferSource, packedLight, packedOverlay);
            case RED_PYLON_MEDIUM_STEEL -> renderRedPylonMediumSteel(context, poseStack, bufferSource, packedLight, packedOverlay);
            case RED_PYLON_MEDIUM_STEEL_TRANSFORMER -> renderRedPylonMediumSteelTransformer(context, poseStack, bufferSource, packedLight, packedOverlay);
            case RED_PYLON_LARGE -> renderRedPylonLarge(context, poseStack, bufferSource, packedLight, packedOverlay);
            case SUBSTATION -> renderSubstation(context, poseStack, bufferSource, packedLight, packedOverlay);
        }
    }

    private static void renderRedConnector(ItemDisplayContext context, PoseStack poseStack,
                                           MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        applyRedConnectorItemPose(context, poseStack);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(RED_CONNECTOR), poseStack,
                bufferSource, HbmBlocks.RED_CONNECTOR.get().defaultBlockState(), packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void applyRedConnectorItemPose(ItemDisplayContext context, PoseStack poseStack) {
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
            poseStack.translate(0.0F, -3.5F, 0.0F);
            poseStack.scale(7.0F, 7.0F, 7.0F);
        } else {
            poseStack.translate(0.5F, 0.25F, 0.0F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        poseStack.scale(2.0F, 2.0F, 2.0F);
    }

    private static void renderConnectorRedSuper(ItemDisplayContext context, PoseStack poseStack,
                                                MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        applyConnectorRedSuperItemPose(context, poseStack);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(CONNECTOR_RED_SUPER), poseStack,
                bufferSource, HbmBlocks.CONNECTOR_RED_SUPER.get().defaultBlockState(), packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void applyConnectorRedSuperItemPose(ItemDisplayContext context, PoseStack poseStack) {
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
            poseStack.translate(0.0F, -5.0F, 0.0F);
            poseStack.scale(7.0F, 7.0F, 7.0F);
        } else {
            poseStack.translate(0.5F, 0.25F, 0.0F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        poseStack.scale(2.0F, 2.0F, 2.0F);
    }

    private static void renderRedPylonSteel(ItemDisplayContext context, PoseStack poseStack,
                                            MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        applyRedPylonSteelItemPose(context, poseStack);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(SMALL_STEEL), poseStack,
                bufferSource, HbmBlocks.RED_PYLON_STEEL.get().defaultBlockState(), packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void applyRedPylonSteelItemPose(ItemDisplayContext context, PoseStack poseStack) {
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
            poseStack.translate(0.0F, -5.0F, 0.0F);
            poseStack.scale(2.9F, 2.9F, 2.9F);
        } else {
            poseStack.translate(0.5F, 0.25F, 0.0F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
    }

    private static void renderRedPylonMediumWood(ItemDisplayContext context, PoseStack poseStack,
                                                MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        applyRedPylonMediumWoodItemPose(context, poseStack);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(MEDIUM_WOOD), poseStack,
                bufferSource, HbmBlocks.RED_PYLON_MEDIUM_WOOD.get().defaultBlockState(), packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void applyRedPylonMediumWoodItemPose(ItemDisplayContext context, PoseStack poseStack) {
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
            poseStack.translate(1.0F, -5.0F, 0.0F);
            poseStack.scale(4.5F, 4.5F, 4.5F);
        } else {
            poseStack.translate(0.5F, 0.25F, 0.0F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.scale(0.5F, 0.5F, 0.5F);
        poseStack.translate(0.75F, 0.0F, 0.0F);
    }

    private static void renderRedPylonMediumWoodTransformer(ItemDisplayContext context, PoseStack poseStack,
                                                           MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        applyRedPylonMediumWoodTransformerItemPose(context, poseStack);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(MEDIUM_WOOD_TRANSFORMER), poseStack,
                bufferSource, HbmBlocks.RED_PYLON_MEDIUM_WOOD_TRANSFORMER.get().defaultBlockState(), packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void applyRedPylonMediumWoodTransformerItemPose(ItemDisplayContext context, PoseStack poseStack) {
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
            poseStack.translate(1.0F, -5.0F, 0.0F);
            poseStack.scale(4.5F, 4.5F, 4.5F);
        } else {
            poseStack.translate(0.5F, 0.25F, 0.0F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.scale(0.5F, 0.5F, 0.5F);
        poseStack.translate(0.75F, 0.0F, 0.0F);
    }

    private static void renderRedPylonMediumSteel(ItemDisplayContext context, PoseStack poseStack,
                                                 MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        applyRedPylonMediumSteelItemPose(context, poseStack);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(MEDIUM_STEEL), poseStack,
                bufferSource, HbmBlocks.RED_PYLON_MEDIUM_STEEL.get().defaultBlockState(), packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void applyRedPylonMediumSteelItemPose(ItemDisplayContext context, PoseStack poseStack) {
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
            poseStack.translate(1.0F, -5.0F, 0.0F);
            poseStack.scale(4.5F, 4.5F, 4.5F);
        } else {
            poseStack.translate(0.5F, 0.25F, 0.0F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.scale(0.5F, 0.5F, 0.5F);
        poseStack.translate(0.75F, 0.0F, 0.0F);
    }

    private static void renderRedPylonMediumSteelTransformer(ItemDisplayContext context, PoseStack poseStack,
                                                            MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        applyRedPylonMediumSteelTransformerItemPose(context, poseStack);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(MEDIUM_STEEL_TRANSFORMER), poseStack,
                bufferSource, HbmBlocks.RED_PYLON_MEDIUM_STEEL_TRANSFORMER.get().defaultBlockState(), packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void applyRedPylonMediumSteelTransformerItemPose(ItemDisplayContext context, PoseStack poseStack) {
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
            poseStack.translate(1.0F, -5.0F, 0.0F);
            poseStack.scale(4.5F, 4.5F, 4.5F);
        } else {
            poseStack.translate(0.5F, 0.25F, 0.0F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.scale(0.5F, 0.5F, 0.5F);
        poseStack.translate(0.75F, 0.0F, 0.0F);
    }

    private static void renderRedPylonLarge(ItemDisplayContext context, PoseStack poseStack,
                                           MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        applyRedPylonLargeItemPose(context, poseStack);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(LARGE), poseStack,
                bufferSource, HbmBlocks.RED_PYLON_LARGE.get().defaultBlockState(), packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void applyRedPylonLargeItemPose(ItemDisplayContext context, PoseStack poseStack) {
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
            poseStack.translate(0.0F, -5.0F, 0.0F);
            poseStack.scale(2.25F, 2.25F, 2.25F);
        } else {
            poseStack.translate(0.5F, 0.25F, 0.0F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        poseStack.scale(0.5F, 0.5F, 0.5F);
    }

    private static void renderSubstation(ItemDisplayContext context, PoseStack poseStack,
                                         MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        applySubstationItemPose(context, poseStack);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(SUBSTATION), poseStack,
                bufferSource, HbmBlocks.SUBSTATION.get().defaultBlockState(), packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void applySubstationItemPose(ItemDisplayContext context, PoseStack poseStack) {
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
            poseStack.translate(0.0F, -2.5F, 0.0F);
            poseStack.scale(4.5F, 4.5F, 4.5F);
        } else {
            poseStack.translate(0.5F, 0.25F, 0.0F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        poseStack.scale(0.5F, 0.5F, 0.5F);
    }
}
