package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.FoundryChannelBlock;
import com.reinhardt.hbm.blockentity.FoundryFlowBlockEntity;
import com.reinhardt.hbm.foundry.FoundryMaterial;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class FoundryFlowBlockEntityRenderer implements BlockEntityRenderer<FoundryFlowBlockEntity> {
    private static final ResourceLocation LAVA = ReinhardtsHBM.id("textures/models/machines/lava.png");

    public FoundryFlowBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(FoundryFlowBlockEntity flow, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        FoundryMaterial material = flow.material();
        int capacity = flow.capacity();
        if (material == null || flow.amount() <= 0 || capacity <= 0) {
            return;
        }

        int color = 0xFF000000 | material.moltenColor();
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        double y = 0.0625D + Math.min(1.0D, (double) flow.amount() / capacity) * 0.3125D;
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(LAVA));
        PoseStack.Pose pose = poseStack.last();
        BlockState state = flow.getBlockState();

        surface(consumer, pose, 0.34375D, 0.34375D, 0.65625D, 0.65625D, y, r, g, b, packedOverlay);
        if (state.hasProperty(FoundryChannelBlock.NORTH) && state.getValue(FoundryChannelBlock.NORTH)) {
            surface(consumer, pose, 0.34375D, 0.0D, 0.65625D, 0.34375D, y, r, g, b, packedOverlay);
        }
        if (state.hasProperty(FoundryChannelBlock.EAST) && state.getValue(FoundryChannelBlock.EAST)) {
            surface(consumer, pose, 0.65625D, 0.34375D, 1.0D, 0.65625D, y, r, g, b, packedOverlay);
        }
        if (state.hasProperty(FoundryChannelBlock.SOUTH) && state.getValue(FoundryChannelBlock.SOUTH)) {
            surface(consumer, pose, 0.34375D, 0.65625D, 0.65625D, 1.0D, y, r, g, b, packedOverlay);
        }
        if (state.hasProperty(FoundryChannelBlock.WEST) && state.getValue(FoundryChannelBlock.WEST)) {
            surface(consumer, pose, 0.0D, 0.34375D, 0.34375D, 0.65625D, y, r, g, b, packedOverlay);
        }
    }

    @Override
    public AABB getRenderBoundingBox(FoundryFlowBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos());
    }

    private static void surface(VertexConsumer consumer, PoseStack.Pose pose, double x0, double z0, double x1, double z1, double y, int r, int g, int b, int packedOverlay) {
        vertex(consumer, pose, x0, y, z0, 0.0F, 0.0F, r, g, b, packedOverlay);
        vertex(consumer, pose, x0, y, z1, 0.0F, 1.0F, r, g, b, packedOverlay);
        vertex(consumer, pose, x1, y, z1, 1.0F, 1.0F, r, g, b, packedOverlay);
        vertex(consumer, pose, x1, y, z0, 1.0F, 0.0F, r, g, b, packedOverlay);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, double x, double y, double z, float u, float v, int r, int g, int b, int packedOverlay) {
        consumer.addVertex(pose, (float) x, (float) y, (float) z)
                .setColor(r, g, b, 220)
                .setUv(u, v)
                .setOverlay(packedOverlay)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }
}
