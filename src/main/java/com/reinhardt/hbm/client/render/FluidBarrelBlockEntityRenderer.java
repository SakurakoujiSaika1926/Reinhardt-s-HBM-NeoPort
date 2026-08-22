package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.FluidBarrelBlockEntity;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidSymbol;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;

public final class FluidBarrelBlockEntityRenderer implements BlockEntityRenderer<FluidBarrelBlockEntity> {
    private static final ResourceLocation DANGER_DIAMOND = ReinhardtsHBM.id("textures/models/misc/danger_diamond.png");
    private static final float ATLAS = 1.0F / 256.0F;
    private static final float DIAMOND = 1.0F / 139.0F;

    public FluidBarrelBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(FluidBarrelBlockEntity barrel, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        HbmFluidDefinition fluid = barrel.tank().type();
        if (fluid == null || fluid.isNone()) {
            return;
        }

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(DANGER_DIAMOND));
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.5D, 0.5D);
        for (int side = 0; side < 4; side++) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(side * 90.0F));
            poseStack.translate(0.4D, 0.30D, -0.24D);
            poseStack.scale(1.0F, 0.25F, 0.25F);
            renderDiamond(fluid, poseStack, consumer, LightTexture.FULL_BRIGHT, packedOverlay);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(FluidBarrelBlockEntity barrel) {
        return new AABB(barrel.getBlockPos()).inflate(0.1D);
    }

    static void renderDiamond(HbmFluidDefinition fluid, PoseStack poseStack, VertexConsumer consumer, int light, int overlay) {
        quad(poseStack, consumer, light, overlay,
                0.0F, 0.5F, -0.5F, 144 * ATLAS, 45 * ATLAS,
                0.0F, 0.5F, 0.5F, 5 * ATLAS, 45 * ATLAS,
                0.0F, -0.5F, 0.5F, 5 * ATLAS, 184 * ATLAS,
                0.0F, -0.5F, -0.5F, 144 * ATLAS, 184 * ATLAS);

        number(poseStack, consumer, light, overlay, fluid.poison(), 0.0F, 33.0F * DIAMOND);
        number(poseStack, consumer, light, overlay, fluid.flammability(), 33.0F * DIAMOND, 0.0F);
        number(poseStack, consumer, light, overlay, fluid.reactivity(), 0.0F, -33.0F * DIAMOND);
        symbol(poseStack, consumer, light, overlay, fluid.symbol());
    }

    private static void number(PoseStack poseStack, VertexConsumer consumer, int light, int overlay, int value, float centerY, float centerZ) {
        if (value < 0 || value >= 6) {
            return;
        }
        float halfWidth = 10.0F * DIAMOND;
        float halfHeight = 14.0F * DIAMOND;
        int x = value == 0 ? 125 : 5 + (value - 1) * 24;
        int y = 5;
        quad(poseStack, consumer, light, overlay,
                0.01F, centerY + halfHeight, centerZ - halfWidth, (x + 20) * ATLAS, y * ATLAS,
                0.01F, centerY + halfHeight, centerZ + halfWidth, x * ATLAS, y * ATLAS,
                0.01F, centerY - halfHeight, centerZ + halfWidth, x * ATLAS, (y + 28) * ATLAS,
                0.01F, centerY - halfHeight, centerZ - halfWidth, (x + 20) * ATLAS, (y + 28) * ATLAS);
    }

    private static void symbol(PoseStack poseStack, VertexConsumer consumer, int light, int overlay, HbmFluidSymbol symbol) {
        if (symbol == null || symbol == HbmFluidSymbol.NONE) {
            return;
        }
        int[] uv = switch (symbol) {
            case RADIATION -> new int[]{195, 2};
            case NOWATER -> new int[]{195, 63};
            case ACID -> new int[]{195, 124};
            case ASPHYXIANT -> new int[]{195, 185};
            case CROYGENIC -> new int[]{134, 185};
            case ANTIMATTER -> new int[]{73, 185};
            case OXIDIZER -> new int[]{12, 185};
            default -> new int[]{0, 0};
        };
        float halfSize = 29.5F * DIAMOND;
        float centerY = -33.0F * DIAMOND;
        quad(poseStack, consumer, light, overlay,
                0.01F, centerY + halfSize, -halfSize, (uv[0] + 59) * ATLAS, uv[1] * ATLAS,
                0.01F, centerY + halfSize, halfSize, uv[0] * ATLAS, uv[1] * ATLAS,
                0.01F, centerY - halfSize, halfSize, uv[0] * ATLAS, (uv[1] + 59) * ATLAS,
                0.01F, centerY - halfSize, -halfSize, (uv[0] + 59) * ATLAS, (uv[1] + 59) * ATLAS);
    }

    private static void quad(
            PoseStack poseStack,
            VertexConsumer consumer,
            int light,
            int overlay,
            float x1, float y1, float z1, float u1, float v1,
            float x2, float y2, float z2, float u2, float v2,
            float x3, float y3, float z3, float u3, float v3,
            float x4, float y4, float z4, float u4, float v4
    ) {
        PoseStack.Pose pose = poseStack.last();
        vertex(pose, consumer, light, overlay, x1, y1, z1, u1, v1);
        vertex(pose, consumer, light, overlay, x2, y2, z2, u2, v2);
        vertex(pose, consumer, light, overlay, x3, y3, z3, u3, v3);
        vertex(pose, consumer, light, overlay, x4, y4, z4, u4, v4);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer consumer, int light, int overlay, float x, float y, float z, float u, float v) {
        consumer.addVertex(pose, x, y, z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(pose, 1.0F, 0.0F, 0.0F);
    }
}
