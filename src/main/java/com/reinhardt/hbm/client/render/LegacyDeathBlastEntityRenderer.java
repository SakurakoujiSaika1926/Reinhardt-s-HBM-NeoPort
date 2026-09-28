package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.entity.LegacyDeathBlastEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

/** Literal RenderDeathBlast beam and Icosphere charge-stage port. */
public final class LegacyDeathBlastEntityRenderer extends EntityRenderer<LegacyDeathBlastEntity> {
    private static final ResourceLocation SPHERE_LOCATION =
            ReinhardtsHBM.id("models/entity/death_blast_sphere.obj");
    private static final LegacyEntityObjMesh SPHERE = LegacyEntityObjMesh.load(SPHERE_LOCATION);
    private static final double SPHERE_MODEL_RADIUS = SPHERE.maximumFaceRadius(0.0D, 0.0D, 0.0D);
    private static final double OUTER_SHELL_FACTOR = 1.25D * Math.pow(1.05D, 7.0D);

    private static final RenderType BEAM_ADDITIVE = positionColorType(
            "reinhardtshbm_death_blast_beam", VertexFormat.Mode.QUADS,
            RenderStateShard.LIGHTNING_TRANSPARENCY, RenderStateShard.COLOR_WRITE);
    private static final RenderType SPHERE_ALPHA = positionColorType(
            "reinhardtshbm_death_blast_sphere", VertexFormat.Mode.TRIANGLES,
            RenderStateShard.TRANSLUCENT_TRANSPARENCY, RenderStateShard.COLOR_DEPTH_WRITE);
    private static final RenderType SPHERE_ADDITIVE = positionColorType(
            "reinhardtshbm_death_blast_shell", VertexFormat.Mode.TRIANGLES,
            RenderStateShard.LIGHTNING_TRANSPARENCY, RenderStateShard.COLOR_DEPTH_WRITE);

    public LegacyDeathBlastEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(LegacyDeathBlastEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        renderBeams(poseStack, bufferSource.getBuffer(BEAM_ADDITIVE));
        renderOrb(entity, poseStack, bufferSource);
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    private static void renderBeams(PoseStack poseStack, VertexConsumer consumer) {
        PoseStack.Pose pose = poseStack.last();
        double x = 0.5D;
        double z = 0.0D;

        // RenderDeathBlast passes 45 directly to Vec3.rotateAroundY. That API
        // consumes radians; preserve the old result rather than silently
        // correcting it to a conventional 45-degree octagon.
        float cosine = Mth.cos(45.0F);
        float sine = Mth.sin(45.0F);
        for (int index = 0; index < 8; index++) {
            double nextX = x * cosine + z * sine;
            double nextZ = z * cosine - x * sine;
            beamQuad(consumer, pose, x, z, nextX, nextZ, 255, 0, 0);
            x = nextX;
            z = nextZ;
        }
        for (int index = 0; index < 8; index++) {
            double nextX = x * cosine + z * sine;
            double nextZ = z * cosine - x * sine;
            beamQuad(consumer, pose, x * 0.5D, z * 0.5D,
                    nextX * 0.5D, nextZ * 0.5D, 255, 0, 255);
            x = nextX;
            z = nextZ;
        }
    }

    private static void beamQuad(VertexConsumer consumer, PoseStack.Pose pose,
                                 double x1, double z1, double x2, double z2,
                                 int red, int green, int blue) {
        vertex(consumer, pose, x1, 250.0D, z1, red, green, blue, 255);
        vertex(consumer, pose, x1, 0.0D, z1, red, green, blue, 255);
        vertex(consumer, pose, x2, 0.0D, z2, red, green, blue, 255);
        vertex(consumer, pose, x2, 250.0D, z2, red, green, blue, 255);
    }

    private static void renderOrb(LegacyDeathBlastEntity entity, PoseStack poseStack,
                                  MultiBufferSource bufferSource) {
        double scale = Math.max(0.0D,
                10.0D - 10.0D * entity.tickCount / LegacyDeathBlastEntity.MAX_AGE);
        double alpha = (double) entity.tickCount / LegacyDeathBlastEntity.MAX_AGE;
        if (scale <= 0.0D || alpha <= 0.0D) {
            return;
        }

        poseStack.pushPose();
        poseStack.scale((float) scale, (float) scale, (float) scale);
        SPHERE.renderAllTriangles(poseStack, bufferSource.getBuffer(SPHERE_ALPHA),
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                argb(alpha, 255, 0, 255));

        poseStack.scale(1.25F, 1.25F, 1.25F);
        VertexConsumer shell = bufferSource.getBuffer(SPHERE_ADDITIVE);
        int shellColor = argb(alpha * 0.125D, 255, 0, 0);
        for (int index = 0; index < 8; index++) {
            SPHERE.renderAllTriangles(poseStack, shell,
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, shellColor);
            poseStack.scale(1.05F, 1.05F, 1.05F);
        }
        poseStack.popPose();
    }

    @Override
    public boolean shouldRender(LegacyDeathBlastEntity entity, Frustum frustum,
                                double cameraX, double cameraY, double cameraZ) {
        return EntityRenderBounds.shouldRender(
                entity, visualBounds(entity), entity.getBoundingBox(), frustum,
                cameraX, cameraY, cameraZ);
    }

    private static AABB visualBounds(LegacyDeathBlastEntity entity) {
        double scale = Math.max(0.0D,
                10.0D - 10.0D * entity.tickCount / LegacyDeathBlastEntity.MAX_AGE);
        double sphereRadius = Math.nextUp(scale * OUTER_SHELL_FACTOR * SPHERE_MODEL_RADIUS);
        AABB beam = new AABB(
                entity.getX() - 0.5D, entity.getY(), entity.getZ() - 0.5D,
                entity.getX() + 0.5D, entity.getY() + 250.0D, entity.getZ() + 0.5D);
        if (sphereRadius <= 0.0D) {
            return beam;
        }
        return beam.minmax(EntityRenderBounds.radius(
                entity.getX(), entity.getY(), entity.getZ(), sphereRadius));
    }

    private static RenderType positionColorType(
            String name, VertexFormat.Mode mode,
            RenderStateShard.TransparencyStateShard transparency,
            RenderStateShard.WriteMaskStateShard writeMask) {
        return RenderType.create(
                name,
                DefaultVertexFormat.POSITION_COLOR,
                mode,
                1536,
                false,
                true,
                RenderType.CompositeState.builder()
                        .setShaderState(RenderStateShard.RENDERTYPE_LIGHTNING_SHADER)
                        .setTextureState(RenderStateShard.NO_TEXTURE)
                        .setTransparencyState(transparency)
                        .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                        .setWriteMaskState(writeMask)
                        .setOutputState(RenderStateShard.MAIN_TARGET)
                        .createCompositeState(false));
    }

    private static int argb(double alpha, int red, int green, int blue) {
        int alphaByte = Mth.clamp((int) Math.round(alpha * 255.0D), 0, 255);
        return alphaByte << 24 | red << 16 | green << 8 | blue;
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose,
                               double x, double y, double z,
                               int red, int green, int blue, int alpha) {
        consumer.addVertex(pose, (float) x, (float) y, (float) z)
                .setColor(red, green, blue, alpha);
    }

    @Override
    public ResourceLocation getTextureLocation(LegacyDeathBlastEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
