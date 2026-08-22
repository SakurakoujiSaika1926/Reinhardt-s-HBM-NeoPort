package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.reinhardt.hbm.entity.LegacyArtilleryShellEntity;
import com.reinhardt.hbm.entity.LegacyBulletEntity;
import com.reinhardt.hbm.entity.LegacyHimarsRocketEntity;
import com.reinhardt.hbm.entity.LegacyProjectileUtil;
import com.reinhardt.hbm.item.StandardAmmoItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;

public final class LegacyProjectileEntityRenderer {
    private static final ModelResourceLocation RICHARD_MISSILE = MachineModelRenderer.standalone("entity/legacy_richard_missile");
    private static final ModelResourceLocation ARTILLERY = MachineModelRenderer.standalone("entity/legacy_artillery_shell");
    private static final ModelResourceLocation[] HIMARS_MODELS = new ModelResourceLocation[LegacyProjectileUtil.HimarsType.values().length];
    private static final BlockState RENDER_STATE = Blocks.STONE.defaultBlockState();

    static {
        for (LegacyProjectileUtil.HimarsType type : LegacyProjectileUtil.HimarsType.values()) {
            HIMARS_MODELS[type.modelData()] = MachineModelRenderer.standalone("entity/legacy_himars_" + type.id());
        }
    }

    private LegacyProjectileEntityRenderer() {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(RICHARD_MISSILE);
        event.register(ARTILLERY);
        for (ModelResourceLocation model : HIMARS_MODELS) {
            event.register(model);
        }
    }

    static void renderRifleBullet(Entity entity, StandardAmmoItem.StandardAmmoType ammo, float partialTick,
                                  PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        renderTracer(entity, ammo, partialTick, poseStack, bufferSource, packedLight, 1.0D, false);
    }

