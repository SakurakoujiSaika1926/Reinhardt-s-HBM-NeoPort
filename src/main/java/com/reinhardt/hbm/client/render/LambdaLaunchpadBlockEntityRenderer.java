package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.LambdaLaunchpadBlock;
import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.blockentity.LambdaLaunchpadBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;

/** Dedicated grouped renderer; no generic machine rotation is used. */
public final class LambdaLaunchpadBlockEntityRenderer
        implements LongRangeBlockEntityRenderer<LambdaLaunchpadBlockEntity> {
    private static final ResourceLocation PAD_OBJ =
            ReinhardtsHBM.id("models/obj/machine/launchpad_lambda.obj");
    private static final ResourceLocation ROCKET_OBJ =
            ReinhardtsHBM.id("models/obj/entity/lambda_rocket.obj");
    private static final ResourceLocation PAD_TEXTURE =
            ReinhardtsHBM.id("textures/models/legacy/launchpad_lambda.png");
    private static final ResourceLocation ROCKET_TEXTURE =
            ReinhardtsHBM.id("textures/models/legacy/lambda_rocket.png");
    private static final LegacyEntityObjMesh PAD = LegacyEntityObjMesh.load(PAD_OBJ);
    private static final LegacyEntityObjMesh ROCKET = LegacyEntityObjMesh.load(ROCKET_OBJ);

    public LambdaLaunchpadBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(LambdaLaunchpadBlockEntity launchpad, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        Direction facing = launchpad.getBlockState().getValue(LargeMachineBlock.FACING);
        float rotation = LambdaLaunchpadBlock.legacyRotation(facing);
        float doors = launchpad.renderPosition(LambdaLaunchpadBlockEntity.INDEX_DOORS, partialTick);
        float erector = launchpad.renderPosition(LambdaLaunchpadBlockEntity.INDEX_ERECTOR, partialTick) - 25.0F;
        float rotor = launchpad.renderPosition(LambdaLaunchpadBlockEntity.INDEX_ROTOR, partialTick);
        float clamps = launchpad.renderPosition(LambdaLaunchpadBlockEntity.INDEX_CLAMPS, partialTick);
        float pistons = launchpad.renderPosition(LambdaLaunchpadBlockEntity.INDEX_PISTONS, partialTick);
        boolean renderRocket = launchpad.erecting() || launchpad.erected();
        boolean rocketFixed = launchpad.erected();

        VertexConsumer pad = buffers.getBuffer(RenderType.entityCutoutNoCull(PAD_TEXTURE));
        VertexConsumer rocket = buffers.getBuffer(RenderType.entityCutoutNoCull(ROCKET_TEXTURE));

        pose.pushPose();
        pose.translate(0.5D, 0.0D, 0.5D);
        pose.mulPose(Axis.YP.rotationDegrees(rotation));

        if (rocketFixed) {
            pose.pushPose();
            pose.translate(0.0D, 2.0D, 0.0D);
            pose.mulPose(Axis.YP.rotationDegrees(-rotation));
            ROCKET.renderAll(pose, rocket, packedLight, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
            pose.popPose();
        }

        group("Silo", pose, pad, packedLight, packedOverlay);
        pose.pushPose();
        pose.translate(0.0D, 0.0D, doors);
        group("DoorLeft", pose, pad, packedLight, packedOverlay);
        pose.popPose();
        pose.pushPose();
        pose.translate(0.0D, 0.0D, -doors);
        group("DoorRight", pose, pad, packedLight, packedOverlay);
        pose.popPose();

        pose.translate(0.0D, erector, 0.0D);
        group("Erector", pose, pad, packedLight, packedOverlay);
        pose.translate(0.0D, 13.25D, 0.0D);
        pose.mulPose(Axis.XP.rotationDegrees(rotor));
        pose.translate(0.0D, -13.25D, 0.0D);
        group("Rotor", pose, pad, packedLight, packedOverlay);

        renderClampPair(pose, pad, packedLight, packedOverlay,
                3.5D, 19.75D, -clamps, pistons, "PivotUpper1", "ClampUpper1");
        renderClampPair(pose, pad, packedLight, packedOverlay,
                3.5D, 6.75D, clamps, pistons, "PivotLower1", "ClampLower1");
        renderClampPair(pose, pad, packedLight, packedOverlay,
                -3.5D, 19.75D, clamps, -pistons, "PivotUpper2", "ClampUpper2");
        renderClampPair(pose, pad, packedLight, packedOverlay,
                -3.5D, 6.75D, -clamps, -pistons, "PivotLower2", "ClampLower2");

        if (renderRocket && !rocketFixed) {
            pose.translate(0.0D, 2.0D, 0.0D);
            pose.mulPose(Axis.YP.rotationDegrees(-rotation));
            ROCKET.renderAll(pose, rocket, packedLight, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        }
        pose.popPose();
    }

    private static void renderClampPair(PoseStack pose, VertexConsumer consumer,
                                        int light, int overlay,
                                        double pivotX, double pivotY, float rotation,
                                        float piston, String pivotGroup, String clampGroup) {
        pose.pushPose();
        pose.translate(pivotX, pivotY, 0.0D);
        pose.mulPose(Axis.ZP.rotationDegrees(rotation));
        pose.translate(-pivotX, -pivotY, 0.0D);
        group(pivotGroup, pose, consumer, light, overlay);
        pose.translate(piston, 0.0D, 0.0D);
        group(clampGroup, pose, consumer, light, overlay);
        pose.popPose();
    }

    private static void group(String group, PoseStack pose, VertexConsumer consumer,
                              int light, int overlay) {
        PAD.renderGroup(group, pose, consumer, light, overlay, 0xFFFFFFFF);
    }

    @Override
    public AABB getRenderBoundingBox(LambdaLaunchpadBlockEntity launchpad) {
        var pos = launchpad.getBlockPos();
        return new AABB(pos.getX() - 7.0D, pos.getY(), pos.getZ() - 7.0D,
                pos.getX() + 8.0D, pos.getY() + 30.0D, pos.getZ() + 8.0D);
    }
}
