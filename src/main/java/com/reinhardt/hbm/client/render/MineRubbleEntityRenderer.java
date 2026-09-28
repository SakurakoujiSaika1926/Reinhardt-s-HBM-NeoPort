package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.entity.MineRubbleEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class MineRubbleEntityRenderer extends EntityRenderer<MineRubbleEntity> {
    private static final Map<BlockState, Double> VISUAL_RADII = new ConcurrentHashMap<>();
    private static final Shape[] SHAPES = {
            new Shape(-7, 1, 2, 14, 6, 6, 0, 0, 0),
            new Shape(-7, -6, -5, 6, 13, 5, 0, 0, 0),
            new Shape(1, 1, -5, 6, 6, 6, 0, 0, 0),
            new Shape(-7, -7, 2, 14, 7, 4, 0, 0.4363323F, 0),
            new Shape(0, -6, -5, 6, 6, 11, 0, 0, 0),
            new Shape(-4, -4, -4, 8, 8, 8, 0, 0, 0),
            new Shape(-7, -5, 1, 6, 5, 7, 0, 0, 0),
            new Shape(-6, -1, 3, 12, 6, 4, 0, 0, -0.3490659F),
            new Shape(-6, 2, -3, 12, 6, 6, 0, -0.2094395F, 0),
            new Shape(-5, -3, -6, 6, 10, 4, 0, 0, -0.3490659F)
    };

    public MineRubbleEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(MineRubbleEntity rubble, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0.0D, 0.25D, 0.0D);
        float angle = ((rubble.tickCount + partialTick) % 360.0F) * 10.0F;
        poseStack.mulPose(new org.joml.Quaternionf(new org.joml.AxisAngle4f(
                (float) Math.toRadians(angle), 1.0F, 1.0F, 1.0F)));
        BlockState renderState = rubble.blockState();
        for (Shape shape : SHAPES) {
            poseStack.pushPose();
            poseStack.translate(shape.x / 16.0D, shape.y / 16.0D, shape.z / 16.0D);
            poseStack.mulPose(new org.joml.Quaternionf(new org.joml.AxisAngle4f(shape.rotY, 0.0F, 1.0F, 0.0F)));
            poseStack.mulPose(new org.joml.Quaternionf(new org.joml.AxisAngle4f(shape.rotZ, 0.0F, 0.0F, 1.0F)));
            poseStack.scale(shape.width / 16.0F, shape.height / 16.0F, shape.depth / 16.0F);
            MachineModelRenderer.renderUnculled(
                    Minecraft.getInstance().getBlockRenderer().getBlockModel(renderState),
                    poseStack, bufferSource, renderState, packedLight, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }
        poseStack.popPose();
        super.render(rubble, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public boolean shouldRender(MineRubbleEntity rubble, Frustum frustum,
                                double cameraX, double cameraY, double cameraZ) {
        double radius = VISUAL_RADII.computeIfAbsent(
                rubble.blockState(), MineRubbleEntityRenderer::captureVisualRadius);
        AABB bounds = EntityRenderBounds.radius(
                rubble.getX(), rubble.getY() + 0.25D, rubble.getZ(), radius);
        return EntityRenderBounds.shouldRender(rubble, bounds, frustum,
                cameraX, cameraY, cameraZ);
    }

    @Override
    public ResourceLocation getTextureLocation(MineRubbleEntity entity) {
        return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
    }

    static void invalidateBounds() {
        VISUAL_RADII.clear();
    }

    private static double captureVisualRadius(BlockState state) {
        List<PoseStack> poses = new ArrayList<>(SHAPES.length);
        for (Shape shape : SHAPES) {
            PoseStack poseStack = new PoseStack();
            poseStack.translate(shape.x / 16.0D, shape.y / 16.0D, shape.z / 16.0D);
            poseStack.mulPose(new org.joml.Quaternionf(new org.joml.AxisAngle4f(
                    shape.rotY, 0.0F, 1.0F, 0.0F)));
            poseStack.mulPose(new org.joml.Quaternionf(new org.joml.AxisAngle4f(
                    shape.rotZ, 0.0F, 0.0F, 1.0F)));
            poseStack.scale(shape.width / 16.0F, shape.height / 16.0F, shape.depth / 16.0F);
            poses.add(poseStack);
        }
        AABB captured = BakedBlockGeometrySnapshot.build(
                state, poses, BlockPos.ZERO).bounds();
        double maximumSquared = 0.0D;
        for (double x : new double[] {captured.minX, captured.maxX}) {
            for (double y : new double[] {captured.minY, captured.maxY}) {
                for (double z : new double[] {captured.minZ, captured.maxZ}) {
                    maximumSquared = Math.max(maximumSquared, x * x + y * y + z * z);
                }
            }
        }
        double unitRoundoff = 0x1.0p-24;
        double gamma64 = 64.0D * unitRoundoff / (1.0D - 64.0D * unitRoundoff);
        return Math.nextUp(Math.sqrt(maximumSquared) * (1.0D + gamma64));
    }

    private record Shape(int x, int y, int z, int width, int height, int depth,
                         float rotX, float rotY, float rotZ) {
    }
}
