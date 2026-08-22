package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.FoundrySlagBlockEntity;
import com.reinhardt.hbm.foundry.FoundryMaterial;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;

public class FoundrySlagBlockEntityRenderer implements BlockEntityRenderer<FoundrySlagBlockEntity> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/block/slag.png");

    public FoundrySlagBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(FoundrySlagBlockEntity slag, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        FoundryMaterial material = slag.material();
        if (material == null || slag.amount() <= 0) {
            return;
        }
        double height = Math.max(1.0D / 16.0D, Math.min(1.0D, (double) slag.amount() / FoundrySlagBlockEntity.MAX_AMOUNT));
        int color = 0xFF000000 | material.moltenColor();
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        PoseStack.Pose pose = poseStack.last();

        face(consumer, pose, 0.0D, height, 0.0D, 1.0D, height, 1.0D, 0.0F, 1.0F, 0.0F, r, g, b, packedLight, packedOverlay);
        face(consumer, pose, 0.0D, 0.0D, 1.0D, 1.0D, 0.0D, 0.0D, 0.0F, -1.0F, 0.0F, r, g, b, packedLight, packedOverlay);
        verticalFace(consumer, pose, 0.0D, 0.0D, 0.0D, 1.0D, height, 0.0D, 0.0F, 0.0F, -1.0F, r, g, b, packedLight, packedOverlay);
        verticalFace(consumer, pose, 1.0D, 0.0D, 1.0D, 0.0D, height, 1.0D, 0.0F, 0.0F, 1.0F, r, g, b, packedLight, packedOverlay);
        verticalFace(consumer, pose, 1.0D, 0.0D, 0.0D, 1.0D, height, 1.0D, 1.0F, 0.0F, 0.0F, r, g, b, packedLight, packedOverlay);
        verticalFace(consumer, pose, 0.0D, 0.0D, 1.0D, 0.0D, height, 0.0D, -1.0F, 0.0F, 0.0F, r, g, b, packedLight, packedOverlay);
    }

    @Override
    public AABB getRenderBoundingBox(FoundrySlagBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos());
    }

    private static void face(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            double x0,
            double y0,
            double z0,
            double x1,
            double y1,
            double z1,
            float nx,
            float ny,
            float nz,
            int r,
            int g,
            int b,
            int packedLight,
            int packedOverlay
    ) {
        vertex(consumer, pose, x0, y0, z0, 0.0F, 0.0F, nx, ny, nz, r, g, b, packedLight, packedOverlay);
        vertex(consumer, pose, x0, y1, z1, 0.0F, 1.0F, nx, ny, nz, r, g, b, packedLight, packedOverlay);
        vertex(consumer, pose, x1, y1, z1, 1.0F, 1.0F, nx, ny, nz, r, g, b, packedLight, packedOverlay);
        vertex(consumer, pose, x1, y0, z0, 1.0F, 0.0F, nx, ny, nz, r, g, b, packedLight, packedOverlay);
    }

    private static void verticalFace(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            double x0,
            double y0,
            double z0,
            double x1,
            double y1,
            double z1,
            float nx,
            float ny,
            float nz,
            int r,
            int g,
            int b,
            int packedLight,
            int packedOverlay
    ) {
        vertex(consumer, pose, x0, y0, z0, 0.0F, 1.0F, nx, ny, nz, r, g, b, packedLight, packedOverlay);
        vertex(consumer, pose, x0, y1, z0, 0.0F, 0.0F, nx, ny, nz, r, g, b, packedLight, packedOverlay);
        vertex(consumer, pose, x1, y1, z1, 1.0F, 0.0F, nx, ny, nz, r, g, b, packedLight, packedOverlay);
        vertex(consumer, pose, x1, y0, z1, 1.0F, 1.0F, nx, ny, nz, r, g, b, packedLight, packedOverlay);
    }

    private static void vertex(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            double x,
            double y,
            double z,
            float u,
            float v,
            float nx,
            float ny,
            float nz,
            int r,
            int g,
            int b,
            int packedLight,
            int packedOverlay
    ) {
        consumer.addVertex(pose, (float) x, (float) y, (float) z)
                .setColor(r, g, b, 255)
                .setUv(u, v)
                .setOverlay(packedOverlay)
                .setLight(packedLight)
                .setNormal(pose, nx, ny, nz);
    }
}
