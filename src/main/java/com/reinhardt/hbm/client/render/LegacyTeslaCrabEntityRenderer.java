package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.entity.LegacyTeslaCrabEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Tesla Crab OBJ parts and the original leg swing. */
public final class LegacyTeslaCrabEntityRenderer extends EntityRenderer<LegacyTeslaCrabEntity> {
    private static final ModelResourceLocation BODY = model("teslacrab_body");
    private static final ModelResourceLocation FRONT = model("teslacrab_front");
    private static final ModelResourceLocation BACK = model("teslacrab_back");
    private static final BlockState STATE = Blocks.IRON_BLOCK.defaultBlockState();
    private static final LegacyEntityObjMesh BOUNDS_MODEL = LegacyEntityObjMesh.load(
            ReinhardtsHBM.id("models/obj/entity/teslacrab.obj"));
    private static final double BODY_RADIUS = transformedBodyRadius();

    public LegacyTeslaCrabEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        // RenderTeslaCrab used RenderLiving's 1.0F shadow size and only
        // disabled shadow opacity.
        shadowRadius = 1.0F;
    }

    private static ModelResourceLocation model(String name) {
        return ModelResourceLocation.standalone(ReinhardtsHBM.id("entity/" + name));
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(BODY);
        event.register(FRONT);
        event.register(BACK);
    }

    @Override
    public void render(LegacyTeslaCrabEntity entity, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        float bodyYaw = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
        // RenderLivingBase's 1.7.10 rotate/scale/prepare pipeline.
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
        // RenderLiving's fixed biped mirror was applied before ModelTeslaCrab
        // in 1.7.10.  This is part of the authored coordinate system, not a
        // generic scale adjustment.
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0F, -1.5078125F, 0.0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        poseStack.translate(0.0F, -1.5F, 0.0F);
        MachineModelRenderer.renderUnculledUv(MachineModelRenderer.model(BODY), poseStack, bufferSource,
                STATE, packedLight, OverlayTexture.NO_OVERLAY,
                ReinhardtsHBM.id("textures/entity/teslacrab.png"), 0.0F, 0.0F);
        float swing = (float) (-(Math.cos(entity.walkAnimation.position(partialTick) * .6662F * 2.0F)
                * .4F) * entity.walkAnimation.speed(partialTick) * 57.3F);
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(swing));
        MachineModelRenderer.renderUnculledUv(MachineModelRenderer.model(FRONT), poseStack, bufferSource,
                STATE, packedLight, OverlayTexture.NO_OVERLAY,
                ReinhardtsHBM.id("textures/entity/teslacrab.png"), 0.0F, 0.0F);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-swing));
        MachineModelRenderer.renderUnculledUv(MachineModelRenderer.model(BACK), poseStack, bufferSource,
                STATE, packedLight, OverlayTexture.NO_OVERLAY,
                ReinhardtsHBM.id("textures/entity/teslacrab.png"), 0.0F, 0.0F);
        poseStack.popPose();
        poseStack.popPose();
        LegacyCyberCrabBeamRenderer.render(poseStack, bufferSource, entity.targets(),
                entity.position(), 1.0D, entity.level().getGameTime());
        super.render(entity, yaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public boolean shouldRender(LegacyTeslaCrabEntity entity, Frustum frustum,
                                double cameraX, double cameraY, double cameraZ) {
        AABB bodyBounds = EntityRenderBounds.radius(
                entity.getX(), entity.getY(), entity.getZ(), BODY_RADIUS);
        AABB bounds = LegacyCyberCrabBeamRenderer.includeBeamBounds(
                bodyBounds, entity.targets(), entity.position(), 1.0D);
        return EntityRenderBounds.shouldRender(entity, bounds, frustum,
                cameraX, cameraY, cameraZ);
    }

    @Override
    public ResourceLocation getTextureLocation(LegacyTeslaCrabEntity entity) {
        return ReinhardtsHBM.id("textures/entity/teslacrab.png");
    }

    private static double transformedBodyRadius() {
        // The two fixed translations have magnitudes 1.5078125 and 1.5.
        // Every remaining body/leg operation is a unit scale or rotation, so
        // the triangle inequality contains every walk-animation phase.
        double radius = BOUNDS_MODEL.maximumFaceRadius(0.0D, 0.0D, 0.0D)
                + 1.5078125D + 1.5D;
        double unitRoundoff = 0x1.0p-24;
        double gamma64 = 64.0D * unitRoundoff / (1.0D - 64.0D * unitRoundoff);
        return Math.nextUp(radius * (1.0D + gamma64));
    }
}
