package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.FoundryTankBlockEntity;
import com.reinhardt.hbm.foundry.FoundryMaterial;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;

import java.awt.Color;

public class FoundryTankBlockEntityRenderer implements BlockEntityRenderer<FoundryTankBlockEntity> {
    private static final ResourceLocation TOP = texture("foundry_tank_top");
    private static final ResourceLocation SIDE = texture("foundry_tank_side");
    private static final ResourceLocation SIDE_OUTLET = texture("foundry_tank_side_outlet");
    private static final ResourceLocation UPPER = texture("foundry_tank_upper");
    private static final ResourceLocation UPPER_OUTLET = texture("foundry_tank_upper_outlet");
    private static final ResourceLocation BOTTOM = texture("foundry_tank_bottom");
    private static final ResourceLocation INNER = texture("foundry_tank_inner");
    private static final ResourceLocation LAVA = texture("lava_gray");

    public FoundryTankBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(FoundryTankBlockEntity tank, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        FoundryTankBlockEntity.RenderNeighborSnapshot neighbors = tank.renderNeighborSnapshot();
        boolean east = neighbors.tank(Direction.EAST);
        boolean west = neighbors.tank(Direction.WEST);
        boolean south = neighbors.tank(Direction.SOUTH);
        boolean north = neighbors.tank(Direction.NORTH);
        boolean up = neighbors.tank(Direction.UP);
        boolean down = neighbors.tank(Direction.DOWN);

        if (!down) {
            plate(buffers, poseStack, packedLight, packedOverlay);
        }
        if (!east) {
            wall(buffers, poseStack, Direction.EAST, neighbors.outlet(Direction.EAST),
                    up, down, north, south, packedLight, packedOverlay);
        }
        if (!west) {
            wall(buffers, poseStack, Direction.WEST, neighbors.outlet(Direction.WEST),
                    up, down, north, south, packedLight, packedOverlay);
        }
        if (!south) {
            wall(buffers, poseStack, Direction.SOUTH, neighbors.outlet(Direction.SOUTH),
                    up, down, east, west, packedLight, packedOverlay);
        }
        if (!north) {
            wall(buffers, poseStack, Direction.NORTH, neighbors.outlet(Direction.NORTH),
                    up, down, east, west, packedLight, packedOverlay);
        }

        renderFluid(tank, poseStack, buffers, east, west, south, north, up, down, packedOverlay);
    }

    @Override
    public AABB getRenderBoundingBox(FoundryTankBlockEntity tank) {
        // Every hand-emitted wall, plate, edge and legal fluid surface uses
        // coordinates in the owning block's closed unit cube.
        return new AABB(tank.getBlockPos());
    }

