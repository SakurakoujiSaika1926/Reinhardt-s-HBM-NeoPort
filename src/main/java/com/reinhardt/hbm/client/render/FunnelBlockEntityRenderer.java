package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.blockentity.FunnelBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Exact single-block origin handling from 1.7.10 MachineFunnel#renderWorld. */
public final class FunnelBlockEntityRenderer implements BlockEntityRenderer<FunnelBlockEntity> {
    private static final ModelResourceLocation TOP = part("machine_funnel_top");
    private static final ModelResourceLocation BOTTOM = part("machine_funnel_bottom");
    private static final ModelResourceLocation SIDE = part("machine_funnel_side");

    public FunnelBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(TOP);
        event.register(BOTTOM);
        event.register(SIDE);
    }

    @Override
    public void render(FunnelBlockEntity funnel, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = funnel.getBlockState();
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        renderPart(TOP, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderPart(BOTTOM, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderPart(SIDE, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(FunnelBlockEntity funnel) {
        return new AABB(funnel.getBlockPos());
    }

    public static void renderItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                                  MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        applyLegacyBlockItemTransform(context, poseStack);
        // MachineFunnel#renderInventory has no model-specific scale or
        // rotation; it only lowers the centered OBJ by half a block.
        poseStack.translate(0.0F, -0.5F, 0.0F);
        BlockState state = com.reinhardt.hbm.registry.HbmBlocks.MACHINE_FUNNEL.get().defaultBlockState();
        renderPart(TOP, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderPart(BOTTOM, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderPart(SIDE, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    /** The standard block-item transform used around the 1.7.10 custom block renderer. */
    private static void applyLegacyBlockItemTransform(ItemDisplayContext context, PoseStack poseStack) {
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

    private static void renderPart(ModelResourceLocation model, PoseStack poseStack,
                                   MultiBufferSource bufferSource, BlockState state,
                                   int packedLight, int packedOverlay) {
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(model), poseStack,
                bufferSource, state, packedLight, packedOverlay);
    }

    private static ModelResourceLocation part(String name) {
        return MachineModelRenderer.standalone("block/" + name);
    }
}