    private static void renderTracer(Entity entity, StandardAmmoItem.StandardAmmoType ammo, float partialTick,
                                     PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                                     double lengthMultiplier, boolean fullBright) {
        double dx = entity.getX() - entity.xo;
        double dy = entity.getY() - entity.yo;
        double dz = entity.getZ() - entity.zo;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz) * lengthMultiplier;
        if (length <= 0.0D) {
            return;
        }
        int dark;
        int light;
        switch (ammo) {
            case BMG50_AP, R556_AP, P9_AP -> {
                dark = 0xFF6A00;
                light = 0xFFE28D;
            }
            case BMG50_DU -> {
                dark = 0x5CCD41;
                light = 0xE9FF8D;
            }
            default -> {
                dark = 0xFFBF00;
                light = 0xFFFFFF;
            }
        }
        float yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        float pitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(pitch + 180.0F));
        renderTracerGeometry(poseStack.last(), bufferSource.getBuffer(RenderType.lightning()), length,
                0.03125D, 0.03125D * 0.25D, dark, light,
                fullBright ? LightTexture.FULL_BRIGHT : packedLight);
        poseStack.popPose();
    }

    private static void renderRichardMissile(LegacyBulletEntity entity, float partialTick, PoseStack poseStack,
                                              MultiBufferSource bufferSource, int packedLight) {
        float yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        float pitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(pitch + 180.0F));
        poseStack.pushPose();
        poseStack.scale(0.25F, 0.25F, 0.25F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
        poseStack.translate(0.0D, -1.0D, -4.5D);
        MachineModelRenderer.renderUnculled(
                MachineModelRenderer.model(RICHARD_MISSILE), poseStack, bufferSource, RENDER_STATE, packedLight, 0);
        poseStack.popPose();
        poseStack.translate(0.375D, 0.0D, 0.0D);
        double dx = entity.getX() - entity.xo;
        double dy = entity.getY() - entity.yo;
        double dz = entity.getZ() - entity.zo;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz) * 2.0D;
        if (length > 0.0D) {
            renderTracerGeometry(poseStack.last(), bufferSource.getBuffer(RenderType.lightning()), length,
                    0.03125D, 0.03125D * 0.25D, 0x808080, 0xFFF2A7, LightTexture.FULL_BRIGHT);
        }
        poseStack.popPose();
    }

    private static void renderTracerGeometry(PoseStack.Pose pose, VertexConsumer consumer, double length,
                                             double frontWidth, double backWidth,
                                             int dark, int light, int packedLight) {
        tracerQuad(consumer, pose,
                length, backWidth, -backWidth, dark,
                length, backWidth, backWidth, dark,
                0.0D, frontWidth, frontWidth, light,
                0.0D, frontWidth, -frontWidth, light,
                packedLight);
        tracerQuad(consumer, pose,
                length, -backWidth, -backWidth, dark,
                length, -backWidth, backWidth, dark,
                0.0D, -frontWidth, frontWidth, light,
                0.0D, -frontWidth, -frontWidth, light,
                packedLight);
        tracerQuad(consumer, pose,
                length, -backWidth, backWidth, dark,
                length, backWidth, backWidth, dark,
                0.0D, frontWidth, frontWidth, light,
                0.0D, -frontWidth, frontWidth, light,
                packedLight);
        tracerQuad(consumer, pose,
                length, -backWidth, -backWidth, dark,
                length, backWidth, -backWidth, dark,
                0.0D, frontWidth, -frontWidth, light,
                0.0D, -frontWidth, -frontWidth, light,
                packedLight);
        tracerQuad(consumer, pose,
                length, backWidth, backWidth, dark,
                length, backWidth, -backWidth, dark,
                length, -backWidth, -backWidth, dark,
                length, -backWidth, backWidth, dark,
                packedLight);
        tracerQuad(consumer, pose,
                0.0D, frontWidth, frontWidth, light,
                0.0D, frontWidth, -frontWidth, light,
                0.0D, -frontWidth, -frontWidth, light,
                0.0D, -frontWidth, frontWidth, light,
                packedLight);
    }

    private static void tracerQuad(VertexConsumer consumer, PoseStack.Pose pose,
                                   double ax, double ay, double az, int ac,
                                   double bx, double by, double bz, int bc,
                                   double cx, double cy, double cz, int cc,
                                   double dx, double dy, double dz, int dc,
                                   int packedLight) {
        tracerVertex(consumer, pose, ax, ay, az, ac, packedLight);
        tracerVertex(consumer, pose, bx, by, bz, bc, packedLight);
        tracerVertex(consumer, pose, cx, cy, cz, cc, packedLight);
        tracerVertex(consumer, pose, dx, dy, dz, dc, packedLight);
    }

    private static void tracerVertex(VertexConsumer consumer, PoseStack.Pose pose,
                                     double x, double y, double z, int color, int packedLight) {
        consumer.addVertex(pose, (float) x, (float) y, (float) z)
                .setColor((color >>> 16) & 0xFF, (color >>> 8) & 0xFF, color & 0xFF, 255)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    private static void renderArtilleryShell(LegacyArtilleryShellEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        float yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        float pitch = -Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(pitch - 90.0F));
        poseStack.scale(2.5F, 5.0F, 2.5F);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(ARTILLERY), poseStack, bufferSource, RENDER_STATE, packedLight, 0);
        poseStack.popPose();
    }

    private static void renderHimarsRocket(LegacyHimarsRocketEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        float yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        float pitch = -Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(pitch - 90.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        MachineModelRenderer.renderUnculled(
                MachineModelRenderer.model(HIMARS_MODELS[entity.rocketType().modelData()]),
                poseStack,
                bufferSource,
                RENDER_STATE,
                packedLight,
                0
        );
        poseStack.popPose();
    }

    public static class Bullet extends EntityRenderer<LegacyBulletEntity> {
        public Bullet(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public void render(LegacyBulletEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
            StandardAmmoItem.StandardAmmoType ammo = entity.ammoType();
            if (ammo.family() == StandardAmmoItem.AmmoFamily.ROCKET_ML) {
                renderRichardMissile(entity, partialTick, poseStack, bufferSource, packedLight);
            } else if (ammo.family() != StandardAmmoItem.AmmoFamily.FLAME) {
                renderRifleBullet(entity, ammo, partialTick, poseStack, bufferSource, packedLight);
            }
            super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        }

        @Override
        public ResourceLocation getTextureLocation(LegacyBulletEntity entity) {
            return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
        }
    }

    public static class ArtilleryShell extends EntityRenderer<LegacyArtilleryShellEntity> {
        public ArtilleryShell(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public void render(LegacyArtilleryShellEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
            renderArtilleryShell(entity, partialTick, poseStack, bufferSource, packedLight);
            super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        }

        @Override
        public ResourceLocation getTextureLocation(LegacyArtilleryShellEntity entity) {
            return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
        }
    }

    public static class HimarsRocket extends EntityRenderer<LegacyHimarsRocketEntity> {
        public HimarsRocket(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public void render(LegacyHimarsRocketEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
            renderHimarsRocket(entity, partialTick, poseStack, bufferSource, packedLight);
            super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        }

        @Override
        public ResourceLocation getTextureLocation(LegacyHimarsRocketEntity entity) {
            return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
        }
    }
}
