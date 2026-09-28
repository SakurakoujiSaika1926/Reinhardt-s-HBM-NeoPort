package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.reinhardt.hbm.entity.OrbitalLaserEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

/** Literal red outer/white inner 250-block RenderOrbitalLaser beam. */
public final class OrbitalLaserEntityRenderer extends EntityRenderer<OrbitalLaserEntity> {
    private static final RenderType BEAM = RenderType.create(
            "reinhardtshbm_orbital_laser",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            1024,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_LIGHTNING_SHADER)
                    .setTextureState(RenderStateShard.NO_TEXTURE)
                    .setTransparencyState(RenderStateShard.LIGHTNING_TRANSPARENCY)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setOutputState(RenderStateShard.MAIN_TARGET)
                    .createCompositeState(false));

    public OrbitalLaserEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(OrbitalLaserEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        VertexConsumer consumer = bufferSource.getBuffer(BEAM);
        PoseStack.Pose pose = poseStack.last();
        double x = 0.5D;
        double z = 0.0D;
        // Vec3.rotateAroundY(45) in 1.7.10 consumes radians. Preserve it.
        float cosine = Mth.cos(45.0F);
        float sine = Mth.sin(45.0F);
        for (int index = 0; index < 8; index++) {
            double nextX = x * cosine + z * sine;
            double nextZ = z * cosine - x * sine;
            quad(consumer, pose, x, z, nextX, nextZ, 255, 0, 0);
            x = nextX;
            z = nextZ;
        }
        for (int index = 0; index < 8; index++) {
            double nextX = x * cosine + z * sine;
            double nextZ = z * cosine - x * sine;
            quad(consumer, pose, x * 0.5D, z * 0.5D,
                    nextX * 0.5D, nextZ * 0.5D, 255, 255, 255);
            x = nextX;
            z = nextZ;
        }
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    private static void quad(VertexConsumer consumer, PoseStack.Pose pose,
                             double x1, double z1, double x2, double z2,
                             int red, int green, int blue) {
        vertex(consumer, pose, x1, 250.0D, z1, red, green, blue);
        vertex(consumer, pose, x1, 0.0D, z1, red, green, blue);
        vertex(consumer, pose, x2, 0.0D, z2, red, green, blue);
        vertex(consumer, pose, x2, 250.0D, z2, red, green, blue);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose,
                               double x, double y, double z, int red, int green, int blue) {
        consumer.addVertex(pose, (float) x, (float) y, (float) z).setColor(red, green, blue, 255);
    }

    @Override
    public boolean shouldRender(OrbitalLaserEntity entity, Frustum frustum,
                                double cameraX, double cameraY, double cameraZ) {
        AABB beam = new AABB(entity.getX() - 0.5D, entity.getY(), entity.getZ() - 0.5D,
                entity.getX() + 0.5D, entity.getY() + 250.0D, entity.getZ() + 0.5D);
        return EntityRenderBounds.shouldRender(entity, beam, entity.getBoundingBox(), frustum,
                cameraX, cameraY, cameraZ);
    }

    @Override
    public ResourceLocation getTextureLocation(OrbitalLaserEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
