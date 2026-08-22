package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.FoundryCastingBlockEntity;
import com.reinhardt.hbm.foundry.FoundryMaterial;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class FoundryCastingBlockEntityRenderer implements BlockEntityRenderer<FoundryCastingBlockEntity> {
    private static final ResourceLocation LAVA = ReinhardtsHBM.id("textures/models/machines/lava.png");

    public FoundryCastingBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(FoundryCastingBlockEntity casting, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        FoundryMaterial material = casting.material();
        int capacity = casting.getCapacity();
        if (material == null || casting.amount() <= 0 || capacity <= 0) {
            return;
        }

        double level = (casting.getMoldSize() == 0 ? 0.125D : 0.125D)
                + casting.amount() * (casting.getMoldSize() == 0 ? 0.25D : 0.75D) / capacity;
        int color = 0xFF000000 | material.moltenColor();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(LAVA));
        PoseStack.Pose pose = poseStack.last();
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        vertex(consumer, pose, new Vec3(0.125D, level, 0.125D), 0.0F, 0.0F, r, g, b, packedOverlay);
        vertex(consumer, pose, new Vec3(0.125D, level, 0.875D), 0.0F, 1.0F, r, g, b, packedOverlay);
        vertex(consumer, pose, new Vec3(0.875D, level, 0.875D), 1.0F, 1.0F, r, g, b, packedOverlay);
        vertex(consumer, pose, new Vec3(0.875D, level, 0.125D), 1.0F, 0.0F, r, g, b, packedOverlay);
    }

    @Override
    public AABB getRenderBoundingBox(FoundryCastingBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos());
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, Vec3 pos, float u, float v, int r, int g, int b, int packedOverlay) {
        consumer.addVertex(pose, (float) pos.x, (float) pos.y, (float) pos.z)
                .setColor(r, g, b, 220)
                .setUv(u, v)
                .setOverlay(packedOverlay)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }
}
