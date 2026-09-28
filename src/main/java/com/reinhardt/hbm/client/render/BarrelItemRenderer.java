package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.ReinhardtsHBM;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Exact 1.7.10 RenderBarrel inventory path for the six filled barrel items. */
public final class BarrelItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ModelResourceLocation RED = model("barrel_item/red_barrel");
    private static final ModelResourceLocation PINK = model("barrel_item/pink_barrel");
    private static final ModelResourceLocation LOX = model("barrel_item/lox_barrel");
    private static final ModelResourceLocation TAINT = model("barrel_item/taint_barrel");
    private static final ModelResourceLocation YELLOW = model("barrel_item/yellow_barrel");
    private static final ModelResourceLocation VITRIFIED = model("barrel_item/vitrified_barrel");

    public BarrelItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(RED);
        event.register(PINK);
        event.register(LOX);
        event.register(TAINT);
        event.register(YELLOW);
        event.register(VITRIFIED);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        switch (BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath()) {
            case "red_barrel" -> renderRedBarrelItem(stack, context, poseStack, bufferSource, packedLight, packedOverlay);
            case "pink_barrel" -> renderPinkBarrelItem(stack, context, poseStack, bufferSource, packedLight, packedOverlay);
            case "lox_barrel" -> renderLoxBarrelItem(stack, context, poseStack, bufferSource, packedLight, packedOverlay);
            case "taint_barrel" -> renderTaintBarrelItem(stack, context, poseStack, bufferSource, packedLight, packedOverlay);
            case "yellow_barrel" -> renderYellowBarrelItem(stack, context, poseStack, bufferSource, packedLight, packedOverlay);
            case "vitrified_barrel" -> renderVitrifiedBarrelItem(stack, context, poseStack, bufferSource, packedLight, packedOverlay);
            default -> { }
        }
    }

    private static void renderRedBarrelItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                                            MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = ((BlockItem) stack.getItem()).getBlock().defaultBlockState();
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        applyRedBarrelItemPose(context, poseStack);
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        renderBarrelModel(RED, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderPinkBarrelItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = ((BlockItem) stack.getItem()).getBlock().defaultBlockState();
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        applyPinkBarrelItemPose(context, poseStack);
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        renderBarrelModel(PINK, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderLoxBarrelItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                                            MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = ((BlockItem) stack.getItem()).getBlock().defaultBlockState();
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        applyLoxBarrelItemPose(context, poseStack);
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        renderBarrelModel(LOX, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderTaintBarrelItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                                              MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = ((BlockItem) stack.getItem()).getBlock().defaultBlockState();
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        applyTaintBarrelItemPose(context, poseStack);
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        renderBarrelModel(TAINT, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderYellowBarrelItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                                               MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = ((BlockItem) stack.getItem()).getBlock().defaultBlockState();
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        applyYellowBarrelItemPose(context, poseStack);
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        renderBarrelModel(YELLOW, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderVitrifiedBarrelItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                                                  MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = ((BlockItem) stack.getItem()).getBlock().defaultBlockState();
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        applyVitrifiedBarrelItemPose(context, poseStack);
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        renderBarrelModel(VITRIFIED, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void applyRedBarrelItemPose(ItemDisplayContext context, PoseStack poseStack) {
        switch (context) {
            case GUI -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
                poseStack.scale(0.625F, 0.625F, 0.625F);
            }
            case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(75.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
                poseStack.translate(0.0F, 2.5F / 16.0F, 0.0F);
                poseStack.scale(0.375F, 0.375F, 0.375F);
            }
            case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
                poseStack.scale(0.4F, 0.4F, 0.4F);
            }
            case GROUND -> {
                poseStack.translate(0.0F, 3.0F / 16.0F, 0.0F);
                poseStack.scale(0.25F, 0.25F, 0.25F);
            }
            case FIXED -> poseStack.scale(0.5F, 0.5F, 0.5F);
            default -> {
            }
        }
    }

    private static void applyPinkBarrelItemPose(ItemDisplayContext context, PoseStack poseStack) {
        switch (context) {
            case GUI -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
                poseStack.scale(0.625F, 0.625F, 0.625F);
            }
            case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(75.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
                poseStack.translate(0.0F, 2.5F / 16.0F, 0.0F);
                poseStack.scale(0.375F, 0.375F, 0.375F);
            }
            case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
                poseStack.scale(0.4F, 0.4F, 0.4F);
            }
            case GROUND -> {
                poseStack.translate(0.0F, 3.0F / 16.0F, 0.0F);
                poseStack.scale(0.25F, 0.25F, 0.25F);
            }
            case FIXED -> poseStack.scale(0.5F, 0.5F, 0.5F);
            default -> { }
        }
    }

    private static void applyLoxBarrelItemPose(ItemDisplayContext context, PoseStack poseStack) {
        switch (context) {
            case GUI -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
                poseStack.scale(0.625F, 0.625F, 0.625F);
            }
            case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(75.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
                poseStack.translate(0.0F, 2.5F / 16.0F, 0.0F);
                poseStack.scale(0.375F, 0.375F, 0.375F);
            }
            case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
                poseStack.scale(0.4F, 0.4F, 0.4F);
            }
            case GROUND -> {
                poseStack.translate(0.0F, 3.0F / 16.0F, 0.0F);
                poseStack.scale(0.25F, 0.25F, 0.25F);
            }
            case FIXED -> poseStack.scale(0.5F, 0.5F, 0.5F);
            default -> { }
        }
    }

    private static void applyTaintBarrelItemPose(ItemDisplayContext context, PoseStack poseStack) {
        switch (context) {
            case GUI -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
                poseStack.scale(0.625F, 0.625F, 0.625F);
            }
            case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(75.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
                poseStack.translate(0.0F, 2.5F / 16.0F, 0.0F);
                poseStack.scale(0.375F, 0.375F, 0.375F);
            }
            case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
                poseStack.scale(0.4F, 0.4F, 0.4F);
            }
            case GROUND -> {
                poseStack.translate(0.0F, 3.0F / 16.0F, 0.0F);
                poseStack.scale(0.25F, 0.25F, 0.25F);
            }
            case FIXED -> poseStack.scale(0.5F, 0.5F, 0.5F);
            default -> { }
        }
    }

    private static void applyYellowBarrelItemPose(ItemDisplayContext context, PoseStack poseStack) {
        switch (context) {
            case GUI -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
                poseStack.scale(0.625F, 0.625F, 0.625F);
            }
            case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(75.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
                poseStack.translate(0.0F, 2.5F / 16.0F, 0.0F);
                poseStack.scale(0.375F, 0.375F, 0.375F);
            }
            case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
                poseStack.scale(0.4F, 0.4F, 0.4F);
            }
            case GROUND -> {
                poseStack.translate(0.0F, 3.0F / 16.0F, 0.0F);
                poseStack.scale(0.25F, 0.25F, 0.25F);
            }
            case FIXED -> poseStack.scale(0.5F, 0.5F, 0.5F);
            default -> { }
        }
    }

    private static void applyVitrifiedBarrelItemPose(ItemDisplayContext context, PoseStack poseStack) {
        switch (context) {
            case GUI -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
                poseStack.scale(0.625F, 0.625F, 0.625F);
            }
            case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(75.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
                poseStack.translate(0.0F, 2.5F / 16.0F, 0.0F);
                poseStack.scale(0.375F, 0.375F, 0.375F);
            }
            case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
                poseStack.scale(0.4F, 0.4F, 0.4F);
            }
            case GROUND -> {
                poseStack.translate(0.0F, 3.0F / 16.0F, 0.0F);
                poseStack.scale(0.25F, 0.25F, 0.25F);
            }
            case FIXED -> poseStack.scale(0.5F, 0.5F, 0.5F);
            default -> { }
        }
    }

    private static void renderBarrelModel(ModelResourceLocation model, BlockState state, PoseStack poseStack,
                                          MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(model), poseStack,
                bufferSource, state, packedLight, packedOverlay);
    }

    private static ModelResourceLocation model(String path) {
        return ModelResourceLocation.standalone(ReinhardtsHBM.id(path));
    }
}
