package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.item.LegacyCrucibleItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/** 1.7.10 ItemRenderCrucible transforms, split into the original OBJ parts. */
public final class CrucibleItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ModelResourceLocation HILT = MachineModelRenderer.standalone("weapons/crucible_hilt");
    private static final ModelResourceLocation GUARD_LEFT = MachineModelRenderer.standalone("weapons/crucible_guard_left");
    private static final ModelResourceLocation GUARD_RIGHT = MachineModelRenderer.standalone("weapons/crucible_guard_right");
    private static final ModelResourceLocation BLADE = MachineModelRenderer.standalone("weapons/crucible_blade");

    public CrucibleItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        LegacyMachineItemRenderer.applyItemRenderBasePose(context, poseStack);
        boolean charged = LegacyCrucibleItem.isCharged(stack);
        if (context == ItemDisplayContext.GUI) {
            poseStack.translate(2.0F, 14.0F, 0.0F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(-135.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            poseStack.scale(1.5F, 1.5F, 1.5F);
        } else {
            if (context == ItemDisplayContext.GROUND) {
                poseStack.translate(-0.75F, 0.6F, 0.0F);
                poseStack.mulPose(Axis.ZP.rotationDegrees(-45.0F));
            }
            poseStack.mulPose(Axis.ZP.rotationDegrees(45.0F));
            poseStack.translate(0.75F, -0.4F, 0.0F);
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            poseStack.scale(0.15F, 0.15F, 0.15F);
        }

        render(HILT, poseStack, bufferSource, packedLight, packedOverlay);
        renderGuard(GUARD_LEFT, poseStack, bufferSource, packedLight, packedOverlay, charged, true);
        renderGuard(GUARD_RIGHT, poseStack, bufferSource, packedLight, packedOverlay, charged, false);
        if (charged) {
            poseStack.pushPose();
            poseStack.translate(0.005F, 0.0F, 0.0F);
            MachineModelRenderer.renderUnculledFullBright(MachineModelRenderer.model(BLADE), poseStack, bufferSource,
                    Blocks.IRON_BLOCK.defaultBlockState(), packedOverlay);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    private static void renderGuard(ModelResourceLocation model, PoseStack poseStack, MultiBufferSource bufferSource,
                                    int packedLight, int packedOverlay, boolean charged, boolean left) {
        poseStack.pushPose();
        if (left) {
            poseStack.translate(0.0F, 3.0F, 0.5F);
            poseStack.mulPose(Axis.XN.rotationDegrees(charged ? 0.0F : 90.0F));
            poseStack.translate(0.0F, -3.0F, -0.5F);
        } else {
            poseStack.translate(0.0F, 3.0F, -0.5F);
            poseStack.mulPose(Axis.XP.rotationDegrees(charged ? 0.0F : 90.0F));
            poseStack.translate(0.0F, -3.0F, 0.5F);
        }
        render(model, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void render(ModelResourceLocation model, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(model), poseStack, bufferSource,
                Blocks.IRON_BLOCK.defaultBlockState(), packedLight, packedOverlay);
    }
}
