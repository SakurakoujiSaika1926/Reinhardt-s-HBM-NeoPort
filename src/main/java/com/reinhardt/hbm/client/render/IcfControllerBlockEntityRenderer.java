package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.reinhardt.hbm.block.IcfControllerBlock;
import com.reinhardt.hbm.blockentity.IcfControllerBlockEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

/** Exact finite-beam port of the latest 1.7.10 {@code RenderICFController}. */
public final class IcfControllerBlockEntityRenderer implements LongRangeBlockEntityRenderer<IcfControllerBlockEntity> {
    private static final int OUTER_COLOR = 0x202020;
    private static final int INNER_COLOR = 0x100000;
    private static final int LAYERS = 10;
    private static final double MAX_RADIUS = 0.125D;

    public IcfControllerBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(IcfControllerBlockEntity controller, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        int length = controller.laserLength();
        if (length <= 0) {
            return;
        }

        Direction direction = facing(controller);
        double directionX = direction.getStepX();
        double directionZ = direction.getStepZ();
        double startX = 0.5D;
        double startY = 0.5D;
        double startZ = 0.5D;
        double endX = startX + directionX * length;
        double endZ = startZ + directionZ * length;
        double perpendicularX = -directionZ;
        double perpendicularZ = directionX;
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());
        PoseStack.Pose pose = poseStack.last();

        for (int layer = 1; layer <= LAYERS; layer++) {
            double radius = MAX_RADIUS * layer / LAYERS;
            double blend = (layer - 1) / (double) (LAYERS - 1);
            int color = interpolateColor(OUTER_COLOR, INNER_COLOR, blend);
            emitLayer(consumer, pose,
                    startX, startY, startZ, endX, startY, endZ,
                    perpendicularX, perpendicularZ, radius, color);
        }
    }

    @Override
    public AABB getRenderBoundingBox(IcfControllerBlockEntity controller) {
        BlockPos pos = controller.getBlockPos();
        Direction direction = facing(controller);
        int length = Math.max(0, controller.laserLength());
        double startX = pos.getX() + 0.5D;
        double startY = pos.getY() + 0.5D;
        double startZ = pos.getZ() + 0.5D;
        double endX = startX + direction.getStepX() * length;
        double endZ = startZ + direction.getStepZ() * length;
        return new AABB(
                Math.min(startX, endX) - MAX_RADIUS,
                startY - MAX_RADIUS,
                Math.min(startZ, endZ) - MAX_RADIUS,
                Math.max(startX, endX) + MAX_RADIUS,
                startY + MAX_RADIUS,
                Math.max(startZ, endZ) + MAX_RADIUS
        );
    }

    private static Direction facing(IcfControllerBlockEntity controller) {
        return controller.getBlockState().getValue(IcfControllerBlock.FACING);
    }

    private static void emitLayer(VertexConsumer consumer, PoseStack.Pose pose,
                                  double startX, double startY, double startZ,
                                  double endX, double endY, double endZ,
                                  double perpendicularX, double perpendicularZ,
                                  double radius, int color) {
        double[] start = crossSection(startX, startY, startZ, perpendicularX, perpendicularZ, radius);
        double[] end = crossSection(endX, endY, endZ, perpendicularX, perpendicularZ, radius);
        for (int side = 0; side < 4; side++) {
            int next = (side + 1) & 3;
            vertex(consumer, pose, start, side, color);
            vertex(consumer, pose, start, next, color);
            vertex(consumer, pose, end, next, color);
            vertex(consumer, pose, end, side, color);
        }
    }

    private static double[] crossSection(double centerX, double centerY, double centerZ,
                                         double perpendicularX, double perpendicularZ, double radius) {
        return new double[]{
                centerX + perpendicularX * radius, centerY + radius, centerZ + perpendicularZ * radius,
                centerX - perpendicularX * radius, centerY + radius, centerZ - perpendicularZ * radius,
                centerX - perpendicularX * radius, centerY - radius, centerZ - perpendicularZ * radius,
                centerX + perpendicularX * radius, centerY - radius, centerZ + perpendicularZ * radius
        };
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose,
                               double[] points, int index, int color) {
        int offset = index * 3;
        consumer.addVertex(pose, (float) points[offset], (float) points[offset + 1], (float) points[offset + 2])
                .setColor((color >>> 16) & 0xFF, (color >>> 8) & 0xFF, color & 0xFF, 255)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    private static int interpolateColor(int from, int to, double progress) {
        int red = (int) (((from >>> 16) & 0xFF) + (((to >>> 16) & 0xFF) - ((from >>> 16) & 0xFF)) * progress);
        int green = (int) (((from >>> 8) & 0xFF) + (((to >>> 8) & 0xFF) - ((from >>> 8) & 0xFF)) * progress);
        int blue = (int) ((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * progress);
        return red << 16 | green << 8 | blue;
    }
}