    private static void plate(MultiBufferSource buffers, PoseStack poseStack, int light, int overlay) {
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(BOTTOM));
        quad(consumer, poseStack.last(), Direction.UP, 0.0D, 0.125D, 0.0D, 1.0D, 0.125D, 1.0D,
                255, 255, 255, 255, light, overlay);
        quad(consumer, poseStack.last(), Direction.DOWN, 0.0D, 0.0D, 0.0D, 1.0D, 0.0D, 1.0D,
                255, 255, 255, 255, light, overlay);
    }

    private static void wall(MultiBufferSource buffers, PoseStack poseStack, Direction side,
                             boolean hasOutlet, boolean up, boolean down,
                             boolean connectedA, boolean connectedB, int light, int overlay) {
        ResourceLocation outer = down ? (hasOutlet ? UPPER_OUTLET : UPPER)
                : (hasOutlet ? SIDE_OUTLET : SIDE);
        ResourceLocation inner = up ? BOTTOM : INNER;
        PoseStack.Pose pose = poseStack.last();

        VertexConsumer outerConsumer = buffers.getBuffer(RenderType.entityCutoutNoCull(outer));
        VertexConsumer innerConsumer = buffers.getBuffer(RenderType.entityCutoutNoCull(inner));
        VertexConsumer topConsumer = buffers.getBuffer(RenderType.entityCutoutNoCull(TOP));
        VertexConsumer edgeConsumer = buffers.getBuffer(RenderType.entityCutoutNoCull(inner));

        switch (side) {
            case EAST -> {
                quad(outerConsumer, pose, Direction.EAST, 1.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D,
                        255, 255, 255, 255, light, overlay);
                quad(innerConsumer, pose, Direction.WEST, 0.875D, 0.0D, 0.0D, 0.875D, 1.0D, 1.0D,
                        255, 255, 255, 255, light, overlay);
                quad(topConsumer, pose, Direction.UP, 0.875D, 1.0D, 0.0D, 1.0D, 1.0D, 1.0D,
                        255, 255, 255, 255, light, overlay);
                if (connectedA) quad(edgeConsumer, pose, Direction.NORTH, 0.875D, 0.0D, 0.0D, 1.0D, 1.0D, 0.0D,
                        255, 255, 255, 255, light, overlay);
                if (connectedB) quad(edgeConsumer, pose, Direction.SOUTH, 0.875D, 0.0D, 1.0D, 1.0D, 1.0D, 1.0D,
                        255, 255, 255, 255, light, overlay);
            }
            case WEST -> {
                quad(outerConsumer, pose, Direction.WEST, 0.0D, 0.0D, 0.0D, 0.0D, 1.0D, 1.0D,
                        255, 255, 255, 255, light, overlay);
                quad(innerConsumer, pose, Direction.EAST, 0.125D, 0.0D, 0.0D, 0.125D, 1.0D, 1.0D,
                        255, 255, 255, 255, light, overlay);
                quad(topConsumer, pose, Direction.UP, 0.0D, 1.0D, 0.0D, 0.125D, 1.0D, 1.0D,
                        255, 255, 255, 255, light, overlay);
                if (connectedA) quad(edgeConsumer, pose, Direction.NORTH, 0.0D, 0.0D, 0.0D, 0.125D, 1.0D, 0.0D,
                        255, 255, 255, 255, light, overlay);
                if (connectedB) quad(edgeConsumer, pose, Direction.SOUTH, 0.0D, 0.0D, 1.0D, 0.125D, 1.0D, 1.0D,
                        255, 255, 255, 255, light, overlay);
            }
            case SOUTH -> {
                quad(outerConsumer, pose, Direction.SOUTH, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D, 1.0D,
                        255, 255, 255, 255, light, overlay);
                quad(innerConsumer, pose, Direction.NORTH, 0.0D, 0.0D, 0.875D, 1.0D, 1.0D, 0.875D,
                        255, 255, 255, 255, light, overlay);
                quad(topConsumer, pose, Direction.UP, 0.0D, 1.0D, 0.875D, 1.0D, 1.0D, 1.0D,
                        255, 255, 255, 255, light, overlay);
                if (connectedA) quad(edgeConsumer, pose, Direction.EAST, 1.0D, 0.0D, 0.875D, 1.0D, 1.0D, 1.0D,
                        255, 255, 255, 255, light, overlay);
                if (connectedB) quad(edgeConsumer, pose, Direction.WEST, 0.0D, 0.0D, 0.875D, 0.0D, 1.0D, 1.0D,
                        255, 255, 255, 255, light, overlay);
            }
            case NORTH -> {
                quad(outerConsumer, pose, Direction.NORTH, 0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 0.0D,
                        255, 255, 255, 255, light, overlay);
                quad(innerConsumer, pose, Direction.SOUTH, 0.0D, 0.0D, 0.125D, 1.0D, 1.0D, 0.125D,
                        255, 255, 255, 255, light, overlay);
                quad(topConsumer, pose, Direction.UP, 0.0D, 1.0D, 0.0D, 1.0D, 1.0D, 0.125D,
                        255, 255, 255, 255, light, overlay);
                if (connectedA) quad(edgeConsumer, pose, Direction.EAST, 1.0D, 0.0D, 0.0D, 1.0D, 1.0D, 0.125D,
                        255, 255, 255, 255, light, overlay);
                if (connectedB) quad(edgeConsumer, pose, Direction.WEST, 0.0D, 0.0D, 0.0D, 0.0D, 1.0D, 0.125D,
                        255, 255, 255, 255, light, overlay);
            }
            case UP, DOWN -> throw new IllegalArgumentException(
                    "Foundry tank wall side must be horizontal: " + side);
        }
    }

    private static void renderFluid(FoundryTankBlockEntity tank, PoseStack poseStack,
                                    MultiBufferSource buffers, boolean east, boolean west,
                                    boolean south, boolean north, boolean up, boolean down,
                                    int overlay) {
        FoundryMaterial material = tank.material();
        if (material == null || tank.amount() <= 0) {
            return;
        }
        double maximumHeight = 0.75D + (down ? 0.125D : 0.0D) + (up ? 0.125D : 0.0D);
        double base = down ? 0.0D : 0.125D;
        double height = base + tank.amount() * maximumHeight / tank.capacity();

        Color color = new Color(material.moltenColor()).brighter();
        int red = (int) (255.0D - (255.0D - color.getRed()) * 0.7D);
        int green = (int) (255.0D - (255.0D - color.getGreen()) * 0.7D);
        int blue = (int) (255.0D - (255.0D - color.getBlue()) * 0.7D);
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(LAVA));
        PoseStack.Pose pose = poseStack.last();
        quad(consumer, pose, Direction.UP, 0.0D, height, 0.0D, 1.0D, height, 1.0D,
                red, green, blue, 220, LightTexture.FULL_BRIGHT, overlay);
        if (east) quad(consumer, pose, Direction.EAST, 1.0D, base, 0.0D, 1.0D, height, 1.0D,
                red, green, blue, 220, LightTexture.FULL_BRIGHT, overlay);
        if (west) quad(consumer, pose, Direction.WEST, 0.0D, base, 0.0D, 0.0D, height, 1.0D,
                red, green, blue, 220, LightTexture.FULL_BRIGHT, overlay);
        if (south) quad(consumer, pose, Direction.SOUTH, 0.0D, base, 1.0D, 1.0D, height, 1.0D,
                red, green, blue, 220, LightTexture.FULL_BRIGHT, overlay);
        if (north) quad(consumer, pose, Direction.NORTH, 0.0D, base, 0.0D, 1.0D, height, 0.0D,
                red, green, blue, 220, LightTexture.FULL_BRIGHT, overlay);
    }

    private static ResourceLocation texture(String name) {
        return ReinhardtsHBM.id("textures/block/" + name + ".png");
    }

    private static void quad(VertexConsumer consumer, PoseStack.Pose pose, Direction face,
                             double x0, double y0, double z0, double x1, double y1, double z1,
                             int red, int green, int blue, int alpha, int light, int overlay) {
        switch (face) {
            case UP -> {
                vertex(consumer, pose, x0, y0, z0, 0.0F, 0.0F, red, green, blue, alpha, light, overlay, 0, 1, 0);
                vertex(consumer, pose, x0, y0, z1, 0.0F, 1.0F, red, green, blue, alpha, light, overlay, 0, 1, 0);
                vertex(consumer, pose, x1, y1, z1, 1.0F, 1.0F, red, green, blue, alpha, light, overlay, 0, 1, 0);
                vertex(consumer, pose, x1, y1, z0, 1.0F, 0.0F, red, green, blue, alpha, light, overlay, 0, 1, 0);
            }
            case DOWN -> {
                vertex(consumer, pose, x1, y1, z0, 1.0F, 0.0F, red, green, blue, alpha, light, overlay, 0, -1, 0);
                vertex(consumer, pose, x1, y1, z1, 1.0F, 1.0F, red, green, blue, alpha, light, overlay, 0, -1, 0);
                vertex(consumer, pose, x0, y0, z1, 0.0F, 1.0F, red, green, blue, alpha, light, overlay, 0, -1, 0);
                vertex(consumer, pose, x0, y0, z0, 0.0F, 0.0F, red, green, blue, alpha, light, overlay, 0, -1, 0);
            }
            case EAST -> {
                vertex(consumer, pose, x0, y0, z0, 0.0F, 1.0F, red, green, blue, alpha, light, overlay, 1, 0, 0);
                vertex(consumer, pose, x0, y1, z0, 0.0F, 0.0F, red, green, blue, alpha, light, overlay, 1, 0, 0);
                vertex(consumer, pose, x0, y1, z1, 1.0F, 0.0F, red, green, blue, alpha, light, overlay, 1, 0, 0);
                vertex(consumer, pose, x0, y0, z1, 1.0F, 1.0F, red, green, blue, alpha, light, overlay, 1, 0, 0);
            }
            case WEST -> {
                vertex(consumer, pose, x0, y0, z0, 0.0F, 1.0F, red, green, blue, alpha, light, overlay, -1, 0, 0);
                vertex(consumer, pose, x0, y0, z1, 1.0F, 1.0F, red, green, blue, alpha, light, overlay, -1, 0, 0);
                vertex(consumer, pose, x0, y1, z1, 1.0F, 0.0F, red, green, blue, alpha, light, overlay, -1, 0, 0);
                vertex(consumer, pose, x0, y1, z0, 0.0F, 0.0F, red, green, blue, alpha, light, overlay, -1, 0, 0);
            }
            case SOUTH -> {
                vertex(consumer, pose, x0, y0, z0, 0.0F, 1.0F, red, green, blue, alpha, light, overlay, 0, 0, 1);
                vertex(consumer, pose, x1, y0, z0, 1.0F, 1.0F, red, green, blue, alpha, light, overlay, 0, 0, 1);
                vertex(consumer, pose, x1, y1, z0, 1.0F, 0.0F, red, green, blue, alpha, light, overlay, 0, 0, 1);
                vertex(consumer, pose, x0, y1, z0, 0.0F, 0.0F, red, green, blue, alpha, light, overlay, 0, 0, 1);
            }
            case NORTH -> {
                vertex(consumer, pose, x0, y0, z0, 0.0F, 1.0F, red, green, blue, alpha, light, overlay, 0, 0, -1);
                vertex(consumer, pose, x0, y1, z0, 0.0F, 0.0F, red, green, blue, alpha, light, overlay, 0, 0, -1);
                vertex(consumer, pose, x1, y1, z0, 1.0F, 0.0F, red, green, blue, alpha, light, overlay, 0, 0, -1);
                vertex(consumer, pose, x1, y0, z0, 1.0F, 1.0F, red, green, blue, alpha, light, overlay, 0, 0, -1);
            }
        }
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, double x, double y, double z,
                               float u, float v, int red, int green, int blue, int alpha,
                               int light, int overlay, float nx, float ny, float nz) {
        consumer.addVertex(pose, (float) x, (float) y, (float) z)
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }
}
