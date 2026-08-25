package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.reinhardt.hbm.entity.LegacyBossProjectileEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Legacy RenderBullet styles used by the chopper, BOTPrime and UFO bosses. */
public final class LegacyBossProjectileEntityRenderer extends EntityRenderer<LegacyBossProjectileEntity> {
    private static final ModelResourceLocation ROCKET = MachineModelRenderer.standalone("entity/legacy_rocket");
    private static final BlockState RENDER_STATE = Blocks.STONE.defaultBlockState();

    public LegacyBossProjectileEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(ROCKET);
    }

    @Override
    public void render(LegacyBossProjectileEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        float yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        float pitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
        if (entity.projectileType() == LegacyBossProjectileEntity.Type.UFO_ROCKET) {
            renderRocket(yaw, pitch, poseStack, bufferSource, packedLight);
        } else {
            int color = switch (entity.projectileType()) {
                case WORM_LASER -> 0xFF2020;
                case WORM_BOLT -> 0x20FF40;
                default -> 0xFFB020;
            };
            renderDart(yaw, pitch, color, poseStack, bufferSource);
        }
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    private static void renderRocket(float yaw, float pitch, PoseStack poseStack,
                                     MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(pitch + 180.0F));
        poseStack.scale(0.75F, 0.75F, 0.75F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(ROCKET), poseStack, bufferSource,
                RENDER_STATE, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }

    private static void renderDart(float yaw, float pitch, int color, PoseStack poseStack,
                                   MultiBufferSource bufferSource) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(pitch + 180.0F));
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());
        dartQuad(consumer, poseStack.last(), 0.75D, 0.09D, color);
        poseStack.popPose();
    }

    private static void dartQuad(VertexConsumer consumer, PoseStack.Pose pose,
                                 double length, double width, int color) {
        int red = (color >>> 16) & 0xFF;
        int green = (color >>> 8) & 0xFF;
        int blue = color & 0xFF;
        vertex(consumer, pose, length, 0.0D, 0.0D, red, green, blue, 255);
        vertex(consumer, pose, 0.0D, -width, -width, red, green, blue, 0);
        vertex(consumer, pose, 0.0D, width, -width, red, green, blue, 0);
        vertex(consumer, pose, length, 0.0D, 0.0D, red, green, blue, 255);
        vertex(consumer, pose, 0.0D, width, width, red, green, blue, 0);
        vertex(consumer, pose, 0.0D, -width, width, red, green, blue, 0);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, double x, double y, double z,
                                int red, int green, int blue, int alpha) {
        consumer.addVertex(pose, (float) x, (float) y, (float) z)
                .setColor(red, green, blue, alpha)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(LegacyBossProjectileEntity entity) {
        return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
    }
}
